# Proposal

## Why

"Scan a reading" today means taking or choosing a photo, waiting for it to be read, then picking the reading on a
review screen: several steps and a round trip through another app for something the user does standing next to the
vehicle. A scanner inside the app that shows the camera's view with the readings boxed as they are read, where a tap on
a reading is all it takes, makes that one step. The photo flow stays for when the camera cannot get a clear view
(glare, an old photo, a refused camera permission).

## What Changes

- **"Scan a reading" opens an in-app scanner** instead of the system's photo chooser: the camera's live preview, full
  screen, with every reading the recognizers find boxed and labeled ("ODO 71140", "TRIP 168.1") as the view changes,
  using the same recognizers (ML Kit and PP-OCR) and the same classification as the photo flow.
- **Tapping a reading applies it**: the scanner closes, the log event form's field takes the value and its way
  switches to match, exactly as accepting a candidate on the photo review screen does today. There is no separate
  confirm step: the tap is the confirmation.
- **Leaving the scanner** (its close action or back) returns to the form unchanged, with nothing kept.
- **A floating photo button on the scanner** opens today's photo flow (system chooser, review screen) unchanged; its
  outcome is the same as today.
- **The camera permission**: the live preview needs the app's own camera access, which no system chooser or intent can
  provide. It is asked for when the user taps "Scan a reading" the first time, not before; if refused, the scanner says
  why it has no preview and how to allow it, and the photo button still works. This is the app's first system
  permission, so the "no system permission" rule and `maestro/check-permissions.sh` change to allow exactly this one.
- **What is kept for a live scan**: the camera frame the tapped reading was read in, and every detection in that frame,
  stored and discarded under the same rules as a photo scan's.

## Assumptions Recorded for Review

- Tapping a reading applies it at once (no "Use" step), as the request says; the photo review screen keeps its
  select-then-Use flow.
- Leaving the photo flow's review screen without accepting returns to the scanner (where the user came from), not to
  the form.
- The kept "photo" of a live scan is the analyzed camera frame, not a separately captured full-resolution photo: it is
  exactly what the boxes were read in, which is what reviewing a misdetection needs.

## Out of Scope

- iOS: the scan action stays hidden on iOS.
- Zoom, focus-on-tap, the torch, and choosing between cameras: the back camera with autofocus only. (The torch is a
  likely follow-up for dim dashboards.)
- Opening the scanner from the Home screen (the earlier follow-up idea), and suggesting a vehicle from a reading
  (`suggest-vehicle-from-odometer`).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `odometer-ocr-capture`: "Scan a reading" opens the live scanner (the photo flow is reached from it); a tapped live
  reading is accepted; the camera permission; what is kept for a live scan.
- `app-shell`: the permissions requirement allows the one camera permission the live scanner needs, and its platform
  note says so.

## Impact

- New Android dependencies: CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`); the
  `CAMERA` permission in the manifest.
- `vehicle/ocr/`: frame analysis feeding the existing recognizers, and keeping the readings steady between frames.
- A new scanner screen (Android; hidden on iOS as the action is), wired into the log event form's scan intents.
- `maestro/check-permissions.sh` allows `android.permission.CAMERA`; the `distance` manifest's `scan-reading` flow
  reaches the photo flow through the scanner's photo button.
