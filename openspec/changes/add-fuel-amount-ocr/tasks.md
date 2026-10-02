# Tasks

## 1. Evaluation photos (decides the label set; nothing after it starts until real photos exist)

- [ ] 1.1 Gather at least 3 real photos of a fuel pump or receipt display showing the dispensed volume — covering a
      few label styles (e.g. "L", "GAL", and ideally one with a per-unit price line next to the volume, to check
      they're told apart in practice) — and check them into `maestro/assets/ocr/fuel/`, documented in
      `maestro/assets/README.md` alongside the existing `odo/`/`trip/` entries (this is a developer action: these
      are real photos of a real pump or receipt, not something generated). Verify: the photos are checked in and
      listed.
- [ ] 1.2 Run the existing `TextRecognizer` pipeline over each gathered photo (a temporary harness, or the same JVM
      test infrastructure `add-seven-segment-ocr` used) and record, in design.md, exactly what text each photo's
      volume and (if present) price labels are recognized as. Update design.md's `VOLUME_LABELS`/price-exclusion
      list to match what was actually read, correcting the guessed one if needed (design.md, "No magnitude
      fallback for fuel amount — label only"). Verify: design.md reflects the measured labels, not the guessed
      ones, with the evaluation notes kept alongside (mirroring `add-seven-segment-ocr`'s measured-not-guessed
      approach).

## 2. Detection and classification

- [ ] 2.1 Add `ReadingKind.FUEL_AMOUNT` and `detectFuelAmount(photo: RecognizedPhoto): List<Detection>` to
      `ReadingDetection.kt`, using the confirmed label set from task 1.2 (label-adjacency only, no magnitude
      fallback, excluding a price-labeled number outright). Verify: `commonTest` unit tests — a volume-labeled
      number is classified `FUEL_AMOUNT`; an unlabeled number is never a candidate regardless of its magnitude; a
      price-labeled number is never a candidate; two numbers at the same place still collapse to one
      (`onePerPlace`).
- [ ] 2.2 Update `LiveScannerContent.kt`'s and `ScanReviewContent.kt`'s binary `if (kind == ODOMETER) "ODO" else
      "TRIP"` to an exhaustive `when` adding `"FUEL"` for `FUEL_AMOUNT` (the compiler enforces exhaustiveness once
      `ReadingKind` has three entries). Verify: `./gradlew :shared:compileKotlinAndroid` (or the equivalent full
      build) succeeds with no remaining non-exhaustive `when`/`if` on `ReadingKind`.

## 3. Wiring the fuel amount field's own scan action

- [ ] 3.1 Add a `ScanTarget` enum (`MILEAGE`, `FUEL_AMOUNT`) and `LogEventState.scanTarget`; change
      `LogEventIntent.ScannerOpened` to carry a `target: ScanTarget`; wire `LogEventProcessor.scanPicked`/
      `liveScanner()` to classify with `::classify` or `::detectFuelAmount` depending on `state.scanTarget`
      (design.md, "The refueling form's two scan actions share one `scan: ScanDraft`"). Verify:
      `LogEventProcessorTest` — opening the scanner from each action sets `scanTarget` accordingly; a photo picked
      via the fuel-amount action yields only fuel-amount candidates even when the same photo also contains an
      odometer-shaped number, and the mileage action's existing behavior (odometer/trip only) is unchanged.
- [ ] 3.2 Extend `withScannedReading`'s `when (kind)` with a `FUEL_AMOUNT` case: sets
      `fuelAmount = FuelAmountEntry(steps = ...)` using the padding rule (design.md, "A detected value becomes
      `FuelAmountEntry.steps`..."), and leaves `way`/`tripDistance`/`newOdometer` untouched. Verify:
      `LogEventProcessorTest` — accepting a fuel-amount candidate (both the photo-review and the live-tap path)
      fills the fuel amount field and leaves the mileage section exactly as it was; a one-decimal-digit reading is
      padded to hundredths (e.g. a detected "42.3" fills the field as 42.30).
- [ ] 3.3 Add a "Scan a reading" action next to `LogEventScreen.kt`'s `FuelAmountField`, dispatching
      `ScannerOpened(ScanTarget.FUEL_AMOUNT)` (shown only when `state.canScan`, matching the mileage section's
      existing action, which now dispatches `ScannerOpened(ScanTarget.MILEAGE)`). Verify: a focused test (or the
      project's existing Compose/Roborazzi coverage for this screen, if any) confirms both actions are present and
      independently wired, and neither is shown when `canScan` is false.

## 4. Maestro

- [ ] 4.1 Extend `distance/scan-reading.yaml` with a fourth case: on a refueling entry, scan one of the gathered
      fuel-amount photos (task 1.1) via the fuel amount field's own action, accept the candidate, and assert the
      field shows the expected value; the save/leave and permission cases already covered for odometer/trip apply
      unchanged, so this case only needs the happy path. Run `maestro/run.sh distance scan-reading` and confirm it
      passes.

## 5. Verification

- [ ] 5.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass before archiving.
