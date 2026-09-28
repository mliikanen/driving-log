# Tasks

## 1. Evaluation (decides the engine; nothing after it starts until it passes)

- [x] 1.1 Hand-mark the reading's region on each of the six LCD photos in `maestro/assets/ocr/` and check the boxes in
      with them (a small JSON beside the photos, listing photo, box and the expected reading from design.md, Context);
      document the file in `maestro/assets/README.md`. Verify: every LCD photo has one entry.
- [x] 1.2 Build each candidate engine from design.md far enough to recognize a region (the decoder of our own as a
      prototype; Tesseract and ONNX Runtime in a temporary instrumented harness, not checked in) and run them over the
      six marked regions, the six LCD photos whole, and the six car photos whole. Check each engine's license and
      trained-data/model license for redistribution, and measure time per region on the emulator and APK growth.
- [x] 1.3 Record the results table and the choice in design.md (replacing the list of candidates with the decision and
      its numbers). Verify: the chosen engine meets the bar in design.md; if none does, stop here and bring the
      results back as a decision. (No engine met the region bar; the decision brought back chose PP-OCRv6 det tiny
      and the English PP-OCRv5 recognizer on the whole photo, which meets it there: design.md, Decisions.)

## 2. The PP-OCR recognizer

- [x] 2.1 Add ONNX Runtime (Android) and the two model files (`PP-OCRv6_det_tiny.onnx`, `en_PP-OCRv5_rec_mobile.onnx`)
      as Android assets, and list them in `THIRD_PARTY_NOTICES.md`. Measure the release APK's growth; if over 20 MB,
      limit the ABIs (design.md, APK size). Verify: `./gradlew :androidApp:assembleDebug` builds, and the measured
      growth is recorded in design.md.
- [x] 2.2 The common pipeline code (design.md, "The PP-OCR pipeline"): detection input sizing, map to rotated
      rectangles (regions, minimum-area rectangle, box score, unclip), the upright crop, recognition input sizing,
      CTC decoding with the model's character list, and splitting a line into words with boxes. Verify: `commonTest`
      unit tests for each step (a known map gives the expected rectangles; a rotated rectangle's crop; CTC decoding
      of repeats and blanks; word boxes by character position; sizes rounded to 32).
- [x] 2.3 The Android `TextRecognizer` running the two models through ONNX Runtime with that code, and a JVM test in
      `androidHostTest` running the same pipeline with ONNX Runtime's JVM library over every photo in
      `maestro/assets/ocr/`. Verify: the JVM test reads 5034, 5368, 3056, 168.1 and 209.1 from the LCD photos (as in
      the evaluation) and every car photo's text it read then.

## 3. Combining and classification

- [x] 3.1 A combining `TextRecognizer` (ML Kit and PP-OCR on the same decoded photo, lines of both) wired into the app
      graph on Android; iOS unchanged. Verify: a unit test with two fake recognizers (lines of both, in the same
      pixels; one failing leaves the other's).
- [x] 3.2 Candidate detection keeps one candidate per place (overlapping boxes: more digits wins, then the labeled
      one). Verify: unit tests (ML Kit `71140` over PP-OCR `140` at the same place; the same value found twice is one
      candidate; far apart, both stay).
- [x] 3.3 The unit rule (design.md): an unlabeled number without a unit is classified as an odometer reading by
      magnitude, or as a trip when it has a decimal; a unit-less whole number that would be a trip by magnitude stays
      dropped. Verify: unit tests (`3056` with a known odometer of 3000 is an odometer reading; `209.1` is a trip; dial
      numbers `120` and `240` are still dropped), and the existing `ReadingDetectionTest` cases still pass.
- [x] 3.4 Run the whole scan end to end on the emulator over every photo (a temporary instrumented harness, removed
      afterwards) with each vehicle's known odometer just below its reading, and measure time per scan. Verify: the
      five LCD readings are presented with the right kind, every car reading as before, and a scan takes under 1.5 s;
      record the results in design.md.

## 4. Verification

- [x] 4.1 Add an LCD case to the `distance` manifest: scan `odo/20220911_162029.jpg` ("ODO 5034"), accept it, save, and
      see the odometer (the photo uploaded in the manifest's `setup.yaml`). Verify: `maestro/run.sh distance` passes and
      `maestro/check-permissions.sh` still reports no system permission.
- [x] 4.2 Final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all
      --strict`, without Maestro. Verify: both pass.
