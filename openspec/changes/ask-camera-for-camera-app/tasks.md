# Tasks

## 1. Spike: the chooser and a trampoline of our own

- [ ] 1.1 On `dl34`, with a throwaway build: add one `LabeledIntent` initial entry aimed at a non-exported activity of the app that starts the camera with `FLAG_ACTIVITY_FORWARD_RESULT`. Verify three things: the chooser lists it as "Camera" with the camera app's icon, choosing it starts our activity (no `SecurityException` in logcat), and the photo reaches the picker's launcher with `RESULT_OK`. Record the outcome in design.md (Risks). If forwarding or the non-exported start fails, switch to that risk's fallback before group 2.

## 2. The camera entries ask for the permission

- [ ] 2.1 Add `CameraCaptureActivity` in `shared/src/androidMain/.../vehicle/picture/`. If CAMERA is held, it starts the given camera component with `ACTION_IMAGE_CAPTURE`, `EXTRA_OUTPUT`, the grant flags, `ClipData` and `FLAG_ACTIVITY_FORWARD_RESULT`, then finishes. If not, it asks with `RequestPermission`: allowed does the same, refused sets `RESULT_CAMERA_DENIED` and finishes. It keeps a pending request across rotation. Verify that `./gradlew :shared:compileAndroidMain` (or the Android compile task the build uses) succeeds.
- [ ] 2.2 Declare the activity in `androidApp/src/main/AndroidManifest.xml` (`exported="false"`, translucent no-title theme). Add `<queries>` for `android.media.action.IMAGE_CAPTURE`. Rewrite the FileProvider comment ("No permission is declared: the camera app holds its own" is no longer the whole story). Verify that `./gradlew :androidApp:assembleFakeDebug` succeeds and that `maestro/check-permissions.sh` still lists only CAMERA.
- [ ] 2.3 In `PhotoPicker.android.kt`, replace the single `ACTION_IMAGE_CAPTURE` initial intent with one `LabeledIntent` per resolved camera activity, aimed at `CameraCaptureActivity`. Map `RESULT_CAMERA_DENIED` to `PhotoResult.CameraDenied` in `photoOf` and delete the unused camera file. Update the KDoc of `rememberPhotoPicker` and of `PhotoResult.CameraDenied`/`rememberPhotoPicker` in commonMain, which say Android needs no permission. Verify by hand on `dl34` (fresh install): choosing Camera asks, Allow opens the camera, and Don't allow shows "Camera access is turned off…".

## 3. Common handling of a refusal, now reachable on Android

- [ ] 3.1 Add a unit test to `EventPhotoDraft`'s tests: `PhotoResult.CameraDenied` sets `CAMERA_DENIED` and keeps the photos. Verify it passes with `./gradlew :shared:allTests`.
- [ ] 3.2 Add a unit test to `ScanDraft`'s tests: `PhotoResult.CameraDenied` sets `CAMERA_DENIED` and keeps the draft's photo and reading. Verify it passes with `./gradlew :shared:allTests`.

## 4. Maestro: every branch of the permission

- [ ] 4.1 Add `maestro/picture/camera-permission.yaml`, launched with `permissions: { camera: unset }` and a header saying why. In order: Camera → prompt → "Don't allow" → back on the add form, message shown, no picture. Camera → prompt (the second ask) → "Don't allow" → remembered refusal. Then relaunch with camera unset again and check: Camera → prompt → "While using the app" → camera opens ("Shutter") → photo → crop → "Use photo". Last, relaunch with `camera: deny` (a revoke) and check that Camera prompts again. Choosing the photo picker throughout shows no prompt. Verify that `maestro/run.sh picture` passes.
- [ ] 4.2 Wire the flow into `maestro/picture/run.sh` after the camera step. Update the header comment of `maestro/picture/camera.yaml` (it covers the already-granted path, since `launchApp` grants CAMERA, and no longer claims the app asks for nothing). Verify that `maestro/run.sh picture` runs both flows.
- [ ] 4.3 Run the manifests of the other screens that use the picker, `vehicles` and `distance` (event photos, scan review and live scanner photo action). Verify that `maestro/run.sh vehicles distance` passes.

## 5. Regression

- [ ] 5.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh` (no Maestro), and verify both pass.

## Workflow follow-up

- Mark the PR ready with a `## Release notes` section. For example: "Taking a photo with the camera from Add picture or an event's photos now
  asks for camera access the first time (it used to do nothing). If you say no, you'll see how to turn it on in the settings."
- Archive the change (`/opsx:archive`), then merge `change/ask-camera-for-camera-app` into `main`. Close #29 with the merge.
