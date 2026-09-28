# Tasks

## 1. Evaluation (decides the engine; nothing after it starts until it passes)

- [ ] 1.1 Hand-mark the reading's region on each of the six LCD photos in `maestro/assets/ocr/` and check the boxes in
      with them (a small JSON beside the photos, listing photo, box and the expected reading from design.md, Context);
      document the file in `maestro/assets/README.md`. Verify: every LCD photo has one entry.
- [ ] 1.2 Build each candidate engine from design.md far enough to recognize a region (the decoder of our own as a
      prototype; Tesseract and ONNX Runtime in a temporary instrumented harness, not checked in) and run them over the
      six marked regions, the six LCD photos whole, and the six car photos whole. Check each engine's license and
      trained-data/model license for redistribution, and measure time per region on the emulator and APK growth.
- [ ] 1.3 Record the results table and the choice in design.md (replacing the list of candidates with the decision and
      its numbers). Verify: the chosen engine meets the bar in design.md; if none does, stop here and bring the
      results back as a decision.

## 2. Recognition

- [ ] 2.1 Add the chosen engine (Android only) and implement it as a `TextRecognizer`. Verify: `./gradlew
      :androidApp:assembleDebug` builds and the APK growth is what 1.2 measured.
- [ ] 2.2 Give `TextRecognizer` an optional region: crop with a margin, scale to the working width, recognize, map the
      boxes back to whole-photo pixels. Verify: a unit test of the mapping (a box in a crop lands at the right place in
      the photo, including the margin and the scale), in `commonTest`.
- [ ] 2.3 A combining recognizer that runs ML Kit and the chosen engine on the same decoded photo and returns the union,
      and deduplication in candidate detection (same value, overlapping boxes: one candidate, the labeled box kept).
      Verify: unit tests of the deduplication (duplicate, same value far apart, different values overlapping).
- [ ] 2.4 If the chosen engine runs on the JVM (the decoder of our own does), a test over the real photos in
      `androidHostTest` (like `RealPhotoColorJvmTest`): each marked region from 1.1 gives its expected reading.
      Otherwise record in design.md that this check is the evaluation run of 1.2 and is repeated by hand when the
      engine changes.

## 3. Marking a region

- [ ] 3.1 Processor: intents to start marking, update the box, confirm and cancel; confirming recognizes only the box and
      replaces the candidates; an empty result shows "No reading found in the marked area" with mark again, choose
      another photo or leave; the confirmed box is kept in the state and survives recreation. Verify: processor unit
      tests for confirm (candidates replaced), cancel (state unchanged), empty result, and a saved-state round trip.
- [ ] 3.2 Review screen: the "Mark the reading" action on the review and "no reading found" states, the drag-a-box mode
      (outline and scrim from the color scheme, no hard-coded color), "Use this area", back cancels; screen-to-photo
      conversion shared with the candidate boxes. Verify: a unit test of the screen-to-photo conversion, and the
      Maestro case in 4.2.

## 4. Storage and verification

- [ ] 4.1 Add the nullable `markedRegion` to the serialized detection result and set it when the accepted candidate came
      from a marked box. Verify: unit tests that a result with a box round-trips, one without reads back null, and a
      result serialized before this change (no field) still reads.
- [ ] 4.2 Add a Maestro case to the `distance` manifest: scan an LCD photo, mark its reading with a drag, accept the
      candidate, confirm the form's way and value, save and see the entry in the log. Upload the photo in the
      manifest's `setup.yaml`. Verify: `maestro/run.sh distance` passes.
- [ ] 4.3 Final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all
      --strict`, without Maestro. Verify: both pass.
