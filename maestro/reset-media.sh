#!/usr/bin/env bash
# Removes the test photos the flows have put on the emulator and rescans its media store. Every flow that needs a picture adds it itself (addMedia, from
# maestro/assets), so a fresh emulator needs nothing; but each run adds another copy ("photo-landscape (12).png"), and after a few dozen the emulator's addMedia
# starts to fail ("StatusRuntimeException: UNKNOWN") or the picker shows no photos. Run this then. Needs adb (ADB=/path/to/adb if it is not on the PATH).
set -euo pipefail
ADB="${ADB:-adb}"
$ADB shell 'rm -f /sdcard/Pictures/photo-*'
$ADB shell 'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures' > /dev/null
echo "test photos removed from /sdcard/Pictures"
