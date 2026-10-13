# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [x] 1.1 `vehicle-picture`: compare each of the 6 MODIFIED descriptions with `main`'s text sentence by sentence,
      the three split requirements against their 8 new ones together, and check the two platform notes are below their
      scenarios word for word. Fix the delta where a SHALL, SHALL NOT, condition or value was lost. Verify: every old
      normative statement is found in the delta; any rewording that could be read differently is listed here.
      - Reads differently: "Each button acts once per tap" and "There are no buttons for moving the photo" became
        SHALLs (made normative, as their scenarios already require); the iOS camera prompt likewise.
- [x] 1.2 `vehicle-fuel-type`, `vehicle-type` and `firebase-auth`: the same, with the code lists checked word for word.
      Verify: as 1.1.
      - Clean: the code lists are word for word; "so a vehicle that has a fuel type always has one" (a restatement)
        was dropped.
- [x] 1.3 `licensing` and `event-details`: the same. Verify: as 1.1.
      - Reads differently: downloaded dependencies "need not be listed, and an entry MAY name one only where
        checked-in material needs it" became "SHALL NOT be listed, except that an entry MAY name one where checked-in
        material needs it": the same rule, stated as a prohibition with its exception. The edit screen's "explicit"
        Save is kept.
- [x] 1.4 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-b --strict`
      and `node scripts/check-requirement-length.mjs` pass; an archive in a scratch copy of `openspec/` keeps every
      scenario, leaves 0 requirements over 500 characters in the six specs and 36 in all.
      - 16 changed, 15 added; every scenario kept; 0 long in the six specs, 36 in all.

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one.

- [x] 2.1 `vehicle-fuel-type`, "Every fuel type's code", and `vehicle-type`, "Every type's code". Verify: the tests
      are named here, existing or new, and pass in `./gradlew :shared:allTests`.
      - Existing: `VehicleFuelTypeTest.everyCodeIsPinnedBecauseItIsWhatIsStored` and `everyTypeRoundTripsThroughItsCode`;
        `VehicleTypeTest.everyCodeIsPinnedBecauseItIsWhatIsStored` and `everyTypeRoundTripsThroughItsCode`.
- [x] 2.2 `vehicle-picture`, "Changing a picture by tapping it" and "A file that is not an image when changing a
      picture". Verify: as 2.1.
      - "A file that is not an image when changing a picture": existing
        `EditVehicleProcessorTest.aPhotoThatCannotBeOpenedKeepsTheSavedPictureAndShowsTheError`.
      - "Changing a picture by tapping it": device check on the `dl34` emulator (no Compose UI tests in the project,
        and `maestro/vehicles/edit.yaml` doesn't touch the picture): on the edit screen of a vehicle with a picture,
        the preview is labelled "Change picture" and tapping it opens the system chooser (Camera, Media).
- [x] 2.3 `vehicle-picture`, "Opening the chooser on Android asks for nothing": a device check (the app asks for no
      permission when the chooser opens). Verify: the check is recorded here. Taking a photo with the camera app does
      ask for nothing but fails while CAMERA is declared and not granted: #29.
      - Device check on the `dl34` emulator with CAMERA revoked: tapping "Add picture" opens the chooser directly, with
        no permission prompt.
- [x] 2.4 `vehicle-picture`, "The crop screen's app bar", "Zoom with the keyboard", "Two turns", "The buttons stay
      clear of the navigation bar" and "The crop survives a process restart". Verify: as 2.1, or the device check named
      here where no automated test can check it.
      - "Two turns": added `CropStateTest.twoQuarterTurnsShowThePhotoUpsideDownWithTheFrameOverTheSameContent`.
      - "The crop survives a process restart": existing `CropStateTest.aSavedFrameRestoresToAnEqualOne`; device check:
        zoomed in, the app sent home, its process killed (`am kill`) and restored from recents shows the same pixels.
      - "Zoom with the keyboard": device check: plus zooms in; minus after it gives the start's exact pixels.
      - "The crop screen's app bar" and "The buttons stay clear of the navigation bar": device check (screenshot): back
        arrow, "Crop the photo", "Use photo" in the dark app bar; black background to the bottom edge, buttons above
        the navigation handle. The scenario first said the photo reaches behind the bar; it doesn't (only the
        background does), so the scenario was corrected to what the old requirement says.
- [x] 2.5 `event-details`, "The photo cap on the edit screen", "A photo added from the refueling row is seen from the
      mileage row" and "A refueling's edit screen". Verify: as 2.1.
      - Added: `EventDetailsProcessorTest.anEventWithFivePhotosTakesNoMoreOnItsEditScreen` and
        `aPhotoAddedFromTheRefuelingRowIsSeenFromItsMileageRow`.
      - "A refueling's edit screen": device check: its edit screen shows "Note", "Photos" and "Save" only. The edit
        state (`EventEditState`) has no fuel fields at all.
- [x] 2.6 `licensing`, "Material under the project's own license" and "A dependency no checked-in material needs":
      check `THIRD_PARTY_NOTICES.md`. Verify: the entries are quoted here.
      - `THIRD_PARTY_NOTICES.md`, PaddleOCR models: "used under the Apache License 2.0 (the text is the project's
        [`LICENSE`](LICENSE))". Coil: no entry (`grep -ci coil` is 0).

## 3. Regression run

- [x] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.
