# Design

## Context

See proposal.md for why. What was measured while applying `add-odometer-ocr-capture` (ML Kit `text-recognition`
16.0.1, bundled model, on the `dl34` emulator, through a temporary instrumented harness), against the photos now
checked in under `maestro/assets/ocr/`:

| Photo | Display | Reading on it | ML Kit, whole photo | ML Kit, hand-marked region |
|---|---|---|---|---|
| `odo/20250831_073738` | car, printed | ODO 71140 km | `ODO` + `71140km` | not needed |
| `odo/20250831_073743` | car, printed | ODO 71140 km | `1140knm` (2× upscale: `71140km`) | not needed |
| `odo/20250831_073744` | car, printed | ODO 71140 km | `OD0` + `1140` (2×: `ODO` + `71140`) | not needed |
| `odo/20251109_193736` | car, printed | 32478 km (no label) | `32478 km` | not needed |
| `odo/20251228_190558` | car, printed | 50961 km (no label) | `50961 km` | not needed |
| `odo/20260221_193742` | car, printed | 16865 km over "Total distance" | `16865 em` / `Total distance` | not needed |
| `odo/20220911_162029` | Shiver LCD | ODO 5034 Km | nothing | `ODO` only, never the digits |
| `odo/20230530_170748` | Shiver LCD | ODO 5368 Km | nothing | `5368` in 2 of 12 variants |
| `trip/20220706_112423` | Shiver LCD | ODO 3056 Km (top line 0.00) | nothing | not tried |
| `trip/20220706_132307` | Shiver LCD | TRIP 142.0 Km | nothing | not tried |
| `trip/20220911_162031` | Shiver LCD | TRIP 168.1 Km | nothing | fragments only (`.1`, `3. 1`) |
| `trip/20230624_212428` | Shiver LCD | TRIP 209.1 Km | nothing | `209. 1` in about half, also `2D9, 1`, `9. 1` |

"Variants" were the region at 600, 1000 and 1600 px wide, plain and grayscale with raised contrast, from a tight and a
loose box. Whole-photo attempts also tried 2× and 3× upscaling, contrast, and 3×3 overlapping tiles: none found an LCD
digit. The Shiver's digits are italic seven-segment glyphs about 25 px tall in a 983×1310 photo, often behind glare
and dust.

What `add-odometer-ocr-capture` provides that this builds on: the `TextRecognizer` interface
(`recognize(bytes): RecognizedPhoto?`, lines and elements with boxes in decoded-photo pixels), the pure candidate
detection and classification, the review screen, and the serialized detection result stored in `event_capture`.

## Goals / Non-Goals

**Goals:**
- Read the Shiver's LCD readings: with a marked region reliably, from the whole photo where the engine allows.
- No change to what the car photos give today.

**Non-Goals:**
- Choosing an engine by preference instead of by measurement.
- A model trained by us (proposal.md, Out of Scope).
- Region marking as a precision tool (zoom, handles, rotation): one drag, one box.

## Decisions

### The engine is chosen by a measured evaluation, against a bar fixed now
Candidates, all offline and on-device on Android:

1. **A seven-segment decoder of our own, in common Kotlin.** Binarize the region, find digit-sized blobs, correct the
   slant, and test the seven segment positions of each blob; a decimal point is a small blob on the baseline. No
   dependency, no APK growth, runs on iOS later unchanged, and is unit-testable on the JVM over the real photos. It
   needs a region (a whole busy photo has too many blobs), so it serves the marked-region path; the whole-photo path
   would stay ML Kit only.
2. **Tesseract** through an Android binding, with a published seven-segment trained-data file (for example the
   `ssd` / `letsgodigital` sets). Established, but a native library plus trained data (several MB), and the
   trained data's license must allow redistribution in the APK.
3. **PaddleOCR's mobile detection and recognition models on ONNX Runtime.** A general scene-text recognizer that is
   commonly better than ML Kit on display digits, and can run on the whole photo; the largest addition (runtime plus
   models, around 10–15 MB).

The bar, over the six LCD photos with hand-marked regions (the regions are checked in with the photos, task 1.1):
- **Marked region:** the exact reading (all digits and the decimal point) on at least 5 of 6.
- **Whole photo:** reported, not required. An engine that also reads LCD readings from the whole photo is preferred
  when two meet the region bar.
- **No regression:** the six car photos give the same candidates as with ML Kit alone.
- **Cost:** under 1.5 s for one region on the emulator; the APK grows by no more than 20 MB.

Among engines that meet the bar, the order of preference is 1, 3, 2: no dependency beats a dependency, and ONNX
Runtime is a better-supported runtime than a Tesseract binding. If none meets the bar, the apply stops and the
results come back as a decision (proposal.md, Out of Scope); the tasks after the evaluation do not start.

Alternatives considered: pre-committing to one engine (rejected: the ML Kit assumption in `add-odometer-ocr-capture`
is exactly what did not survive contact with the photos); ML Kit with more preprocessing (rejected: measured above).

### Both recognizers run, and their results are merged before candidate detection
The chosen engine is a second `TextRecognizer`. A combining recognizer runs both on the same decoded photo (the same
pixels, so the boxes agree) and returns the union of their lines. Candidate detection deduplicates: two candidates
with the same value whose boxes overlap are one, keeping the box with the label next to it if only one has one.
Candidate detection and classification themselves are unchanged. An engine that only works on a region (candidate 1)
contributes only to the marked-region path.

### A region is recognized as a crop, and the boxes are mapped back
`TextRecognizer` gains an optional region: the recognizer crops the decoded photo to the box (plus a small margin),
scales the crop to a fixed working width, recognizes, and maps every box back into whole-photo pixels. The review
screen, the classification and the stored detections therefore only ever see whole-photo coordinates.

### Marking is one drag on the review screen
"Mark the reading" puts the review screen into a marking mode: a drag draws a box over the photo (outline in the
theme's primary, the rest dimmed with a scrim, both from the color scheme), "Use this area" confirms, back cancels.
The box is converted from screen to photo pixels with the same transform that places the candidate boxes. The
processor keeps the confirmed box in its state, so it survives recreation.

### The marked box is an optional field of the stored detection result
The detection result is serialized into `event_capture.detections` (`add-odometer-ocr-capture`); it gains a nullable
`markedRegion`. A result stored without one reads back with null, so no migration and no fixture regeneration.

## Risks / Trade-offs

- **[Risk]** Six LCD photos, one motorcycle model: an engine can meet the bar and still miss another LCD →
  **Mitigation**: accepted for now; the bar is on the data we have, and the stored photo and detections of every
  accepted scan (`add-odometer-ocr-capture`) are what a future misdetection is diagnosed from.
- **[Risk]** A decoder of our own is tuned to the Shiver's font → **Mitigation**: the segment test is on relative
  positions within a digit's box, not on pixel sizes, and the unit tests use every LCD photo, not one.
- **[Risk]** A trained-data or model license that does not allow redistribution → **Mitigation**: checked in the
  evaluation task, before the engine is chosen.
- **[Trade-off]** Running two recognizers makes a scan slower → accepted within the cost bar; the region path only
  runs on the user's request.
