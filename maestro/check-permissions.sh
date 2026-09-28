#!/usr/bin/env bash
# The application requests the minimum set of permissions it needs (see the project context). This lists what the installed debug app
# requests on a running emulator or device and fails when it asks for any other system permission: the only ones allowed are the app's own
# (`com.mikonoma.drivinglog.*`, added by AndroidX for internal receivers) and the camera, which the live scanner's in-app preview needs
# (add-live-scanner; no chooser or intent can provide one). The other image functions use the system chooser and the camera app
# through intents, which need none.
set -euo pipefail
ADB="${ADB:-adb}"
APP=com.mikonoma.drivinglog

requested="$($ADB shell dumpsys package "$APP" | tr -d '\r' | awk '/requested permissions:/{f=1;next} f&&/^    [A-Za-z]/{f=0} f&&NF{print $1}' | sed 's/:$//' | sort -u)"
echo "requested permissions:"; echo "${requested:-  (none)}" | sed 's/^/  /'
system="$(echo "$requested" | grep -v "^$APP\." | grep -v '^android.permission.CAMERA$' | grep -v '^$' || true)"
if [ -n "$system" ]; then
  echo "FAIL: the app requests system permissions it should not need unless a function requires them:"; echo "$system" | sed 's/^/  /'
  exit 1
fi
echo "ok: no system permission is requested but the camera"
