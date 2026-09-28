# Design

## Context

See proposal.md for why. What `add-live-scanner` leaves in place:

- `LiveScannerContent` and `ScanReviewContent` (in `vehicle/distance/`) take the log event form's `LogEventIntent`s;
  `LogEventContent` shows them while `ScanDraft.scannerOpen` / `ScanDraft.review` is set.
- `ScanEditor` (common) runs the photo flow and the live acceptance on a `ScanDraft`; it classifies a photo with
  `detectReadings(photo, knownOdometer)`. `LiveScanner` does the same per frame.
- The camera permission is asked for by `ScanReadingAction` before `ScannerOpened`.
- `VehicleRepository.addDistanceEntry`/`addOdometerAnchor` take a `PendingCapture` and store it in `event_capture`
  under the new event's id, in the same transaction; `addVehicle` does not take one yet.

The add-vehicle form: `AddVehicleState.entry` is an `OdometerEntry` whose unit is the vehicle's odometer unit
(`UnitSelected` changes it); it already runs a full-screen flow of its own (the picture crop), shown in place of the
form's content.

## Goals / Non-Goals

**Goals:**
- The same scanner, photo flow and permission as the log event form, with one classification rule for a vehicle that
  has no odometer yet.

**Non-Goals:**
- A second copy of the scanner or the review: the existing ones are reused.
- Reading the unit (km or miles) from the dashboard.

## Decisions

### The scanner and the review take callbacks, not the log event form's intents
`LiveScannerContent` and `ScanReviewContent` move to `vehicle/ocr/ui/` and take a small `ScanCallbacks` (close, reading
tapped, photo picked, candidate selected, confirm, cancel, error dismissed) instead of `(LogEventIntent) -> Unit`; each
form maps them to its own intents. `ScanReadingAction` moves with them. Nothing in them changes behavior; the log
event form's tests and its Maestro flow are the check that it did not.

### The initial odometer has its own classification, applied where the scan classifies
`detectReadings` stays as it is for the log event form. A pure `detectInitialOdometer(photo)` classifies for a new
vehicle (vehicles spec, "The current odometer can be scanned when adding a vehicle"): it runs `detectReadings` with no
known odometer (so labels, units and `onePerPlace` apply as today) and then re-classifies each detection: a trip label
drops it; an odometer label keeps it; unlabeled with a unit becomes an odometer reading whatever its size; unlabeled
without a unit becomes one only above `ReadingThresholds.MAX_TRIP` (2000). `ScanEditor` and `LiveScanner` take the
classification as a function (`(RecognizedPhoto) -> List<Detection>`) instead of calling `detectReadings` with a known
odometer themselves, so each form passes its own; the log event form passes `detectReadings` with its known odometer,
as now.

Alternative: offering every non-trip number (rejected: on the test photos that offers the speedometer's `120`–`240`
and `RPM x1000` as odometers).

### The add-vehicle form holds a `ScanDraft` like the log event form
`AddVehicleState` gains `scan: ScanDraft` (persisted, as there) and the transient `isScanning` / `scanPhotoUri`; the
processor gets the recognizer and the capture store and handles the same scan intents through `ScanEditor`. Accepting a
reading sets `entry` to it: the value as whole units, or, with a tenth, the unit switched to the tenths unit of the same
family first (the log event form's `withScannedReading` rule, restricted to one field and no way). Leaving the form
discards the scan with the rest (`ScanEditor.discardAll`), as for the picture. The content shows the scanner, then the
review, in place of the form, the way the crop already does.

### The scan is saved with the initial odometer event
`addVehicle` takes an optional `PendingCapture`, promotes it and inserts its `event_capture` row under the initial
odometer event's id inside the transaction that inserts the vehicle and that event, and deletes the promoted photo if
the transaction fails, exactly as `addDistanceEntry` does. The fake repository records it.

## Risks / Trade-offs

- **[Risk]** A nearly new vehicle's small odometer read with neither label nor unit is not offered → accepted (proposal,
  Assumptions): it is typed as today; the photo review shows why nothing was offered.
- **[Risk]** Moving the scanner composables breaks the log event form's scan → **Mitigation**: its processor tests and
  the `distance` manifest's `scan-reading` flow run unchanged against the moved code.
