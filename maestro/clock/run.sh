#!/usr/bin/env bash
# Runs time-format.yaml once with the device on a 24-hour clock and once on a 12-hour clock, then restores the setting.
set -euo pipefail
cd "$(dirname "$0")"
ADB="${ADB:-adb}"
original="$($ADB shell settings get system time_12_24 | tr -d '\r')"
restore() { if [ "$original" = "null" ]; then $ADB shell settings delete system time_12_24; else $ADB shell settings put system time_12_24 "$original"; fi; }
trap restore EXIT

$ADB shell settings put system time_12_24 24
maestro test time-format.yaml -e TIME_PATTERN='\d{2}:\d{2}' -e TWELVE_HOUR=false

$ADB shell settings put system time_12_24 12
maestro test time-format.yaml -e TIME_PATTERN='\d{1,2}:\d{2} (AM|PM)' -e TWELVE_HOUR=true
