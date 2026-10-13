# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [x] 1.1 `distance-logging`: compare each of the 17 MODIFIED descriptions with `main`'s text sentence by sentence,
      the split ones against their new requirements together. Fix the delta where a SHALL, SHALL NOT, condition or
      value was lost. Verify: every old normative statement is found in the delta; any rewording that could be read
      differently is listed here.
      - Reads differently: "later events are recalculated forward" (descriptive) became "Later events SHALL be
        recalculated forward"; the confirmation's "exactly as it was" is kept.
- [x] 1.2 `vehicles`: the same for its 7 MODIFIED descriptions, with the region list and the trip labels checked word
      for word. Verify: as 1.1.
      - Fixed: the odometer field names the two tenths units again (a first draft said "the two tenths units"), and the
        unit choice keeps "the user SHALL be able to change it before saving". The region list and the trip labels are
        word for word.
- [x] 1.3 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-c --strict`
      and `node scripts/check-requirement-length.mjs` pass; an archive in a scratch copy of `openspec/` keeps every
      scenario, leaves 0 requirements over 500 characters in the two specs and 12 in all.
      - 24 changed, 25 added; every scenario kept; 0 long in the two specs, 12 in all.

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one; a device
check is recorded where only the screen can show it.

- [x] 2.1 `distance-logging`, the scan, the anchor and the required-mark scenarios ("A scanned reading replaces the
      typed number", "A confirmed lower count adds no distance", "The mark follows the active field"). Verify: the tests
      are named here, existing or new, and pass in `./gradlew :shared:allTests`.
      - Added `LogEventProcessorTest.aScannedOdometerReadingReplacesTheTypedCount` and `aConfirmedLowerCountAddsNoDistance`.
      - "The mark follows the active field": device check on the `dl34` emulator: "Trip distance *", then "New odometer *"
        once it is chosen (the label is built in the composable).
- [x] 2.2 `distance-logging`, the unit, moment and clock scenarios ("Saving in miles remembers only the tenths",
      "Miles for a whole-kilometer vehicle", "A moment last year", "Just after midnight in 12-hour form"). Verify: as 2.1.
      - Existing: `LogEventProcessorTest.onlyTheTenthsChoiceIsRememberedNotTheUnitFamily`,
        `FormatTimeTest.midnightIsTwelveAmAndNoonIsTwelvePm` ("12:15 AM").
      - Added: `EventRowContentTest.tenMilesForAWholeKilometerVehicleIsShownInKilometers`,
        `LogEventProcessorTest.aMomentLastYearIsAccepted`.
- [x] 2.3 `distance-logging`, the anchor form, vehicle and memory scenarios ("The explanation goes away at a known
      time", "A starting point of zero", "The date and time survive a vehicle change", "An update does not fill the
      memory from the log"). Verify: as 2.1.
      - Added: `LogEventProcessorTest.theStartingPointExplanationGoesAwayAtAKnownTime`, `aStartingPointOfZeroIsSavedAsAnAnchor`,
        `theDateAndTimeSurviveAVehicleChange`.
      - Existing: `VehicleMigrationJvmTest.migratingFromVersionSixKeepsTheVehicleItsColorAndItsEventAndAddsAnEmptyAppStateTable`
        (no remembered vehicle after the update) and `LogEventProcessorTest.withNothingRememberedTheChooserStartsOnTheFirstVehicleByName`.
- [x] 2.4 `distance-logging`, the layout, note and photo scenarios ("The row survives a rotation", "Tapping with a note
      pending", "A photo is attached uncropped"). Verify: as 2.1, or the device check recorded here.
      - Device checks: the Kind and vehicle selectors are side by side, equal width and height, Kind on the left, in
        portrait ([42,338][529,506] and [550,338][1038,506]) and landscape; tapping the note element with a note pending
        opens the editor with that note, and the form shows no keyboard; a 3:4 camera photo added through "Add photo"
        shows no crop screen and is stored 1392 x 1856 (and 192 x 256), not square. The scenario first said a landscape
        photo; the emulator's camera takes portrait ones, so it now says what was checked.
- [x] 2.5 `vehicles`, the add and edit form scenarios ("Color is the last field", "A name of only spaces keeps Save
      disabled", "Closing the add screen discards what was typed", "Typing a name again enables Save"). Verify: as 2.4.
      - Device checks (Save's enabled state is computed in the composables): spaces as the name with an odometer typed
        leaves Save disabled; the add form opened again after its close "X" has an empty name; on the edit screen Save
        is disabled with the name cleared and enabled once "Van" is typed. Existing: `AddVehicleProcessorTest.leavingWithoutSavingAddsNothing`,
        `aWhitespaceOnlyNameIsRefusedAndNothingIsSaved`.
      - "Color is the last field": device check: the color choice is the last field. The scenario first said "directly
        after the type choice"; the fuel type choice is between them, and the spec's field order leaves it out (added to
        #34).
- [x] 2.6 `vehicles`, the odometer field scenarios ("A thousands separator appears while typing", "Clear after a typed
      zero", "Two zeros and a digit in a whole-number unit", "Pasting more than seven digits", "The log event form's
      odometer field"). Verify: as 2.1.
      - Added: `OdometerEntryTest.aThousandsSeparatorAppearsWhileTyping`, `clearAfterATypedZeroEmptiesTheField`,
        `twoZerosAndADigitInAWholeNumberUnit`, `pastingMoreThanSevenDigitsKeepsTheFirstSeven`;
        `LogEventProcessorTest.theLogEventFormsOdometerFieldKeepsAFirstZero`.
- [x] 2.7 `vehicles`, the add-vehicle scan scenarios ("An unlabeled number just above 2000", "A tenth in miles").
      Verify: as 2.1.
      - Added: `InitialOdometerDetectionTest.anUnlabeledUnitlessReadingJustAbove2000IsOffered`. Existing:
        `AddVehicleProcessorTest.aReadingWithATenthSwitchesMilesToTenthsAndNeverToKilometers`.

## 3. Regression run

- [x] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.
