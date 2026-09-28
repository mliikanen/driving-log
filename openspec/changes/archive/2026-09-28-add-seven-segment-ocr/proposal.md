# Proposal

## Why

`add-odometer-ocr-capture` reads dashboard photos with ML Kit's on-device Latin text recognizer. Measured on the real
test photos (in `maestro/assets/ocr/`), that works for car clusters drawn in ordinary fonts, but it cannot read a
reading shown in seven-segment LCD digits, the display a motorcycle (and many older cars) uses: on all six Aprilia
Shiver photos it finds no reading at all, so the scan always ends in "no reading found" for that vehicle. Neither
preprocessing nor the user pointing at the reading fixes that with ML Kit (design.md, Context), so the problem is the
recognizer, and this change adds a second one that reads these digits.

**This depends on `add-odometer-ocr-capture`** (archived): the scan action, the review screen, the classification
and the stored detections it builds on are introduced there.

## What Changes

- A second, offline, on-device recognizer (PaddleOCR's PP-OCR detection and recognition models on ONNX Runtime,
  chosen by a measured evaluation, design.md) runs alongside ML Kit on the scanned photo, so a reading like the
  Shiver's "ODO 5034 Km" or "TRIP 168.1 Km" becomes a candidate, classified and reviewed exactly like one read from a
  car cluster. A reading both recognizers find is presented once.
- The classification accepts an unlabeled reading with no unit after it when it is plausible as the odometer, or has
  a decimal (a trip meter's tenths): the LCD readings often come back with neither label nor unit, and the dial
  numbers the unit rule exists to drop are whole numbers far from the odometer.

## Out of Scope

- **Marking the reading's region on the photo** (the original proposal's "Mark the reading"). The evaluation showed it
  reads nothing the whole-photo pass does not (design.md, Evaluation results), so it is dropped from this change; it
  can come back as its own proposal if a display turns up that needs it.
- A live camera viewfinder (a separate, future proposal, as in `add-odometer-ocr-capture`).
- iOS: the scan action stays hidden on iOS, and so does everything this change adds.
- Training a model of our own, and displays that neither recognizer reads (the motion-blurred `142.0` photo is one).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `odometer-ocr-capture`: readings in seven-segment digits are detected (an added requirement; the existing ones are
  unchanged).

## Impact

- New Android dependency: ONNX Runtime for Android, plus two PP-OCR model files (about 10 MB) in the app's assets;
  the APK grows by what the measurement task records, under the cap in design.md.
- `vehicle/ocr/`: a second `TextRecognizer` (Android), a combining recognizer, deduplication in candidate detection,
  and the relaxed unit rule; the PP-OCR pre- and post-processing that is pure (detection map to boxes, CTC decoding)
  in common code with unit tests.
- `maestro/assets/ocr/`: the photos are the evaluation set; a JVM test reads them with the real models.
- The `distance` manifest gains a case scanning an LCD photo.
