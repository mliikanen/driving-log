# Tasks

## 1. Spike: asking for the permission between the choice and the launch

- [x] 1.1 On `dl34`, check how to ask for CAMERA between choosing a camera app in the chooser and its launch, and record the outcome in design.md (Risks). Check that:
  - the chooser lists the camera apps as themselves;
  - the chosen app opens with no `SecurityException` in logcat;
  - the photo reaches the picker's launcher with `RESULT_OK`;
  - `RequestPermission` reports "not granted" when the dialog is left with back (the spec counts this as a refusal).

  Outcome: a `LabeledIntent` aimed at an app activity fails the first check, so the design uses the chooser's refinement step instead (Decision 1).

## 2. The camera entries ask for the permission

- [x] 2.1 In `shared/src/androidMain/.../vehicle/picture/CameraChooserRefinement.kt`, add:
  - `CameraRefinementReceiver`: launch any non-camera choice, or a camera while CAMERA is held, as chosen; otherwise start `CameraPermissionActivity`;
  - `CameraPermissionActivity`: ask with `RequestPermission`. Allowed launches the chosen camera app. Refused notes the refusal by output Uri and cancels. Restored across rotation without asking twice.

  Verify that `./gradlew :androidApp:assembleFakeDebug` succeeds.
- [x] 2.2 Update `androidApp/src/main/AndroidManifest.xml`:
  - declare the receiver and the activity (`exported="false"`, the activity translucent and without a title);
  - add `<queries>` for `android.media.action.IMAGE_CAPTURE`;
  - rewrite the FileProvider comment, which says the camera app holds its own permission.

  Verify that `./gradlew :androidApp:assembleFakeDebug` succeeds and that `maestro/check-permissions.sh` still lists only CAMERA.
- [x] 2.3 In `PhotoPicker.android.kt`:
  - add one explicit `ACTION_IMAGE_CAPTURE` initial intent per installed camera app, and set `EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER`;
  - map a cancel with a noted refusal (`consumeDenial`) to `PhotoResult.CameraDenied`, and every other cancel to `Cancelled`, deleting the unused camera file;
  - update the KDoc of `rememberPhotoPicker` (Android and common) and of `PhotoResult.CameraDenied`, which say Android needs no permission.

  Verify by hand on `dl34` after a fresh install: the chooser shows the real "Camera"; choosing it asks for access; Allow opens the camera; Don't allow and back on the prompt show the camera-denied error; leaving the chooser shows nothing.

## 3. Which message each result calls for: unit and processor tests

Each test below sets up both kinds of case. The permission case is `CameraDenied`, which must show the missing-permission message. The
non-permission cases are `Cancelled`, which changes nothing and shows no message, and `Unreadable`, which shows the "could not be
opened" error, not the permission one. They also check that the message is cleared once access is allowed.

- [ ] 3.1 Editor unit tests (commonTest) for each of `PictureDraftEditor`, `EventPhotoDraft` and `ScanEditor` (`ScanEditor` with and without a review open):
  - `CameraDenied` sets `CAMERA_DENIED` and keeps the picture, photos or review;
  - `Cancelled` leaves the state as it was, error included;
  - `Unreadable` sets `COULD_NOT_OPEN`;
  - the dismiss step clears the error.

  Verify with `./gradlew :shared:allTests`.
