# Tasks

## 1. OCR dependency and abstraction

- [x] 1.1 Add the ML Kit Text Recognition (on-device, Latin script) dependency to `gradle/libs.versions.toml` and
      `shared`'s Android source set only (Android-only per design.md).
- [x] 1.2 Define a commonMain `expect`/interface for text recognition — e.g. `TextRecognizer.recognize(bytes:
      ByteArray): List<RecognizedTextBlock>`, each block carrying its text and its bounding box in image pixel
      coordinates — matching the project's platform-boundary convention (`ImageCodec` is the model to follow).
      Implement the `actual` in androidMain using ML Kit's on-device recognizer.
- [x] 1.3 Verify against the real test photos (`maestro/assets/ocr/`): run the Android implementation on the
      emulator (a temporary instrumented harness, not checked in) against every photo, and record what it reads in
      design.md ("Measured with ML Kit"). Car clusters: the label and the number come back as separate lines. LCD
      photos: nothing is read, which is `add-seven-segment-ocr`'s to solve.

## 2. Candidate detection and classification

- [x] 2.1 Write the pure candidate-detection function: from a list of recognized text blocks, produce every
      plausible numeric candidate (design.md, "A candidate is a number on its own"), each keeping its own bounding box.
- [x] 2.2 Write the pure classification function: for each candidate, check nearby text for an ODO/TRIP/"T" label
      first, then fall back to magnitude against a supplied known-odometer value (design.md's thresholds). Returns
      an odometer-like/trip-like/not-a-candidate result per candidate.
- [x] 2.3 Unit-test detection and classification against the shapes the real photos show (hand-transcribed
      recognized-text fixtures, design.md "Measured with ML Kit"): "ODO" above `71140km`; `OD0` read as ODO;
      `16865` above "Total distance"; "TRIP"/"T" with a small number; an unlabeled `32478 km` near a known odometer;
      an unlabeled small value with a unit read as a trip; the range `917 km` under "Trip Average" not labeled as a
      trip by that line; unit-less dial numbers (`120`, `RPMx 1000`), clocks (`7:36`) and letter-prefixed ranges
      (`P890 km`) dropped; an unlabeled value with a unit that is neither near the known odometer nor plausible as a
      trip dropped; no known odometer.
- [x] 2.4 Run detection end-to-end (recognizer at twice the size + candidate detection + classification) against
      every photo in `maestro/assets/ocr/` on the emulator, with each vehicle's known odometer just below its
      reading, and record the result per photo in design.md. Verify: every car photo gives its reading as a candidate
      with the right classification, and no LCD photo gives a wrong candidate. Tune the constants in the classifier
      if not. Remove the temporary harness afterwards.

## 3. Storage

- [x] 3.1 Add the `event_capture` table (`event_id`, `photo_id`, `detections`) as a new `.sqm` migration in
      `shared/src/commonMain/sqldelight/.../VehicleEvent.sq` (or a new `.sq` file for it), with insert/select
      queries. Read `docs/test-fixtures.md` first: this migration makes every checked-in fixture stale.
- [x] 3.2 Define `CaptureStore` (commonMain interface and `FileCaptureStore` implementation on kotlinx-io, as `FilePictureStore` is) mirroring
      `VehiclePictureStore`'s pending/promote/discard/sweep shape (design.md): `putPending` (photo bytes + detection
      result) returns a pending id; `promote(pendingId, eventId)` moves it under the saved event and writes the
      `event_capture` row; `discardPending`/`sweep` clean up an abandoned or stale pending capture.
- [x] 3.3 Encode the photo once, full-size, without cropping (reuse `ImageCodec`'s decode/encode primitives, not its
      square-crop path). Serialize the detection result (every candidate's box, recognized text, classification) in
      a form `event_capture.detections` can hold and a later reader can parse back into boxes.

## 4. The review screen

- [x] 4.1 Build the candidate-review screen: the chosen photo shown full-size, a box drawn over every candidate with
      its classification text ("ODO"/"TRIP") next to it, `colorScheme.outline`/`onSurfaceVariant` for the neutral
      state and the domain's "Road Trip Emerald" accent for the selected one (design.md — no hardcoded colors).
      Tapping a candidate selects it (deselecting any other); a confirm action accepts the selection; back
      navigation with nothing selected, or with a selection not yet confirmed, leaves the log event form unchanged.
- [x] 4.2 Handle the "no candidates found" state: an explicit message, with a way to choose another photo or leave.

## 5. Wiring into the log event form

- [x] 5.1 Add the "Scan a reading" action to `LogEventScreen.kt`, next to the odometer/trip field, opening the
      existing system photo chooser (`PhotoResult`) the same way vehicle pictures do; not shown where the platform has
      no recognizer (iOS, `TextRecognizer.isAvailable`).
- [x] 5.2 Add the new intents/state to `LogEventContract.kt`/`LogEventProcessor.kt`: chosen photo → run detection →
      show the review screen → on accept, set the active field's value and switch the way
      (`LogEventIntent.WayChanged`-equivalent) to match the candidate's classification, and keep the accepted photo
      + detections as a pending capture (task 3.2). On save, promote the pending capture under the new event's id;
      on leaving without saving, discard it (matching how every other unsaved field on this form is already
      discarded).
- [x] 5.3 Unit-test the processor: accepting an odometer-like candidate while on "Trip distance" switches the way
      and sets the field; accepting a trip-like one while on "New odometer" does the reverse; leaving after
      accepting discards the pending capture (no `OcrCaptureStore`/`event_capture` call); saving after accepting
      promotes it.

## 6. Fixtures and verification

- [x] 6.1 Run `./gradlew :shared:generateMaestroFixtures` (the migration in task 3.1 makes every checked-in fixture
      stale) and check in the regenerated `.db` files, per `docs/test-fixtures.md`.
- [x] 6.2 Add a Maestro case (or a new flow) covering the happy path: open the log event form, tap "Scan a reading",
      choose a car photo from `maestro/assets/ocr/` (uploaded in the manifest's `setup.yaml`), accept the odometer candidate, confirm the form switched to "New odometer" with the
      right value, save, and confirm the entry is in the log — without asserting anything about the stored photo or
      detections (`odometer-ocr-capture`'s "Not shown to the user" requirement — there is nothing to assert in the
      UI).
- [x] 6.3 Run `maestro/run.sh distance` and confirm it (including the new case) passes.
- [x] 6.4 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
