# Proposal

## Why

On Android, choosing the camera app in the "Add picture" chooser does nothing until the app holds the camera permission (#29). Since
the live scanner declared `android.permission.CAMERA`, Android refuses an `ACTION_IMAGE_CAPTURE` started by the app while that
permission isn't granted. The chooser launches the camera as the app, so the camera never opens. The user is back on the form with no
photo and no message. This is how a fresh install behaves: CAMERA is a runtime permission, and it is not granted until the user allows it.
It was reproduced on a clean install of the fake debug build on the `dl34` emulator, with no `pm revoke` and no Maestro `launchApp`. The
log shows `Permission Denial … with revoked permission android.permission.CAMERA`. The camera-intents guide says no permission is needed,
but that holds only for an app that doesn't declare CAMERA. The `MediaStore.ACTION_IMAGE_CAPTURE` reference documents the exception.
`vehicle-picture` and `app-shell` both state the opposite of what happens.

## What Changes

- The chooser still lists every app that can provide an image, the camera apps included, as it does today.
- When the user picks a camera app and the app doesn't hold the camera permission, the system asks for it right then, before the camera opens.
  - Allowed: the chosen camera app opens, and taking a photo continues to the crop screen as today.
  - Refused (including a refusal Android remembers and no longer asks about): no camera opens. The user is back on the screen they came
    from with its picture or photos unchanged, and the existing message "Camera access is turned off. You can allow it in the device
    settings." is shown. This is the message iOS already shows.
  - Already held: the camera opens with no prompt.
  - Granted earlier and later revoked in the device settings: the next time the camera is chosen, the system asks again.
- Choosing a photo, file or other image app still asks for nothing. Opening the chooser asks for nothing.
- This covers every screen that uses this chooser: the vehicle picture (add and edit), an event's photos, and the photo action of the
  live scanner and the scan review.
- `vehicle-picture` and `app-shell` stop claiming that the Android camera app needs no permission. They state the rule above instead.

Out of scope:
- iOS, which already asks for camera access at Take Photo and shows the same message.
- The live scanner's own permission request (`odometer-ocr-capture`), which is unchanged.
- An in-app camera screen or an in-app picker. The spec forbids both, and this change keeps the system chooser.
- Leaving camera apps out of the chooser while CAMERA isn't granted. This was considered in #29 and rejected in favor of asking at the choice.
- Dropping the CAMERA declaration. The live scanner's in-app preview needs it.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-picture`: the chooser requirement's camera scenarios and platform note. "Permissions are asked for only when needed" changes: on
  Android, choosing the camera app asks for the camera permission when the app doesn't hold it, a refusal shows the camera-access message,
  and opening the chooser or choosing any other app still asks for nothing.
- `app-shell`: "The application requests the minimum set of permissions". The platform note and the scenario "A permission is asked for at
  the action" now name taking a photo with the camera app on Android as a function that asks for the camera permission, which the app
  declares for the live scanner anyway.

## Impact

- `shared/src/androidMain/.../vehicle/picture/PhotoPicker.android.kt`: the chooser's camera entries go through the app first, and the
  result maps a refusal to `PhotoResult.CameraDenied`. The common `PhotoResult.CameraDenied` doc changes too. The screens already show its
  message (`PictureField`, `EventNoteAndPhotos`, `LiveScannerContent`).
- A new small, non-exported activity in the Android app. It asks for the permission and then hands over to the chosen camera app.
  `androidApp/src/main/AndroidManifest.xml` gains it, and the FileProvider comment changes. No permission is added: CAMERA is already
  declared, so `maestro/check-permissions.sh` is unaffected.
- Maestro: a new flow in the `picture` device-state group covers the permission paths. The existing `picture/camera.yaml` keeps covering
  the already-granted path.
- No data, migration or dependency change.