- [ ] 3.2 Processor tests (commonTest, the screens' MVI) for `AddVehicleProcessor` (picture and scan), `EditVehicleProcessor` (picture), `LogEventProcessor` (event photos and scan) and `EventDetailsProcessor` (photos when editing). For each picker the screen has, dispatch `PhotoPicked` with:
  - `CameraDenied`: the view state carries the camera-denied error;
  - `Cancelled`: no error and nothing else changed;
  - `Unreadable`: the could-not-open error and no camera-denied error.

  Then dispatch the dismiss/allowed intent: the error is gone. Add the dismiss intent where a screen lacks one (event photos). Verify with `./gradlew :shared:allTests`.

## 4. The missing-permission message

- [ ] 4.1 Add `expect fun rememberOpenAppSettings(): () -> Unit`, with Android (`ACTION_APPLICATION_DETAILS_SETTINGS`, moved out of `LiveCamera.android.kt` so `CameraPermission.openSettings` uses it) and iOS (`UIApplication.openSettingsURLString`; the iOS `CameraPermission.openSettings` uses it too). Verify that `./gradlew :shared:allTests :androidApp:assembleDebug` compiles both targets.
- [ ] 4.2 Add the common composable `PermissionMissingMessage(text, onOpenSettings, onAllowed)`: the text, an "Open settings" `Button`, M3 defaults and theme colors only, and test tags. On `ON_RESUME` it re-checks camera access and calls `onAllowed` when access is held. Verify that `codeQuality` passes (detekt/ktlint for the new file).
- [ ] 4.3 Use it in `PictureField` (add and edit vehicle) for `CAMERA_DENIED` only, wiring `onAllowed` to `PictureErrorDismissed`. `COULD_NOT_OPEN` keeps its plain text. Verify on `dl34` with the flows in 5.1 and 5.2.
- [ ] 4.4 Use it in `EventNoteAndPhotos` (log event form and event edit), wired to the photo error's dismiss intent. Verify on `dl34` with the flows in 5.3.
- [ ] 4.5 Use it in `LiveScannerContent` (replacing `NoCamera` and the `CAMERA_DENIED` text) and in `ScanReviewContent`, which shows no error today. Wire `onAllowed` to `ScanErrorDismissed`. Make `rememberCameraPermission` re-check on resume, so the preview appears when the user is back with access allowed. Verify on `dl34` with the flows in 5.4, and by hand that allowing access in the settings brings the preview back.

## 5. Maestro: the device-only part of each case

What the processor tests (3.2) can't reach goes here: the system prompt and how it is left, the settings page, the remembered refusal and
the revoke, and what the screen draws. Each area gets one flow with the permission cases and one with the non-permission cases. The owner
asked for these on #29, even though Maestro normally covers only the happy path (`docs/test-strategy.md`). A Compose UI test can't
replace them here: the project has no Compose UI test tooling, and the permission paths need the real chooser and permission system. The
permission flows launch with `permissions: { camera: unset }` and say why in their header.

- [ ] 5.1 `maestro/picture/camera-permission.yaml` (vehicle picture, add form):
  1. Camera → prompt → "Don't allow": no picture, and the message with "Open settings".
  2. "Open settings": the app's settings page opens. Back: the message is still there.
  3. Camera → prompt left with back: the message is shown, as for a refusal.
  4. Camera → "Don't allow" again: the refusal is remembered. Camera once more: no prompt, the message only.
  5. "Open settings" → Permissions → Camera → allow, then back: the message is gone, and Camera opens the camera with no prompt.
  6. Relaunch with `camera: deny` (a revoke): Camera prompts again → "While using the app" → "Shutter" → photo → crop → "Use photo".

  Choosing the photo picker shows no prompt and no message throughout. Verify that `maestro/run.sh picture` passes.
- [ ] 5.2 `maestro/picture/photo-errors.yaml` (vehicle picture, non-permission): leave the chooser → no message. Camera (granted) → back without a photo → no message. Choose a photo that can't be opened → "The picture could not be opened" and no "Open settings". Add the unreadable photo to `maestro/assets` (see its README and `docs/licensing.md`): first try a truncated JPEG, and if the emulator's media store rejects it, use an image over `MAX_PHOTO_BYTES`. Verify that `maestro/run.sh picture` passes.
- [ ] 5.3 `maestro/distance/photo-permission.yaml` and `maestro/distance/photo-errors.yaml` (the log event form's "Add photo"):
  - permission flow: refused → message below the strip, photos kept; "Open settings" opens the app's page; back → still shown;
  - errors flow: leave the chooser → no message; unreadable photo → "The photo could not be opened" without "Open settings".

  Add both to `maestro/manifests/distance.yaml` (explicit paths and order). Verify that `maestro/run.sh distance` passes.
- [ ] 5.4 `maestro/distance/scan-permission.yaml` and `maestro/distance/scan-errors.yaml` (live scanner and photo review):
  - permission flow: "Scan a reading" → refuse → the scanner shows the message in place of the preview with "Open settings". The photo action's Camera, refused → message on the scanner. A no-reading photo → review → "choose another photo" → Camera, refused → message on the review.
  - errors flow: from the scanner and from the review, leave the chooser → no message. Unreadable photo from the scanner → "The photo could not be opened" without "Open settings".

  Add both to the `distance` manifest. Update `scan-reading.yaml` if it checks the scanner's old refused text. Verify that `maestro/run.sh distance` passes.
- [ ] 5.5 Wire 5.1 and 5.2 into `maestro/picture/run.sh` after the camera step. Update `maestro/picture/camera.yaml`'s header: it covers the already-granted path, since `launchApp` grants CAMERA, and the app no longer asks for nothing. Update the area list in `CLAUDE.md` or `maestro/README` if it names the flows. Verify that `maestro/run.sh picture distance vehicles` passes.

## 6. Regression

- [ ] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh` (no Maestro), and verify both pass.

## Workflow follow-up

- Mark the PR ready with a `## Release notes` section. For example: "Taking a photo with the camera (Add picture, an event's photos, the
  scanner) now asks for camera access the first time; it used to do nothing. If you say no, the screen tells you why and offers 'Open
  settings' to turn it on."
- Archive the change (`/opsx:archive`), then merge `change/ask-camera-for-camera-app` into `main`. Close #29 with the merge.
