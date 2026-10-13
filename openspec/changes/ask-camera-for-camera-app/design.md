# Design

## Context

`rememberPhotoPicker` (`PhotoPicker.android.kt`) builds one chooser intent and launches it for a result:

- `Intent.createChooser(ACTION_GET_CONTENT image/*)`, with `ACTION_IMAGE_CAPTURE` as an initial intent;
- the capture intent carries an `EXTRA_OUTPUT` FileProvider Uri in `cacheDir/camera`, with grant flags and `ClipData`.

The chooser (`com.android.intentresolver`) starts the chosen target as the app's uid. Since `add-live-scanner`, the app declares
`android.permission.CAMERA`. Android then refuses `ACTION_IMAGE_CAPTURE` from that uid while CAMERA isn't granted (see
`MediaStore.ACTION_IMAGE_CAPTURE`), and the chooser swallows the `SecurityException`. The result is `RESULT_CANCELED`, so the screen just
comes back. The proposal (Why) has the reproduction.

The common side already handles a refusal. `PhotoResult.CameraDenied` → `PictureError.CAMERA_DENIED` → "Camera access is turned off. You
can allow it in the device settings." is wired in `PictureDraftEditor`, `EventPhotoDraft` and `ScanDraft`, and shown by `PictureField`,
`EventNoteAndPhotos` and `LiveScannerContent`. Only iOS produces it today.

The system chooser can't be told to check a permission before it launches a target, and it gives no hook between the choice and the
launch. `EXTRA_CHOOSER_RESULT_INTENT_SENDER` reports the choice only after the launch.

## Goals / Non-Goals

**Goals:**
- Keep the system chooser and its full list, with each camera app shown under its own name and icon.
- Ask for CAMERA between choosing a camera app and opening it. Do this only then, and every time it isn't held.
- One fix in the shared Android picker, so every screen that uses it gets it.

**Non-Goals:**
- Changing the live scanner's permission handling (`LiveCamera.android.kt`).
- Adding Robolectric to the project for this change.

## Decisions

### 1. Camera entries in the chooser point at a small activity of the app

For each activity that resolves `ACTION_IMAGE_CAPTURE` (`PackageManager.queryIntentActivities`), the chooser gets one initial intent: a
`LabeledIntent` with that app's package, label and icon, aimed at a new activity of ours, `CameraCaptureActivity`. It carries the chosen
camera component and the output file. The chooser lists it like any app, so the user still sees "Camera" with the camera app's icon. When it
is chosen, the chooser starts our activity, not the camera. Our activity then does the permission step:

- CAMERA held: start the chosen camera component with `ACTION_IMAGE_CAPTURE`, `EXTRA_OUTPUT`, the grant flags and `ClipData`, and
  `FLAG_ACTIVITY_FORWARD_RESULT`. Then `finish()`. The camera's result goes straight to the picker's launcher, as it does today.
- Not held: `ActivityResultContracts.RequestPermission`.
  - Allowed: do the same as when CAMERA is held.
  - Refused: `setResult(RESULT_CAMERA_DENIED)` (a `RESULT_FIRST_USER`-based code) and `finish()`. This includes the remembered refusal,
    where Android answers "denied" with no dialog, so it needs no code of its own.

The activity has no UI of its own: it uses a translucent no-title theme, so the system permission dialog appears over the screen the user
came from. On rotation it keeps the pending request (`savedInstanceState`) and doesn't ask twice.

`photoOf` checks `RESULT_CAMERA_DENIED` before anything else. It deletes the unused camera file and returns `PhotoResult.CameraDenied`.

Alternatives considered:
- **Ask for CAMERA before opening the chooser.** Rejected: it prompts users who then pick a photo, and the spec forbids asking for a
  permission the chosen way doesn't need.
- **Leave camera apps out of the chooser while CAMERA isn't held (#29, option 2).** Rejected by the owner's desired flow on #29: the chooser
  lists everything, and the user is asked at the choice.
- **`ACTION_PICK_ACTIVITY`.** It returns the chosen activity without launching it, but it can't merge `GET_CONTENT` targets with
  initial intents into one list, and its UI is the old Settings picker, not the system chooser.
- **An in-app chooser or an in-app camera (CameraX capture).** Rejected: `vehicle-picture` forbids drawing our own picker or camera screen.
- **Not declaring CAMERA.** Not possible: the live scanner's preview needs it.
- **Relaying the result instead of `FLAG_ACTIVITY_FORWARD_RESULT`** (the trampoline starts the camera for a result and passes it on). This
  is the fallback if forwarding turns out unreliable through the chooser (see Risks). It costs keeping the trampoline alive under the camera.

### 2. The trampoline is not exported, and it lives in `shared/androidMain`

The class goes next to the picker in `vehicle/picture/`, because the picker is its only user. It is declared in
`androidApp/src/main/AndroidManifest.xml` next to the `.pictures` FileProvider, with `android:exported="false"`. The chooser starts it with
the app's own uid (`startActivityAsCaller`), so it doesn't need to be exported. No other app can drive it to grant write access to our
FileProvider.

### 3. Camera apps are found with a `<queries>` entry

On API 30 and later, `queryIntentActivities(ACTION_IMAGE_CAPTURE)` sees only packages the app declares it queries. The manifest gains
`<queries><intent><action android:name="android.media.action.IMAGE_CAPTURE" /></intent></queries>`. It isn't a permission, so
`maestro/check-permissions.sh` doesn't change. With no camera app, no camera entry is added, which is today's behavior.

### 4. Tests

- The common handling of `CameraDenied` becomes reachable on Android from every picker screen. `PictureDraftEditorTest` and
  `AddVehicleProcessorTest` cover it already. `EventPhotoDraft` and `ScanDraft` get the same unit test (commonTest).
- The new Android code is glue between the chooser, the permission system and the camera app. It can only be checked against the real ones,
  which `docs/test-strategy.md` keeps for Maestro ("the system's photo chooser and the camera app"). Every branch gets a Maestro flow, as
  the owner asked on #29: not held → allow, not held → refuse, refusal remembered, already held, and granted then revoked.

## Risks / Trade-offs

- [The chooser might not forward our trampoline's result, or might not start a non-exported activity of ours] → task 1 is a spike on
  `dl34` that checks both before the rest is built. Fallbacks:
  - result: relaying the result (Decision 1, last alternative);
  - export: an exported trampoline that accepts only a file under `cacheDir/camera` and builds the Uri itself.
- [A `LabeledIntent` label or icon may differ from the camera app's own entry in the chooser] → the label comes from `loadLabel(pm)`, so
  "Camera" stays "Camera" for the existing flows. The spike compares a screenshot of the chooser.
- [Several camera apps on a device each get an entry] → this is wanted (the chooser lists every matching app). Each entry targets its own
  component, so no second resolver appears.
- [Revoking CAMERA in the settings kills the app's process] → the next choice of a camera app asks again; nothing is cached in the app.
  The revoked-path flow checks it.
- [Maestro's `launchApp` grants every permission by default (the cause of #29 going unnoticed)] → the new flow sets
  `permissions: { camera: unset }` (as `distance/scan-reading.yaml` does), and its header says why.
