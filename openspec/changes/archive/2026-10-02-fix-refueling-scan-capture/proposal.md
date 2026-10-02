# Proposal

## Why

`odometer-ocr-capture`'s "An accepted scan's photo and detections are kept only if the entry is saved" requirement
already describes the correct, intended behavior for every log event form entry — it names no exception for a
refueling. The implementation does not follow it: `VehicleRepository.addRefueling` has no `capture` parameter at
all (unlike `addDistanceEntry`/`addOdometerAnchor`, both of which do), and neither of `LogEventProcessor`'s two
refueling-save paths passes `form.scan.accepted` through. Scanning a reading for the refueling form's mileage
section (already a shipped action, `odometer-ocr-capture`) today fills the field correctly but silently drops the
photo and detection result that the spec says must be stored with the saved event — found while designing
`add-fuel-amount-ocr`, which would otherwise inherit the identical gap for its own new scan.

## What Changes

- `VehicleRepository.addRefueling` gains a `capture: PendingCapture?` parameter, matching
  `addDistanceEntry`/`addOdometerAnchor`'s existing shape exactly (promoted and stored in the same transaction as
  the event, deleted on a failed save).
- `SqlDelightVehicleRepository.addRefueling` stores it with `eventCaptures.insertEventCapture`, the same call the
  other two already make.
- `LogEventProcessor.saveRefueling`/`saveConfirmedLowerOdometerRefueling` pass `capture = form.scan.accepted`, the
  same way `saveDistance`/`saveConfirmedLowerOdometerDistance` already do.

No capability's specified behavior changes: `odometer-ocr-capture`'s own requirement already covers this case
correctly as written (its scenarios never scope "the entry" to one kind), so this is a pure implementation fix
bringing the code in line with the existing spec — not a new or changed requirement. `skip_specs: true` is set
accordingly.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
(none — see "What Changes")

## Impact

- `VehicleRepository.kt` / `SqlDelightVehicleRepository.kt`: `addRefueling`'s signature and body.
- `LogEventProcessor.kt`: both refueling-save call sites.
- `FakeVehicleRepository.kt` (`shared/commonTest`): `addRefueling`'s override and `RefuelingCall` gain a `capture`
  field, mirroring `DistanceCall`/`AnchorCall`'s existing ones, so tests can assert on it.
- No database schema change: `event_capture` already stores a capture by `event_id` for any event kind: a
  refueling's own id was always a valid key, it was simply never written for one.
