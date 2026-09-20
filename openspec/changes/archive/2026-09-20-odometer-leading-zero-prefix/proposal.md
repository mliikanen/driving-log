# Proposal

## Why

The odometer field is already required and starts empty: no digit means nothing to save, zero has to be typed on
purpose, and backspace on a lone zero empties the field again (archived change `add-vehicles`). One gap remains in how
a typed zero behaves. The field forgets a leading zero as soon as more digits follow it: typing 0 then 5 shows 0.5, but
backspace jumps straight to empty instead of back to the explicit zero the user typed. Backspace should undo exactly
what was typed, one digit at a time, so the zero the user chose to enter is not silently lost.

## What Changes

- The first digit typed, when it is a 0, is kept as a prefix digit when more digits follow. It is never drawn as an extra
  digit (0 then 5 then 3 reads 0.0, 0.5, 5.3 in a tenths mode and 0, 5, 53 in a whole-number mode), but backspace removes
  it only after the digits typed after it, so 5.3 goes back to 0.5, then to the typed zero 0.0, then to empty.
- In a tenths mode the first typed 0 fills the tenth (0.0). When more digits follow, the readings keep their leading
  zero before the decimal separator (0.5, not .5), as tenths readings always do.
- Further zeros typed while the entry is only the typed zero are ignored, so there is never more than one prefix zero.
- The prefix zero does not count towards the 7-digit limit.
- A pasted "0123" behaves like the digits typed one by one: it reads 12.3 and backspace goes 1.2, 0.1, 0.0, then empty.
- This applies to every place an odometer reading is entered, not only the add-vehicle form. Today that form is the only
  one and it already uses the shared odometer field and entry model, so the behavior is implemented once in those shared
  pieces and the spec now says it holds for every odometer field. A convention in the project context makes new odometer
  inputs (refueling, trips, camera confirmation) use the same pieces. Whether an empty field can be saved stays a
  decision of each screen.

Already in place from `add-vehicles`, and not changed here: the odometer is required when adding a vehicle, the field
renders empty until the user types, a typed 0 is an explicit reading, backspace on the lone zero empties the field, and
saving is blocked while it is empty.

Out of scope: editing the odometer after the vehicle exists, changing how readings are shown elsewhere, and any change
to the digit limit or the units.

## Capabilities

### New Capabilities
<!-- None. -->

### Modified Capabilities
- `vehicles`: the requirement for entering the odometer in the microwave-style number field gains the leading zero
  rules and their scenarios.

## Impact

- Code: the shared odometer entry model in `shared/src/commonMain/.../vehicle/input/OdometerEntry.kt` and its persisted form
  state; the odometer field composable needs no new behavior (it draws the number, and the prefix zero is not drawn).
- Tests: entry model and add-screen processor tests, and the odometer Maestro flow.
- Project context: `openspec/config.yaml` gains the rule that every odometer input uses the shared field and entry model.
- No data, schema or dependency changes: the stored reading is the same whole meters as before.
