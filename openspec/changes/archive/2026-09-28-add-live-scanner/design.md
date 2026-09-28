# Design

## Context

See proposal.md for why. What exists (`add-odometer-ocr-capture`, `add-seven-segment-ocr`):

- `TextRecognizer.recognize(bytes)`: a JPEG/PNG in, a `RecognizedPhoto` (lines, words, boxes in decoded-photo pixels)
  out. On Android it is a `CombinedTextRecognizer` of `MlKitTextRecognizer` and `PpOcrTextRecognizer`, each of which
  decodes the bytes itself. One scan takes 0.7–1.25 s on the `dl34` emulator once the models are loaded; loading them
  takes about 4 s the first time.
- `detectReadings(photo, knownOdometer)` classifies; `ScanEditor` and the log event processor hold the photo review
  (`ScanDraft.review`) and the accepted scan (`ScanDraft.accepted`, a `PendingCapture` saved with the entry).
- The photo review is full-screen content inside the log event screen (like the note editor), and the scan action is
  hidden where `TextRecognizer.isAvailable` is false (iOS).
- The app requests no system permission today; `maestro/check-permissions.sh` fails if it does.

## Goals / Non-Goals

**Goals:**
- A live preview with readings boxed as they are read, a tap to apply one, and a way out without one.
- The same recognizers and classification as a photo scan, so a reading reads the same either way.
- The photo flow unchanged, one tap away.

**Non-Goals:**
- Real-time frame rates: the recognizers take about a second a frame on the emulator, so the boxes update about once a
  second; the preview itself is live.
- Torch, zoom, tap-to-focus, the front camera (proposal.md, Out of Scope).

## Decisions

### CameraX: a `PreviewView` and an `ImageAnalysis` that keeps only the latest frame
The preview is CameraX's `PreviewView` in an `AndroidView` (its Compose viewfinder is still alpha), scale type
`FILL_CENTER`. Frames come from `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` and RGBA output, at a 16:9 resolution
near 1280 × 720 (the size the recognizers were measured at), analyzed one at a time on a single background thread: while
a frame is being read, newer ones are dropped, so the analysis never falls behind the view. Preview and analysis are
both 16:9 streams of the same camera, bound to the scanner's lifecycle, so the whole analysis frame maps onto the
preview with `FILL_CENTER`'s own geometry (next decisions); a `UseCaseGroup` with the view's `ViewPort` turned out not
to be needed. Frames arrive as RGBA and are turned upright by their rotation before recognition.

The camera is Android-only: the scanner's camera area is an `expect` composable whose iOS `actual` is never shown (the
action is hidden there), matching the photo picker's platform split.

Alternatives: CameraX's Compose `CameraXViewfinder` (alpha only); ML Kit's `MlKitAnalyzer` (ML Kit only, and PP-OCR must
see the same frames); Camera2 directly (far more code for the same result).

### Frames go to the same recognizers, as bitmaps
Each recognizer gains a bitmap entry point next to the bytes one (ML Kit's `InputImage.fromBitmap` already takes one;
PP-OCR takes the `RgbImage` its pipeline works on); the bytes entry point becomes "decode, then the bitmap one". A
frame is turned upright by its `rotationDegrees` first, so boxes come back in upright frame pixels, as a photo's are in
upright photo pixels. The combined recognizer does the same for frames as for photos.

### Readings are tracked across frames
`detectReadings` runs on each frame, and a pure `LiveReadings` tracker (common code) merges the result into what is
shown: a reading found again at about the same place (same value, boxes overlapping, the rule `onePerPlace` already
uses) keeps its identity and moves to its new box; a new one appears; one not found again stays shown for 1.5 s after it
was last seen, then goes. A frame's recognition takes about a second, so without the grace period a reading the
recognizers miss in one frame would flicker off and on. Each tracked reading keeps a reference to the frame it was last
seen in, with that frame's full detection result, so a tap can keep exactly what the reading was read in.

The known odometer used for classification is the form's (`LogEventState.knownOdometer`), as for a photo.

### Boxes are placed with the preview's own geometry
Frame pixels map onto the screen as `FILL_CENTER` places the frame: scaled by the larger of the two view/frame ratios and
centered, the overflow cropped. That mapping is a pure function (unit-tested); boxes and labels are drawn like the
photo review's (the neutral outline, the label with kind and value, 4 dp outside the number), tappable, at least 48 dp as
touch targets.

### A tap applies the reading; what is kept is the frame
Tapping a tracked reading dispatches one intent carrying the reading and its frame. The processor encodes the frame
once (the same full-size WebP a photo scan keeps), puts it in the capture store as the pending scan, records the frame's
detections with the tapped one as accepted, applies it to the form exactly as a confirmed photo candidate is applied
(`withScannedReading`), and closes the scanner. An earlier accepted scan is replaced and its file deleted, as today.

### The scanner is part of the log event screen, like the photo review
`ScanDraft` gains `scannerOpen`: "Scan a reading" opens it (after the permission, next decision), its close action and
back close it. The photo action runs today's photo flow on top of it: the photo review shows while `review` is set, and
leaving it without accepting returns to the scanner, because `scannerOpen` is still true; confirming a candidate closes
both. Persisted like the rest of `ScanDraft`, so a rotation keeps the scanner open (CameraX rebinds to the new lifecycle).

### The camera permission, at the action
"Scan a reading" checks the camera permission; if it is not granted it asks for it (the Activity Result
`RequestPermission` contract), and opens the scanner whatever the answer. Granted, the scanner shows the preview;
refused, it shows the explanation and a button to the app's settings page, and the photo action works as always. The
answer is read each time the scanner opens, so allowing it later in the settings takes effect the next time. The
manifest declares `android.permission.CAMERA` and `<uses-feature android:name="android.hardware.camera"
android:required="false"/>`, so a device without a camera can still install the app (and use the photo flow).
`maestro/check-permissions.sh` allows exactly `android.permission.CAMERA` besides the app's own.

Alternatives: asking on entering the scanner (the project's rule is "at the action", and the tap is the action); falling
back to the photo chooser on refusal (the user asked for the scanner; saying why and offering the photo button is
clearer).

### Permissions in Maestro
Maestro grants every permission when it launches the app unless told otherwise, so the `scan-reading` flow launches with
the camera permission unset, to go through the real request. (Checked by hand on the emulator: refusing shows the
explanation, the photo button opens the chooser, and cancelling the chooser returns to the scanner.)

### Warm-up
The recognizers load their models on the first frame (about 4 s on the emulator). The preview shows at once; until the
first frame has been read, the scanner says "Getting ready…" so that no boxes does not look like nothing readable.

## Risks / Trade-offs

- **[Risk]** A live reading can be misread in one frame and right in the next → **Mitigation**: the box shows the value
  it will apply ("ODO 71140"), and tracking replaces a reading's value with the one read most recently at its place.
- **[Risk]** The emulator's camera cannot show a dashboard, so live detection cannot be checked end to end in Maestro →
  **Mitigation**: the frame path is checked on the JVM with the real photos as frames (recognition, classification,
  tracking, mapping), Maestro covers the scanner's own flow (open, permission, leave, photo action), and the developer
  checks the live preview on a phone before archiving.
- **[Trade-off]** Continuous recognition uses the CPU and battery while the scanner is open → accepted: one frame at a
  time, only while the scanner is open, stopped when it closes.
- **[Trade-off]** The app now asks for a system permission → required for an in-app preview; limited to the camera,
  asked only at "Scan a reading", and the photo flow needs none.
