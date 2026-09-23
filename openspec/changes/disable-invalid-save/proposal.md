# Proposal

## Why

The log event form and the add/edit vehicle forms are Material full-screen dialogs (see `close-icon-for-forms`), and
M3's guidance for that pattern is explicit: "the confirmation action is disabled until all mandatory fields in the
dialog are met." Today all three forms instead let the user tap Save at any time; a missing required field (the
distance/odometer field on the log event form, the name or the initial odometer on the add-vehicle form, the name on
the edit-vehicle form) is only caught *after* the tap, by showing an inline error and refusing to save. That is a
"try and get told no" pattern; M3 asks for a "can't try until it would work" one, which also tells the user, before
they even reach for Save, that something is still missing.

## What Changes

- Log event form: Save SHALL be disabled while the active field (trip distance, or new odometer count) is empty.
- Add-vehicle form: Save SHALL be disabled while the name is blank (after trimming) or the initial odometer field is
  empty.
- Edit-vehicle form: Save SHALL be disabled while the name is blank (after trimming).
- **Unaffected, and deliberately not touched:** every validation that is about a *non-empty* value being wrong
  rather than absent — a trip distance of zero, a new odometer not higher than the previous known one, a moment in
  the future. Those stay exactly as they are today: caught when the user taps Save (now always reachable once the
  required fields are filled), shown as an inline field error. M3's wording is about *mandatory fields being met*,
  not about full business-rule validity, and folding those into "disabled" would mean either running the future-time
  check continuously (recomputing "is this still in the future" every tick while the form sits open) or silently
  re-interpreting what "mandatory" means; neither is what was asked for.
- **BREAKING (spec-level, not data-level):** four existing spec scenarios describe tapping Save with a required field
  empty and seeing an inline error (`distance-logging`'s "Empty field", `vehicles`' "Odometer is required", "Name and
  odometer are both missing", "Name is required", "Name cannot be emptied"). Once Save is disabled for exactly those
  conditions, tapping it in that state is no longer possible, so those scenarios are rewritten to describe the
  disabled action instead of an error shown after a tap. No stored data or navigation is affected.

## Capabilities

### Modified Capabilities
- `distance-logging`: the log event form's Save action is disabled while the active field is empty (replaces the
  tap-then-error path for that one case; the zero-distance and odometer-not-higher checks are unchanged).
- `vehicles`: the add-vehicle form's Save action is disabled while the name or the odometer is missing, and the
  edit-vehicle form's Save action is disabled while the name is blank (both replace their tap-then-error paths for
  those cases).

## Impact

- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/distance/LogEventScreen.kt` /
  `LogEventProcessor.kt` / `LogEventContract.kt`: Save's `enabled` expression gains an emptiness check (reusing the
  existing `entry.isEmpty`/`OdometerEntry` state already on the form, no new validation logic).
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/add/AddVehicleScreen.kt` / `AddVehicleProcessor.kt` /
  `AddVehicleContract.kt`: Save's `enabled` expression gains a name/odometer emptiness check (reusing
  `validateVehicleFields` and the odometer entry, already used at save time). The now-unreachable `nameError` /
  `odometerError` field-level error state (their only failure mode was this same emptiness) is removed as dead code.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/edit/EditVehicleScreen.kt` /
  `EditVehicleProcessor.kt` / `EditVehicleContract.kt`: same, for the name only (no odometer field to edit). The
  now-unreachable `nameError` state is removed as dead code.
- No database, navigation or persisted-state change.
