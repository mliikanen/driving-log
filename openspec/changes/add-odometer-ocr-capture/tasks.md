# Tasks

## 1. OCR dependency and abstraction

- [ ] 1.1 Add the ML Kit Text Recognition (on-device, Latin script) dependency to `gradle/libs.versions.toml` and
      `shared`'s Android source set only (Android-only per design.md).
- [ ] 1.2 Define a commonMain `expect`/interface for text recognition — e.g. `TextRecognizer.recognize(bytes:
      ByteArray): List<RecognizedTextBlock>`, each block carrying its text and its bounding box in image pixel
      coordinates — matching the project's platform-boundary convention (`ImageCodec` is the model to follow).
      Implement the `actual` in androidMain using ML Kit's on-device recognizer.
- [ ] 1.3 Verify against the real test photos: run the Android implementation directly (a small JVM/Robolectric or
      instrumented check, not a full UI test) against every file in `~/Downloads/odo/` and `~/Downloads/trip/`, and
      confirm each recognizes the expected "ODO"/"TRIP"/"T" label and its adjacent number as separate text elements.

## 2. Candidate detection and classification

- [ ] 2.1 Write the pure candidate-detection function: from a list of recognized text blocks, produce every
      plausible numeric candidate (design.md's three-significant-digit filter), each keeping its own bounding box.
- [ ] 2.2 Write the pure classification function: for each candidate, check nearby text for an ODO/TRIP/"T" label
      first, then fall back to magnitude against a supplied known-odometer value (design.md's thresholds). Returns
      an odometer-like/trip-like/not-a-candidate result per candidate.
- [ ] 2.3 Unit-test classification against the shapes the real photos actually show (hand-transcribed recognized-text
      fixtures modeled on them, not the photos themselves): a labeled "ODO"/large number; a labeled "TRIP"/"T" with
      a small number; an unlabeled number close to a given known odometer; an unlabeled small number; an unlabeled
      mid-sized number that's implausible either way (modeled on the car photo's "917" fuel range and the
      motorcycle's "1621" clock) confirming it is dropped, not guessed at.
- [ ] 2.4 Run detection end-to-end (recognizer + candidate detection + classification together) against every real
      photo in `~/Downloads/odo/` and `~/Downloads/trip/`, logging what was found for each. Tune the digit-count and
      magnitude thresholds from design.md against these real results before moving on — they are starting points,
      not final.

## 3. Storage

- [ ] 3.1 Add the `event_capture` table (`event_id`, `photo_id`, `detections`) as a new `.sqm` migration in
      `shared/src/commonMain/sqldelight/.../VehicleEvent.sq` (or a new `.sq` file for it), with insert/select
      queries. Read `docs/test-fixtures.md` first: this migration makes every checked-in fixture stale.
- [ ] 3.2 Define `OcrCaptureStore` (commonMain interface, androidMain implementation) mirroring
      `VehiclePictureStore`'s pending/promote/discard/sweep shape (design.md): `putPending` (photo bytes + detection
      result) returns a pending id; `promote(pendingId, eventId)` moves it under the saved event and writes the
      `event_capture` row; `discardPending`/`sweep` clean up an abandoned or stale pending capture.
- [ ] 3.3 Encode the photo once, full-size, without cropping (reuse `ImageCodec`'s decode/encode primitives, not its
      square-crop path). Serialize the detection result (every candidate's box, recognized text, classification) in
      a form `event_capture.detections` can hold and a later reader can parse back into boxes.

## 4. The review screen

- [ ] 4.1 Build the candidate-review screen: the chosen photo shown full-size, a box drawn over every candidate with
      its classification text ("ODO"/"TRIP") next to it, `colorScheme.outline`/`onSurfaceVariant` for the neutral
      state and the domain's "Road Trip Emerald" accent for the selected one (design.md — no hardcoded colors).
      Tapping a candidate selects it (deselecting any other); a confirm action accepts the selection; back
      navigation with nothing selected, or with a selection not yet confirmed, leaves the log event form unchanged.
- [ ] 4.2 Handle the "no candidates found" state: an explicit message, with a way to choose another photo or leave.

## 5. Wiring into the log event form

- [ ] 5.1 Add the "Scan a reading" action to `LogEventScreen.kt`, next to the odometer/trip field, opening the
      existing system photo chooser (`PhotoResult`) the same way vehicle pictures do.
- [ ] 5.2 Add the new intents/state to `LogEventContract.kt`/`LogEventProcessor.kt`: chosen photo → run detection →
      show the review screen → on accept, set the active field's value and switch the way
      (`LogEventIntent.WayChanged`-equivalent) to match the candidate's classification, and keep the accepted photo
      + detections as a pending capture (task 3.2). On save, promote the pending capture under the new event's id;
      on leaving without saving, discard it (matching how every other unsaved field on this form is already
      discarded).
- [ ] 5.3 Unit-test the processor: accepting an odometer-like candidate while on "Trip distance" switches the way
      and sets the field; accepting a trip-like one while on "New odometer" does the reverse; leaving after
      accepting discards the pending capture (no `OcrCaptureStore`/`event_capture` call); saving after accepting
      promotes it.

## 6. Fixtures and verification

- [ ] 6.1 Run `./gradlew :shared:generateMaestroFixtures` (the migration in task 3.1 makes every checked-in fixture
      stale) and check in the regenerated `.db` files, per `docs/test-fixtures.md`.
- [ ] 6.2 Add a Maestro case (or a new flow) covering the happy path: open the log event form, tap "Scan a reading",
      choose one of the real test photos pushed to the device (or a small fixed test asset modeled on one, per
      `maestro/assets/README`), accept the odometer candidate, confirm the form switched to "New odometer" with the
      right value, save, and confirm the entry is in the log — without asserting anything about the stored photo or
      detections (`odometer-ocr-capture`'s "Not shown to the user" requirement — there is nothing to assert in the
      UI).
- [ ] 6.3 Run `maestro/run.sh distance` and confirm it (including the new case) passes.
- [ ] 6.4 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
