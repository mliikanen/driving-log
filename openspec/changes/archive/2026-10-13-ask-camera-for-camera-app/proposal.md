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

- The chooser lists every app that can provide an image, with the installed camera apps, up to two, each listed as itself (its own name and icon),
  whether or not camera access has been allowed.
- When the user picks a camera app and the app doesn't hold the camera permission, the system asks for it right then, before the camera opens.
  - Allowed: the chosen camera app opens, and taking a photo continues to the crop screen as today.
  - Refused (including a refusal Android remembers and no longer asks about): no camera opens. The user is back on the screen they came
    from with its picture or photos unchanged, and the screen shows the missing-permission message (below).
  - Already held: the camera opens with no prompt.
  - Granted earlier and later revoked in the device settings: the next time the camera is chosen, the system asks again.
- Choosing a photo, file or other image app still asks for nothing. Opening the chooser asks for nothing.
- **New: one missing-permission message.** Whenever something the user asked for can't be done because a permission is missing, the
  screen shows the same message component. It says what can't be done and which access is turned off ("A photo can't be taken because
  camera access is turned off"). Its "Open settings" button opens the app's own page in the device settings. When the user comes back
  with the access allowed, the message goes away. The message appears only when a permission is missing. Leaving the chooser, the
  camera or a photo app without a photo is not a permission case: it changes nothing and shows no message, as today. Other failures,
  such as a photo that can't be opened, keep their plain error text.
  This replaces the three copies of the text-only "Camera access is turned off…" line and the live scanner's own "needs camera access"
  view.
- This covers every screen that uses this chooser or the camera, on both platforms:
  - the vehicle picture (add and edit);
  - an event's photos (log event form and event edit), through `EventPhotoDraft`;
  - the live scanner, both its photo action and its no-preview state, and the scan review's "choose another photo", through
    `ScanDraft`. The scan review drops a refusal today without showing anything.
- The live scanner shows its preview as soon as the user is back from the settings with camera access allowed. Before, it waited until
  the scanner was opened again.
- `vehicle-picture`, `app-shell` and `odometer-ocr-capture` stop claiming that the Android camera app needs no permission. They state
  the rule above instead.

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
- `app-shell`: adds two requirements. "A missing permission is explained with a way to the settings" is the shared message with "Open
  settings". "The missing-permission message is shown only for a missing permission" counts a refusal, an earlier refusal or a
  dismissed prompt, and nothing else: leaving a chooser or the camera, or another failure, never shows it. Also changes
  "The application requests the minimum set of permissions":
  - the platform note and the scenario "A permission is asked for at the action" now name taking a photo with the camera app on Android
    as a function that asks for the camera permission, which the app declares for the live scanner anyway;
  - the refusal scenario now points to the shared message.
- `distance-logging`: adds "A refused camera is shown on the photo element" (log event form and event edit).
- `odometer-ocr-capture`: "A photo is chosen to scan for a reading" drops "this application requests no permission for it". It adds "A
  refused camera is shown on the scan screen it was chosen from", for the scanner's photo action and the review. "The camera permission is asked for when the scanner is first
  opened" uses the shared message, and its preview returns when the user comes back with access allowed.

## Impact

- `shared/src/androidMain/.../vehicle/picture/PhotoPicker.android.kt`: the chooser's camera entries go through the app first, and the
  result maps a refusal to `PhotoResult.CameraDenied`. The common `PhotoResult.CameraDenied` doc changes too.
- A new common composable for the missing-permission message, with an `expect` for opening the app's settings page. Android has that
  already in `LiveCamera.android.kt`; iOS gets it. The message is used by:
  - `PictureField`, `EventNoteAndPhotos` and `LiveScannerContent`, in place of their `CAMERA_DENIED` text and `NoCamera`;
  - `ScanReviewContent`, which shows no error today.
- Back from the settings, the screens re-check camera access and clear the message (existing `…ErrorDismissed` intents, plus one for
  event photos if it is missing). `rememberCameraPermission` re-checks on resume.
- New in the Android app (`CameraChooserRefinement.kt`):
  - a non-exported receiver for the chooser's refinement step, which runs after the user picks an app and before it is launched;
  - a translucent, non-exported activity that asks for the permission when a camera app was picked without it.

  `androidApp/src/main/AndroidManifest.xml` gains both and a `<queries>` entry for `IMAGE_CAPTURE`, and the FileProvider comment
  changes. No permission is added: CAMERA is already
  declared, so `maestro/check-permissions.sh` is unaffected.
- Maestro: a new flow in the `picture` device-state group covers the permission paths and the "Open settings" button. The existing
  `picture/camera.yaml` keeps covering the already-granted path. The `distance` flows of the live scanner's refused state follow the new
  message.
- No data, migration or dependency change.
