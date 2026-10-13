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
- Changing when the live scanner asks for its permission (`LiveCamera.android.kt`). Only how a refusal is shown changes.
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
`RESULT_CAMERA_DENIED` is the only path to `CameraDenied`. A cancel anywhere stays `RESULT_CANCELED`, so it stays
`PhotoResult.Cancelled`, which changes nothing and shows no message. That covers:
- leaving the chooser;
- leaving our activity before it decides. Back during the permission dialog is not a cancel: `app-shell` specifies it as a refusal.
  `RequestPermission` reports it as "not granted", so it ends in `RESULT_CAMERA_DENIED`;
- leaving the camera app.

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

### 4. One missing-permission message, with "Open settings"

There is one common composable, `PermissionMissingMessage(text, onOpenSettings)`. It is built from the live scanner's `NoCamera` (text,
`Button("Open settings")`), with M3 defaults and theme colors only. It replaces:
- the `CAMERA_DENIED` branch of the error text in `PictureField`, `EventNoteAndPhotos` and `LiveScannerContent`;
- `NoCamera` itself.

`ScanReviewContent` shows it too. Today it drops `ScanDraft.error` without showing it.

The message is driven by state that already exists: `PictureError.CAMERA_DENIED` in `PictureEditState`, `EventPhotoDraft` and
`ScanDraft`, and `CameraPermission.granted` for the scanner's no-preview state. `COULD_NOT_OPEN` keeps its plain error text, and
`Cancelled` sets no error. So whether the message is shown is decided in common code, where unit tests can check it.

"Open settings" uses an `expect fun rememberOpenAppSettings(): () -> Unit`. It reuses `CameraPermission.openSettings`:
- Android: `ACTION_APPLICATION_DETAILS_SETTINGS`, moved out of `LiveCamera.android.kt` so both share it;
- iOS: `UIApplication.openSettingsURLString`.

Coming back is handled by a lifecycle `ON_RESUME` check inside the composable. If camera access is held by then, it calls `onAllowed`,
which the screen maps to its existing `…ErrorDismissed` intent. Event photos get one if they lack it. `rememberCameraPermission`
re-checks `granted` on resume, so the scanner's preview appears without reopening it.

Alternative: put "Open settings" into each screen's error text. Rejected because the message must be the same everywhere.

### 5. Tests: both kinds of case, each at the lowest level that can check it

Every surface gets tests for both the permission case and the non-permission cases. The owner asked for this on #29.
- **Which error a result produces** is common logic. Unit tests cover it for the editors (`PictureDraftEditor`, `EventPhotoDraft`,
  `ScanEditor`), and processor tests for every screen with a picker (`AddVehicle`, `EditVehicle`, `LogEvent`, `EventDetails`). For each
  picker: `CameraDenied` → camera-denied error; `Cancelled` → nothing, no error; `Unreadable` → could-not-open error, never the
  camera-denied one. The dismiss/allowed intent clears it.
- **What only the device has** goes in Maestro: the system prompt and how it is left, the remembered refusal, the revoke, "Open settings"
  landing on the app's page, the settings round trip, and what the screen draws (the message with its button, or the plain error without
  it). There is one permission flow and one non-permission flow per area: vehicle picture (`picture` group), event photos and scanner/review
  (`distance` manifest). This goes beyond Maestro's usual happy path (`docs/test-strategy.md`) because the owner asked for it. The
  project has no Compose UI test tooling for the rendering, and the permission branches need the real chooser and permission system.

## Risks / Trade-offs

- [The spec counts a prompt left without an answer as a refusal (`app-shell`); this relies on `RequestPermission` reporting "not granted"
  for it] → the spike (1.1) checks this. If Android reports nothing, the trampoline treats an empty answer as a refusal too.

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
