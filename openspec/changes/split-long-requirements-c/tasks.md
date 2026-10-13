# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [ ] 1.1 `distance-logging`: compare each of the 17 MODIFIED descriptions with `main`'s text sentence by sentence,
      the split ones against their new requirements together. Fix the delta where a SHALL, SHALL NOT, condition or
      value was lost. Verify: every old normative statement is found in the delta; any rewording that could be read
      differently is listed here.
- [ ] 1.2 `vehicles`: the same for its 7 MODIFIED descriptions, with the region list and the trip labels checked word
      for word. Verify: as 1.1.
- [ ] 1.3 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-c --strict`
      and `node scripts/check-requirement-length.mjs` pass; an archive in a scratch copy of `openspec/` keeps every
      scenario, leaves 0 requirements over 500 characters in the two specs and 12 in all.

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one; a device
check is recorded where only the screen can show it.

- [ ] 2.1 `distance-logging`, the scan, the anchor and the required-mark scenarios ("A scanned reading replaces the
      typed number", "A confirmed lower count adds no distance", "The mark follows the active field"). Verify: the tests
      are named here, existing or new, and pass in `./gradlew :shared:allTests`.
- [ ] 2.2 `distance-logging`, the unit, moment and clock scenarios ("Saving in miles remembers only the tenths",
      "Miles for a whole-kilometer vehicle", "A moment last year", "Just after midnight in 12-hour form"). Verify: as 2.1.
- [ ] 2.3 `distance-logging`, the anchor form, vehicle and memory scenarios ("The explanation goes away at a known
      time", "A starting point of zero", "The date and time survive a vehicle change", "An update does not fill the
      memory from the log"). Verify: as 2.1.
- [ ] 2.4 `distance-logging`, the layout, note and photo scenarios ("The row survives a rotation", "Tapping with a note
      pending", "A landscape photo is attached whole"). Verify: as 2.1, or the device check recorded here.
- [ ] 2.5 `vehicles`, the add and edit form scenarios ("Color is the last field", "A name of only spaces keeps Save
      disabled", "Closing the add screen discards what was typed", "Typing a name again enables Save"). Verify: as 2.4.
- [ ] 2.6 `vehicles`, the odometer field scenarios ("A thousands separator appears while typing", "Clear after a typed
      zero", "Two zeros and a digit in a whole-number unit", "Pasting more than seven digits", "The log event form's
      odometer field"). Verify: as 2.1.
- [ ] 2.7 `vehicles`, the add-vehicle scan scenarios ("An unlabeled number just above 2000", "A tenth in miles").
      Verify: as 2.1.

## 3. Regression run

- [ ] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.
