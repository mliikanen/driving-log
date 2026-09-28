# Proposal

## Why

Adding a vehicle asks for its current odometer, typed by hand, while the log event form can already read one from the
dashboard with the live scanner (`add-live-scanner`) or a photo. The first reading of a vehicle is the one every later
distance is measured from, so a mistyped digit there is the costliest one; it is also the moment the user is most
likely standing next to the vehicle.

**This depends on `add-live-scanner`**: the live scanner, the photo flow from it and the camera permission are
introduced there. This change is applied and archived after it.

## What Changes

- **"Scan a reading" on the add-vehicle form**, under the odometer field, opens the same live scanner (and, from its
  photo button, the same photo flow) as on the log event form.
- **Only odometer readings are offered**: a new vehicle has no known odometer to compare with, and a trip reading is
  never an initial odometer. So a number labeled "ODO", or unlabeled with a distance unit, is offered as the odometer
  at any size; one labeled as a trip is never offered; an unlabeled one without a unit only above 2000 (the dial's scale
  and clocks are smaller). Tapping one (live) or confirming one (photo) fills the odometer field and closes the scanner.
- **A reading with a tenth** switches the unit to the one with tenths in the same family (kilometers or miles), as the
  log event form does; the family itself is never changed by a scan.
- **What is kept**: the photo or camera frame and its detections, stored with the vehicle's initial odometer event
  when the vehicle is saved, and discarded if the form is left, as for a scanned log entry.

## Assumptions Recorded for Review

- Readings labeled "TRIP" (or "T") are not offered at all. A small odometer (a nearly new vehicle, under 2000) is offered
  when it is labeled or has a unit next to it; read with neither, it is left out with the dial numbers, and is typed.
- The edit-vehicle form gets no scan action: its odometer is not editable.
- The scan is stored with the initial odometer event, like a logged entry's, so a misdetected first reading can be
  reviewed the same way.

## Out of Scope

- Reading the odometer unit (km or miles) from the dashboard: the unit stays the user's choice, as today.
- iOS: the scan action is hidden there, as on the log event form.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicles`: the add-vehicle form's odometer can be scanned (an added requirement).
- `odometer-ocr-capture`: a scan accepted on the add-vehicle form is kept with the vehicle's initial odometer (an added
  requirement).

## Impact

- The scanner's composables (`LiveScannerContent`, `ScanReviewContent`) and `ScanDraft`/`ScanEditor` are used by a
  second form: they are generalized from the log event form's intents to callbacks.
- `AddVehicleContract`/`AddVehicleProcessor`/`AddVehicleScreen`: the scan state and intents, and an "odometer only"
  classification.
- `VehicleRepository.addVehicle` takes the accepted scan and stores it with the initial odometer event (the
  `event_capture` table, no migration).
- The `distance` manifest's `scan-reading` flow (which already uploads the scan photos) gains a case adding a vehicle
  with a scanned odometer, through the scanner's photo button.
