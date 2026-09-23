# Proposal

## Why

The add-vehicle form's odometer reading is required (the vehicle cannot be saved without it), but sits last on the
form, after two fields — type and color — that always have a default and can never be left empty. A user filling the
form top to bottom meets three optional-feeling stops (picture, type, color) before the one field that actually
blocks saving. The log event form's distance/odometer field has the same problem in miniature: it is the form's one
required field, but nothing marks it as such.

Material Design 3's own guidance for this is specific: a required field gets an asterisk next to its label, plus a
short note explaining the convention (typically at the bottom of the form) — not color alone, which fails for
colorblind users and isn't conveyed to screen readers. This app today does the opposite of "mark the required
ones": the add/edit vehicle forms' one optional field (license plate) is the one carrying a "(optional)" suffix,
leaving every required field unmarked. That reads fine when there is only one optional field to call out, but it
gives a user no positive signal — nothing on screen actually says "you must fill this in" until they try to save and
hit an error.

## What Changes

- **Add-vehicle form**: reorder fields to `Picture, Name, License plate, Unit, Odometer, Type, Color` — the unit and
  odometer (paired, since the reading only means something once its unit is known) move up directly after the
  identity fields, ahead of type and color (both optional-feeling: always have a default, can never be emptied).
- **Required-field marking, adopted as a convention** (M3's own pattern, not color-only): every required field's
  label gets a trailing `*`, and each form gains a short line — "* indicates a required field" — near wherever its
  actions are, explaining it once per form. Applied to:
  - Add-vehicle form: `Name`, `Current odometer` (the license plate's existing "(optional)" suffix is dropped —
    redundant once required fields carry their own mark, and having both conventions on the same form at once would
    read as two different systems).
  - Edit-vehicle form: `Name` (its only required field), for consistency with the add form — an unlabeled-required,
    `(optional)`-marked pair otherwise splits the same field's convention across add and edit.
  - Log event form: the distance/odometer field's own label ("Trip distance" / "New odometer", whichever is active)
    — no reorder here; the field already sits ahead of everything but the note, right where it belongs, once the
    kind/vehicle/way/unit context it depends on is chosen.
- **Not marked**: type and color (add-vehicle) always have a default and can never be emptied, so they are not
  "required" in the sense this convention marks — the same reasoning the current `(optional)` label already applies
  in reverse.
- Folded into one change (rather than two) since both are the same convention applied to the two places in the app
  a required numeric reading exists — reviewing and applying it once is more coherent than twice.

## Capabilities

### Modified Capabilities
- `vehicles`: the add-vehicle form's field order, and required-field marking on the add and edit vehicle forms.
- `distance-logging`: required-field marking on the log event form's distance/odometer field.

## Impact

- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/add/AddVehicleScreen.kt`: field order; `Name` and
  `Current odometer` labels gain `*`; the license plate's `(optional)` suffix is dropped; a required-field note is
  added.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/edit/EditVehicleScreen.kt`: `Name` label gains `*`; a
  required-field note is added.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/distance/LogEventScreen.kt`: the distance/odometer
  field's label gains `*`; a required-field note is added.
- Visual and layout only: no data, validation or navigation change (this pairs with, and assumes, `disable-invalid-save`'s
  disabled-Save behavior already in place — the note explains *why* Save may be disabled, it does not change when it is).
