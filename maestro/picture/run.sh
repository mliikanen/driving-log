#!/usr/bin/env bash
# Runs the picture flows one step at a time and checks the files the app keeps in its private storage after each, over adb:
# two files per picture (a small and a large one), both WebP, the small one 256 x 256 pixels and the large one at most
# 1024 x 1024 and under 200 kB, the files of a replaced or removed picture gone, and nothing left after leaving the add screen
# without saving; then the camera flow. The photo they choose is uploaded once, by setup.yaml, at the start. Needs a running Android emulator or device with the debug
# app installed (run it through ../run.sh picture, which also removes the photos of earlier runs).
set -euo pipefail
cd "$(dirname "$0")"
ADB="${ADB:-adb}"
APP=com.mikonoma.drivinglog

# Runs one flow of this directory as a manifest of its own over the maestro/ workspace (media must come from inside the workspace).
config="$(mktemp)"; trap 'rm -f "$config"' EXIT
run_flow() { printf 'flows:\n  - picture/%s.yaml\n' "$1" > "$config"; (cd .. && maestro test --config="$config" .); }

# The command is one string for the device's shell: run-as runs it in the app's data directory.
files() { $ADB shell "run-as $APP ls files/pictures 2>/dev/null" | tr -d '\r' | grep -v '^pending$' || true; }
pending() { $ADB shell "run-as $APP ls files/pictures/pending 2>/dev/null" | tr -d '\r' || true; }

check_picture() { # $1 = file name, $2 = expected side, or "max" for "at most 1024"
  local tmp; tmp="$(mktemp)"
  $ADB exec-out "run-as $APP cat files/pictures/$1" > "$tmp"
  # The Python code is the here-document, so the file is passed by name, not on stdin.
  python3 - "$1" "$2" "$tmp" <<'PY'
import struct, sys
name, expected, path = sys.argv[1], sys.argv[2], sys.argv[3]
data = open(path, "rb").read()
assert data[0:4] == b"RIFF" and data[8:12] == b"WEBP", f"{name}: not a WebP file"
kind = data[12:16]
if kind == b"VP8 ":      # lossy
    width, height = struct.unpack("<HH", data[26:30]); width &= 0x3FFF; height &= 0x3FFF
elif kind == b"VP8X":    # extended (alpha)
    width = 1 + int.from_bytes(data[24:27], "little"); height = 1 + int.from_bytes(data[27:30], "little")
else:
    sys.exit(f"{name}: unexpected WebP chunk {kind!r}")
assert width == height, f"{name}: {width}x{height} is not square"
if expected == "max":
    assert 0 < width <= 1024, f"{name}: {width} px is above 1024"
    assert len(data) < 200 * 1024, f"{name}: {len(data)} bytes is not under 200 kB"
else:
    assert width == int(expected), f"{name}: {width} px, expected {expected}"
print(f"ok  {name}: WebP {width}x{height}, {len(data)} bytes")
PY
  rm -f "$tmp"
}

expect_one_picture() {
  local list; list="$(files)"
  [ "$(echo "$list" | grep -c .)" = "2" ] || { echo "expected 2 picture files, found: $list"; exit 1; }
  check_picture "$(echo "$list" | grep -- '-small\.')" 256
  check_picture "$(echo "$list" | grep -- '-large\.')" max
  [ -z "$(pending)" ] || { echo "pending files left: $(pending)"; exit 1; }
  echo "$list" | head -1 | sed 's/-\(small\|large\).*//'
}

echo "== 0. upload the photo, once"
run_flow setup

echo "== 1. add a vehicle with a picture"
run_flow add
first="$(expect_one_picture | tail -1)"

echo "== 2. replace the picture: new files, old files gone"
run_flow replace
second="$(expect_one_picture | tail -1)"
[ "$first" != "$second" ] || { echo "the picture id did not change"; exit 1; }

echo "== 3. remove the picture: no files left"
run_flow remove
[ -z "$(files)" ] && [ -z "$(pending)" ] || { echo "files left after removing: $(files) $(pending)"; exit 1; }

echo "== 4. leave the add screen after cropping: nothing left"
run_flow cancel
[ -z "$(files)" ] && [ -z "$(pending)" ] || { echo "files left after leaving: $(files) $(pending)"; exit 1; }

echo "== 5. the camera app"
run_flow camera

echo "picture files: all checks passed"
