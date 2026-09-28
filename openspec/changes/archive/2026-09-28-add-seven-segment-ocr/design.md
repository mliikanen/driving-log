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
- Read the Shiver's LCD readings from the whole photo, with no extra step for the user.
- No change to what the car photos give today.

**Non-Goals:**
- Marking a region of the photo (proposal.md, Out of Scope: measured to add nothing here).
- A model trained by us.

## Decisions

### The engine: PP-OCRv6 detection (tiny) and the English PP-OCRv5 recognizer, on ONNX Runtime
Chosen by the evaluation below, against the bar fixed before it: the exact reading on at least 5 of the 6 LCD photos,
no loss on the car photos, under 1.5 s per scan on the emulator, and no more than 20 MB added to the APK. This pair
reads 5 of 6 LCD photos whole (all but the motion-blurred `142.0`), in about 430 ms per photo on the desktop, and its
two models are 9.7 MB together (1.8 MB detection, 7.9 MB recognition). On the car photos it misses `71140`, which
ML Kit reads, and both run (next decision), so the car photos give what they gave before. The English recognizer is
the smaller and the more accurate one here: the multilingual PP-OCRv6 recognizers are larger (4.5–21 MB) and read
fewer LCD photos (1–5 of 6 with the tiny detector). Both models are Apache-2.0 (PaddleOCR; the ONNX conversions are
RapidOCR's) and ONNX Runtime is MIT: they are listed in `THIRD_PARTY_NOTICES.md`.

The rejected candidates and why are in the evaluation below: Tesseract read none of the six, the decoder of our own
did not converge, and marking a region never read more than the whole photo did.

### The PP-OCR pipeline is a port of RapidOCR's, and the arithmetic around the models is common code
The evaluation's numbers come from RapidOCR's pipeline, so it is ported with its constants rather than reinvented:
- The photo (as `ImageCodec` decodes it) is scaled into 30–2000 px on its longer side.
- Detection: scaled so its shorter side is at least 736 px, then each side rounded to a multiple of 32; normalized
  with mean and deviation 0.5; the model gives a probability map. Pixels above 0.3, dilated by a 2×2 kernel, form
  regions; each region's minimum-area rotated rectangle (fewer than 3 px on its short side is dropped) is kept when
  the map's mean inside it is at least 0.5, grown by the unclip distance (area × 1.6 / perimeter on every side) and
  scaled back to photo pixels; a rectangle 3 px or less on a side is dropped.
- Recognition: each rectangle is cropped along its own axes (turned upright when it is 1.5 times taller than wide),
  scaled to 48 px high keeping its aspect ratio, normalized to [-1, 1] and padded to the batch's width; the model's
  per-column scores are decoded greedily (CTC: the best class per column, repeats and the blank dropped) with the
  character list stored in the model's own metadata (a blank first, a space last). A line whose mean score is below
  0.5 is dropped.
- A line's words are its text split on spaces, each given the matching share of the line's box by character
  position, so candidate detection sees words as it does from ML Kit.

Only running the two models needs a platform (ONNX Runtime's Android library). The geometry (regions, rotated
rectangles, unclip), the crop sampling, the CTC decoding and the word boxes are pure Kotlin in common code, unit-tested
there. A JVM test runs the real models over the real photos through ONNX Runtime's JVM library and must reproduce the
evaluation (5 of 6 LCD readings), so the port is checked against what was measured, without an emulator.

### Two workarounds for ONNX Runtime's Java API, found by the JVM test
Porting surfaced two problems the Python runs never showed, both in ONNX Runtime's Java binding rather than the models:
- **`HardSwish` returns zeros** on the JVM build (1.22 to 1.30 tried, Linux x86-64; a one-node model reproduces it), so the
  recognizer read nothing. The recognition model's 28 `HardSwish` nodes are rewritten offline as the equivalent
  `x * HardSigmoid(x, 1/6, 0.5)`; outputs are bit-identical in Python. Whether ONNX Runtime's Android build shares the bug
  is not known, and does not matter: the app ships the rewritten model.
- **Model metadata loses characters outside the Basic Multilingual Plane**, which shifted the recognizer's character list
  (433 entries instead of 436, so spaces vanished). The list is shipped as a file next to the model.

How both files were made is in `androidApp/src/main/assets/ocr/README.md`.

### Both recognizers run, and their results are merged before candidate detection
A combining `TextRecognizer` runs ML Kit and PP-OCR on the same decoded photo (same pixels, so the boxes agree) and
returns the lines of both. Candidate detection then keeps one candidate per place: when two candidates' boxes overlap,
the one with more digits wins (ML Kit's `71140` over PP-OCR's `140` on `odo/20250831_073743`); with the same
digits, one presented as a candidate over one that is not, then the labeled one, else the first. "The same place" is an
intersection of at least 30% of the smaller box. Classification is otherwise unchanged.

### An unlabeled reading without a unit counts when plausible as the odometer, or when it has a decimal
PP-OCR returns `3056` and `209.1` with no label and no unit next to them. The unit rule (`add-odometer-ocr-capture`)
exists to drop dial numbers (`120`, `240`, `RPM x1000`): whole numbers far below the odometer. So an unlabeled number
without a unit is now classified too, but more narrowly than one with a unit: as an odometer reading when its
magnitude says odometer, and as a trip only when it has a decimal (a trip meter's tenths). A unit-less whole number
that would read as a trip by magnitude alone is still dropped. The clock (`1621`) is dropped unless the known
odometer happens to be just below it, an accepted risk: the user passes it over on the review screen.

### APK size: arm64 only in a release, native libraries compressed
ONNX Runtime's Android library is 23–39 MB of native code per ABI, stored uncompressed across four ABIs: adding it
took the debug APK from 82.8 MB to 226.4 MB. Its arm64 library alone (33 MB) and the models (9.7 MB) are over the
20 MB cap by themselves, so, as this decision foresaw, the ABIs are limited and the native libraries compressed
(`useLegacyPackaging`): a release carries arm64-v8a only (every phone at this minSdk is 64-bit, and testers' phones
are arm64), a debug build arm64-v8a and x86_64 (the emulator). Measured (task 2.1):

| APK | Before this change | After |
|---|---|---|
| Release (signed, what testers install) | 73.3 MB (four ABIs of ML Kit) | 58.0 MB |
| Debug | 82.8 MB (four ABIs) | 88.9 MB (two ABIs) |

The recognizer's own share of the release is about 22 MB compressed (ONNX Runtime 13.4 MB, models 8.9 MB), but the
release as a whole shrinks by 15 MB, because ML Kit's three other ABIs go with the same filter. An x86 or 32-bit ARM
device can no longer install a release build; none is in the tester group.

### Evaluation results (task 1.2)

Run on the desktop (the same engines and models run unchanged on Android; timings are desktop CPU), over the six
marked regions in `maestro/assets/ocr/lcd-regions.json` and the twelve photos whole. A region was tried with padding of
half and one times its height around the box, scaled so the digits are 32, 48 or 64 px tall; "best setting" is the one
setting that read the most regions exactly.

| Engine | Marked region, best setting | LCD photo whole | Car photos whole | Size added |
|---|---|---|---|---|
| ML Kit (today) | 0/6 (`5368` in 2 of 12 variants) | 0/6 | all six readings | 0 |
| PaddleOCR PP-OCRv3 mobile, ONNX | **4/6** (pad 1×, 32 px): `5094` for 5034, `12.0` for 142.0 | 3/6 (5368, 3056, 209.1) | all six readings | runtime + 13 MB models |
| PaddleOCR PP-OCRv5 mobile, ONNX | 0/6 | 3/6 (5368, 168.1, 209.1) | 71140 on 1 of 3 | runtime + 21 MB models |
| PaddleOCR PP-OCRv6 small, ONNX | 2/6 (3/6 over all settings) | **4/6** (5368, 3056, 168.1, 209.1) | 71140 on 1 of 3, others right | runtime + 31 MB models |
| Tesseract 5.5 + `ssd`/`ssd_int`/`letsgodigital` | 0/6 (also from binarized, deskewed input) | not run | not run | native lib + 0.2–11 MB |
| Seven-segment decoder of our own (prototype) | 0/6 | n/a (region only) | n/a | 0 |

ONNX Runtime for Android is a 53 MB AAR across all ABIs (a single ABI's library is a fraction of that; a reduced
"minimal" build is smaller still, but is its own build step). Desktop time per region was 200–500 ms for the Paddle
models and about 1 s per whole photo for PP-OCRv6.

Notes: the decoder's binarization and slant correction make every reading legible to the eye, but splitting touching
italic digits and scoring segments did not converge in the time given; it is not ruled out, only unproven. No engine
misread 1621 (the clock) as a reading with a unit. No engine meets the bar (5 of 6 regions); the closest are
PP-OCRv3 on a marked region (4/6) and PP-OCRv6 on the whole photo (4/6, with no marking at all).

Then, for the chosen engine family, the whole-photo pass over all twelve photos with other model pairs:

| Detection + recognition | LCD photos | Car photos | Models | Desktop time |
|---|---|---|---|---|
| PP-OCRv6 det small + rec small | 4/6 | 4/6 | 31 MB | 1070 ms |
| PP-OCRv6 det tiny + rec tiny | 1/6 | 3/6 | 6.3 MB | 270 ms |
| PP-OCRv6 det small + rec tiny | 1/6 | 4/6 | 14.4 MB | 870 ms |
| PP-OCRv6 det tiny + rec small | 5/6 | 3/6 | 23.1 MB | 390 ms |
| PP-OCRv6 det small + en PP-OCRv5 rec | 4/6 | 4/6 | 17.8 MB | 1020 ms |
| **PP-OCRv6 det tiny + en PP-OCRv5 rec** | **5/6** | 3/6 | **9.7 MB** | **430 ms** |

The car readings it misses are `71140` (three photos), which ML Kit reads.

### The whole scan on the emulator (task 3.4)

ML Kit and PP-OCR together, then detection and classification, on the `dl34` emulator (x86-64, 4 CPUs, debug build), with a known
odometer just below each photo's reading:

| Photo | Candidates presented |
|---|---|
| `odo/20220911_162029` (LCD) | **5034** odometer (label `ODO`) |
| `odo/20230530_170748` (LCD) | **5368** odometer (label `ODO`) |
| `trip/20220706_112423` (LCD) | **3056** odometer (magnitude, no label or unit read) |
| `trip/20220911_162031` (LCD) | **168.1** trip (label `TRIP`) |
| `trip/20230624_212428` (LCD) | **209.1** trip (magnitude, with tenths) |
| `trip/20220706_132307` (LCD, motion-blurred) | none ("no reading found") |
| the six car photos | as before this change: 71140, 32478, 50961, 16865 odometer, and the ranges (917, 890, 870) as trips |

A scan takes 0.70–1.25 s once the models are loaded (PP-OCR 0.32–0.70 s, ML Kit 0.29–0.44 s, on the same four cores);
the first scan in a process also loads both models and took 4.4 s. ONNX Runtime's threads spin-wait between operators by
default: with that, PP-OCR alone took a median 3.6 s here, so its sessions run with spinning off and at most four
threads (median 0.66 s). Timed on the emulator, not a phone: an arm64 phone runs the same code on different cores.

## Risks / Trade-offs

- **[Risk]** Six LCD photos, one motorcycle model: an engine can meet the bar and still miss another LCD →
  **Mitigation**: accepted for now; the bar is on the data we have, and the stored photo and detections of every
  accepted scan (`add-odometer-ocr-capture`) are what a future misdetection is diagnosed from.
- **[Risk]** The Kotlin port drifts from RapidOCR's pipeline and reads less than was measured → **Mitigation**: the
  JVM test over the real photos with the real models must reproduce the 5 of 6.
- **[Risk]** ONNX Runtime's native libraries push the APK over the cap → **Mitigation**: measured in the task that
  adds it; the ABIs are limited if needed (Decisions, APK size).
- **[Trade-off]** Running two recognizers makes a scan slower (0.7–1.25 s on the emulator, against 0.3–0.45 s for ML Kit
  alone) → accepted within the 1.5 s cap.
- **[Trade-off]** The first scan in a process loads the models (4.4 s on the emulator) → accepted; the review screen's
  "Reading the photo…" state covers it. Loading them when the log event form opens is a possible follow-up.
