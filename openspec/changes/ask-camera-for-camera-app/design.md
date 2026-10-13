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

The system chooser has one hook between the user's choice and the launch: `EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER` (API 24). When the
user picks a target, the chooser sends that `IntentSender` the chosen intent (`EXTRA_INTENT`) and a `ResultReceiver`
(`EXTRA_RESULT_RECEIVER`). It waits for the answer: `RESULT_OK` with an intent to launch, or `RESULT_CANCELED` to launch nothing.
`EXTRA_CHOSEN_COMPONENT_INTENT_SENDER` reports the choice only after a successful launch, so it doesn't help.

## Goals / Non-Goals

**Goals:**
- Keep the system chooser and its full list, with every installed camera app shown as itself, under its own name and icon.
- Ask for CAMERA between choosing a camera app and opening it. Do this only then, and every time it isn't held.
- One fix in the shared Android picker, so every screen that uses it gets it.

**Non-Goals:**
- Changing when the live scanner asks for its permission (`LiveCamera.android.kt`). Only how a refusal is shown changes.
- Adding Robolectric to the project for this change.

## Decisions

### 1. Real camera entries, and the permission is asked in the chooser's refinement step

For each activity that resolves `ACTION_IMAGE_CAPTURE` (`PackageManager.queryIntentActivities`), the chooser gets one initial intent:
`ACTION_IMAGE_CAPTURE` aimed at that camera component, with `EXTRA_OUTPUT`, the grant flags and `ClipData`. The chooser lists each as
the camera app itself, under its own name and icon. That means the device's own camera apps, not just the emulator's, and whether or
not CAMERA is held. At most two: the system chooser keeps only the first two initial intents (`MAX_INITIAL_INTENTS = 2`, `take(2)` in
IntentResolver's `ChooserRequestReader`), so the app passes the first two it resolves and says so, rather than leaving the cut to the
chooser. The image apps (`GET_CONTENT` targets) are the chooser's own and are not capped; the owner judged more than two image apps far
likelier than more than two camera apps.

The chooser's refinement step goes to `CameraRefinementReceiver`, a non-exported broadcast receiver reached through the app's own mutable
`PendingIntent`:

- **Not a camera app** (a photo or file app), **or a camera app while CAMERA is held:** answer `RESULT_OK` with the chosen intent at
  once. No UI, no prompt; the chooser launches it as today.
- **A camera app without CAMERA:** start `CameraPermissionActivity` (non-exported, translucent, no UI of its own) with the chosen intent
  and the `ResultReceiver`. It asks with `RequestPermission`, and the system prompt shows over the chooser.
  - Allowed: answer `RESULT_OK` with the chosen intent. The chooser launches the camera app the user picked, now that CAMERA is held, and
    the camera's result comes to the picker's launcher as before.
  - Refused (now, a refusal Android remembers and answers at once, or the prompt left with back, which `RequestPermission` reports as
    not granted): note the refusal for that photo's output Uri, then answer `RESULT_CANCELED`. The chooser closes and the picker gets
    `RESULT_CANCELED`. `photoOf` reads and forgets the note (`consumeDenial`), and returns `PhotoResult.CameraDenied` for a noted
    refusal and `PhotoResult.Cancelled` otherwise.

The note is needed because the chooser accepts a refined intent only for the target the user picked. The first build answered a refusal
with `RESULT_OK` and an intent for an activity of the app that returns a "denied" result code; the chooser launched nothing. The note is
kept in memory, in the one process both run in, and keyed by the output Uri, which is new for every launch of the picker. A refusal lost
to a process restart reads as a cancel.

`CameraDenied` comes only from a noted refusal. Every other way of leaving stays `PhotoResult.Cancelled`, which changes nothing and shows
no message:
- leaving the chooser;
- leaving a photo app;
- leaving the camera app.

Alternatives considered:
- **Route camera entries through an activity of the app (`LabeledIntent`).** This was the first design. The spike showed that the chooser
  shows such an entry under the app's own name and icon ("Driving Log", with the label as a subtitle), not the camera app's. That holds
  even with a label and icon on the activity, so the user would not see their camera apps as themselves.
- **Ask for CAMERA before opening the chooser.** Rejected: it prompts users who then pick a photo, and the spec forbids asking for a
  permission the chosen way doesn't need.
- **Leave camera apps out of the chooser while CAMERA isn't held (#29, option 2).** Rejected by the owner: the chooser always lists the
  real camera apps.
- **Camera apps as `EXTRA_ALTERNATE_INTENTS` (to avoid the two-entry cap).** IntentResolver reads that extra uncapped, but on `dl34` the
  camera was not listed at all: the photo picker was the only target and opened at once.
- **The camera as the chooser's target and the image apps as initial intents (to avoid the cap).** Rejected by the owner: it would cap
  the image apps at two instead, and a device far more often has more than two image apps than camera apps.
- **`ACTION_PICK_ACTIVITY`.** It returns the chosen activity without launching it, but it can't merge `GET_CONTENT` targets with
  initial intents into one list, and its UI is the old Settings picker, not the system chooser.
- **An in-app chooser or an in-app camera (CameraX capture).** Rejected: `vehicle-picture` forbids drawing our own picker or camera screen.
- **Not declaring CAMERA.** Not possible: the live scanner's preview needs it.

None of this is a Kotlin Multiplatform limit. The chooser, the launch as the app's uid and the permission check are Android's; an
Android-only app would face the same.

### 2. The receiver and the activity are not exported, and live in `shared/androidMain`

They are in `vehicle/picture/CameraChooserRefinement.kt`, next to the picker, which is their only user. They are declared in
`androidApp/src/main/AndroidManifest.xml` next to the `.pictures` FileProvider, with `android:exported="false"`. Only the chooser reaches
the receiver, through the app's own `PendingIntent`, and the receiver starts the activity, so no other app can drive them.

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

**Spike outcome (task 1.1, `dl34`, API 34, fake debug build):**
- First design (an activity of the app as each camera's chooser entry): the start, the result forwarding and the prompt all worked, but the
  entry showed as "Driving Log" with the app's icon, not as the camera app. That failed the owner's requirement that the real camera apps
  are listed. Replaced by Decision 1.
- Refinement design: the chooser calls the refinement step for the real camera entry before launching it, with the chosen
  `IMAGE_CAPTURE` intent, and waits for the answer. Verified paths:
  - refuse → camera-denied error;
  - photo app after a refusal → no prompt;
  - prompt left with back → camera-denied error;
  - allow → the real camera app → crop screen;
  - already allowed → camera with no prompt.
- The chooser ignores a refined intent for a different activity than the one the user picked. Hence the noted refusal in Decision 1.
- Not checked on the emulator: a device with several camera apps (the emulator has one). Each of the first two is its own initial
  intent, so each is listed; a third is not (the chooser's cap, Decision 1).

- [The spec counts a prompt left without an answer as a refusal (`app-shell`); this relies on `RequestPermission` reporting "not granted"
  for it] → verified in the spike (1.1).
- [The refinement step depends on the system chooser honoring `EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER` (verified on API 34's
  IntentResolver; an OEM chooser might not)] → without it, a camera app picked without CAMERA fails as in #29, and every other path is
  unaffected. The permission flows on a real device are the check.

- [Several camera apps on a device each get an entry, but at most two (the chooser's initial-intent cap, found in PR review)] →
  accepted by the owner. Each entry targets its own component, so no second resolver appears.
- [Revoking CAMERA in the settings kills the app's process] → the next choice of a camera app asks again; nothing is cached in the app.
  The revoked-path flow checks it.
- [Maestro's `launchApp` grants every permission by default (the cause of #29 going unnoticed)] → the new flow sets
  `permissions: { camera: unset }` (as `distance/scan-reading.yaml` does), and its header says why.
