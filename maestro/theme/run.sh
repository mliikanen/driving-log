#!/usr/bin/env bash
# Checks the Petroleum theme on a running emulator or device by sampling the pixels of screenshots in light and in dark mode:
# the header, the screen background, the floating action button, the color of a logged distance, and (cold start) the launch window's
# background. It changes the device's light/dark mode and restores it. Needs the debug app installed and the device on gesture
# navigation with a 1080 x 2400 screen (the sample points are in pixels).
set -euo pipefail
cd "$(dirname "$0")"
ADB="${ADB:-adb}"
APP=com.mikonoma.drivinglog
shots="$(mktemp -d)"
original_mode="$($ADB shell cmd uimode night | tr -d '\r' | sed 's/.*: //')"
restore() { $ADB shell cmd uimode night "$original_mode" >/dev/null; rm -rf "$shots"; }
trap restore EXIT

# Samples a PNG: prints the #RRGGBB at each "x,y" given, or with "find:#RRGGBB:x0,y0,x1,y1" the count of pixels near that color in the box.
sample() {
  python3 - "$@" <<'PY'
import struct, sys, zlib
path, *queries = sys.argv[1:]
data = open(path, "rb").read()
pos, idat, width, height, ctype = 8, b"", 0, 0, 0
while pos < len(data):
    length, kind = struct.unpack(">I4s", data[pos:pos + 8])
    body = data[pos + 8:pos + 8 + length]
    if kind == b"IHDR": width, height, depth, ctype = struct.unpack(">IIBB", body[:10]); assert depth == 8
    if kind == b"IDAT": idat += body
    pos += 12 + length
bpp = {2: 3, 6: 4}[ctype]
raw = zlib.decompress(idat); stride = width * bpp
rows, prev = [], bytearray(stride)
for y in range(height):
    f = raw[y * (stride + 1)]; line = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
    for i in range(stride):
        a = line[i - bpp] if i >= bpp else 0; b = prev[i]; c = prev[i - bpp] if i >= bpp else 0
        if f == 1: line[i] = (line[i] + a) & 255
        elif f == 2: line[i] = (line[i] + b) & 255
        elif f == 3: line[i] = (line[i] + (a + b) // 2) & 255
        elif f == 4:
            p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
            line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
    rows.append(line); prev = line
def px(x, y): o = x * bpp; r = rows[y]; return "#%02X%02X%02X" % (r[o], r[o + 1], r[o + 2])
for q in queries:
    if q.startswith("find:"):
        _, color, box = q.split(":"); x0, y0, x1, y1 = map(int, box.split(","))
        # Small text is anti-aliased and its strokes may never reach full coverage, so "the color" is a pixel within 30 per channel of it.
        want = [int(color[i:i + 2], 16) for i in (1, 3, 5)]
        def near(h): return all(abs(int(h[i:i + 2], 16) - w) <= 30 for i, w in zip((1, 3, 5), want))
        print(sum(1 for y in range(y0, y1) for x in range(x0, x1) if near(px(x, y))))
    else:
        x, y = map(int, q.split(",")); print(px(x, y))
PY
}

shot() { $ADB exec-out screencap -p > "$1"; }
expect() { # $1 = what, $2 = actual, $3 = expected
  if [ "$2" != "$3" ]; then echo "FAIL $1: $2, expected $3"; exit 1; fi; echo "ok   $1: $2"
}

echo "== state"; $ADB shell cmd uimode night no >/dev/null; maestro test state.yaml >/dev/null

for mode in light dark; do
  if [ "$mode" = light ]; then night=no; background='#F4F7F6'; fab='#203A43'; distance='#006F53'; else night=yes; background='#12181B'; fab='#8FC3D7'; distance='#06D6A0'; fi
  echo "== $mode mode"
  $ADB shell cmd uimode night $night >/dev/null
  maestro test list.yaml >/dev/null; sleep 1; shot "$shots/list-$mode.png"
  expect "$mode header (behind the status bar)" "$(sample "$shots/list-$mode.png" 900,80)" '#0F2027'
  expect "$mode header (the app bar)" "$(sample "$shots/list-$mode.png" 900,220)" '#0F2027'
  expect "$mode screen background" "$(sample "$shots/list-$mode.png" 540,1200)" "$background"
  expect "$mode floating action button" "$(sample "$shots/list-$mode.png" 680,2220)" "$fab"
  maestro test details.yaml >/dev/null; sleep 1; shot "$shots/details-$mode.png"
  found="$(sample "$shots/details-$mode.png" "find:$distance:820,1650,1060,2100")"
  [ "$found" -gt 30 ] || { echo "FAIL $mode distance color $distance: only $found pixels in the distance row"; exit 1; }
  echo "ok   $mode distance color $distance: $found pixels"

  # Cold start: capture frames as fast as possible; no frame may be pure white or pure black (the window shows the scheme's background).
  $ADB shell am force-stop $APP; sleep 1
  $ADB shell am start -n $APP/.MainActivity >/dev/null
  for i in 1 2 3 4 5 6; do shot "$shots/cold-$mode-$i.png"; done
  for i in 1 2 3 4 5 6; do
    corner="$(sample "$shots/cold-$mode-$i.png" 30,1500)"
    case "$corner" in '#FFFFFF'|'#000000') echo "FAIL cold start $mode, frame $i: $corner (a flash of a color that is not the theme's)"; exit 1;; esac
  done
  echo "ok   $mode cold start: no white or black frame ($(sample "$shots/cold-$mode-1.png" 30,1500) ... $(sample "$shots/cold-$mode-6.png" 30,1500))"
done
echo "theme: all checks passed"
