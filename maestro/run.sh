#!/usr/bin/env bash
# Runs Maestro manifests (see ../docs/test-strategy.md). A manifest is manifests/<area>.yaml: the flows of maestro/<area>/ and their order, the setup flow
# (which uploads the area's test photos, once) first. Needs a running emulator or device with the debug app installed.
#
#   run.sh vehicles              one manifest            run.sh vehicles distance     several
#   run.sh vehicles edit         the manifest's setup, then the flow maestro/vehicles/edit.yaml
#   run.sh --all                 every plain manifest    run.sh picture theme clock   the device-state groups (they change or inspect device state over adb)
#
# Before the first manifest the test photos earlier runs left on the emulator are removed (reset-media.sh; each upload adds another copy, and after a few
# dozen the emulator's addMedia fails). Without adb that is skipped: a fresh emulator has none.
set -euo pipefail
cd "$(dirname "$0")"

PLAIN="vehicles distance resilience appearance"
DEVICE_STATE="picture theme clock"

usage() {
  echo "usage: run.sh <area>... | <area> <flow> | --all"
  echo "  plain manifests:        $PLAIN"
  echo "  device-state groups:    $DEVICE_STATE"
  exit 2
}
is_plain() { [[ " $PLAIN " == *" $1 "* ]]; }
is_device_state() { [[ " $DEVICE_STATE " == *" $1 "* ]]; }

[ $# -gt 0 ] || usage
if [ "$1" = "--all" ]; then
  [ $# -eq 1 ] || usage
  areas=($PLAIN)
elif [ $# -eq 2 ] && is_plain "$1" && ! is_plain "$2" && ! is_device_state "$2"; then
  single_area="$1"; single_flow="${2%.yaml}"
  [ -f "$single_area/$single_flow.yaml" ] || { echo "no flow $single_area/$single_flow.yaml"; usage; }
  areas=("$single_area")
else
  areas=("$@")
  for area in "${areas[@]}"; do is_plain "$area" || is_device_state "$area" || { echo "unknown area: $area"; usage; }; done
fi

ADB="${ADB:-adb}"
if ! command -v "$ADB" >/dev/null 2>&1 && [ -x "$HOME/Android/Sdk/platform-tools/adb" ]; then ADB="$HOME/Android/Sdk/platform-tools/adb"; fi
if command -v "$ADB" >/dev/null 2>&1; then ADB="$ADB" ./reset-media.sh; else echo "adb not found: old test photos are not removed (set ADB=/path/to/adb)"; fi

config="$(mktemp)"; trap 'rm -f "$config"' EXIT
for area in "${areas[@]}"; do
  if is_device_state "$area"; then
    echo "== $area (device state)"; ADB="$ADB" "./$area/run.sh"; continue
  fi
  echo "== $area"
  if [ -n "${single_flow:-}" ]; then
    # One flow: a config listing the setup (when the area has one) and that flow, in that order.
    { echo "flows:"; [ -f "$area/setup.yaml" ] && echo "  - $area/setup.yaml"; echo "  - $area/$single_flow.yaml"
      echo "executionOrder:"; echo "  continueOnFailure: false"; echo "  flowsOrder:"; [ -f "$area/setup.yaml" ] && echo "    - setup"; echo "    - $single_flow"; } > "$config"
    maestro test --config="$config" .
  else
    maestro test --config="manifests/$area.yaml" .
  fi
done
