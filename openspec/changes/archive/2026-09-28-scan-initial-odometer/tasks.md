# Tasks

## 1. Reuse the scanner

- [x] 1.1 Move `LiveScannerContent`, `ScanReviewContent` and `ScanReadingAction` to `vehicle/ocr/ui/`, taking
      `ScanCallbacks` instead of `LogEventIntent`s; the log event form maps them to its intents. Verify: the
      `LogEventProcessorTest` suite passes unchanged and `./gradlew :androidApp:assembleDebug` builds.
- [x] 1.2 `ScanEditor` and `LiveScanner` take the classification as a function; the log event form passes
      `detectReadings` with its known odometer. Verify: the existing scan tests in `LogEventProcessorTest` and
      `LiveFramesJvmTest` pass unchanged.

## 2. The initial odometer's classification

- [x] 2.1 `detectInitialOdometer(photo)` (design.md): trip-labeled dropped, odometer-labeled kept, unlabeled with a
      unit kept at any size, unlabeled without a unit kept only above 2000. Verify: unit tests for each rule, with the
      shapes of the real photos (`ODO` + `71140km`, `TRIP` + `168.1`, `3056` alone, `120 km`, dial numbers `120`/`240`,
      `RPMx 1000`, the clock `1621`).

## 3. The add-vehicle form

- [x] 3.1 `AddVehicleState.scan` and the scan intents in `AddVehicleProcessor` through `ScanEditor` with
      `detectInitialOdometer`; accepting sets `entry` (tenths switching the unit within its family); leaving discards
      the scan. Verify: processor unit tests: a tapped live reading and a confirmed photo candidate fill the odometer
      and leave name, plate, type, color and picture as they were; a tenth switches kilometers to kilometers with 100 m
      and miles to miles with tenths; closing the scanner changes nothing; leaving the form discards the pending scan.
- [x] 3.2 The add-vehicle screen: "Scan a reading" under the odometer field (hidden where the platform has no
      recognizer), the scanner and review shown in place of the form. The edit-vehicle screen gets no action. Verify:
      `./gradlew :androidApp:assembleDebug` builds; the Maestro case in 4.2.

## 4. Storage and verification

- [x] 4.1 `VehicleRepository.addVehicle` takes an optional `PendingCapture` and stores it with the initial odometer event
      in the same transaction (the fake repository records it). Verify: repository unit tests: saved with the initial
      event, none when typed, no photo left behind when the save fails.
- [x] 4.2 Add a case to the `distance` manifest's `scan-reading` flow: add a vehicle, "Scan a reading" on the add form,
      the scanner's photo button, the car cluster photo, accept "ODO 71140", save, and see 71,140 km on its details.
      Verify: `maestro/run.sh distance` passes.
- [x] 4.3 Final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all
      --strict`, without Maestro. Verify: both pass.
