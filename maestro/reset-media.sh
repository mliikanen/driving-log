#!/usr/bin/env bash
# Removes the test photos the flows have put on the emulator (every image in maestro/assets, by name, with the copies a re-upload numbers) and
# rescans its media store. Every flow that needs a picture adds it itself (addMedia, from
# maestro/assets), so a fresh emulator needs nothing; but each run adds another copy ("photo-landscape (12).png"), and after a few dozen the emulator's addMedia
# starts to fail ("StatusRuntimeException: UNKNOWN") or the picker shows no photos. Run this then. Needs adb (ADB=/path/to/adb if it is not on the PATH).
set -euo pipefail
ADB="${ADB:-adb}"
cd "$(dirname "$0")"
# "DSC_0896.jpg" → /sdcard/Pictures/DSC_0896*.jpg, which also matches "DSC_0896 (12).jpg".
patterns="$(find assets -type f \( -name '*.jpg' -o -name '*.png' \) | sed -E 's|.*/||; s|^(.*)(\.[a-z]+)$|"/sdcard/Pictures/\1"*\2|' | sort -u | tr '\n' ' ')"
$ADB shell "rm -f $patterns"
$ADB shell 'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures' > /dev/null
echo "test photos removed from /sdcard/Pictures"
