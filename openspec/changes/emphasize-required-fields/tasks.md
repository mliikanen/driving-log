# Tasks

## 1. Shared piece

- [x] 1.1 Add `RequiredFieldNote()` next to `BackButton`/`CloseButton` in
      `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/ui/Components.kt`: a single
      `Text("* indicates a required field", style = MaterialTheme.typography.bodySmall, color =
      MaterialTheme.colorScheme.onSurfaceVariant)`.

## 2. Add-vehicle form

- [x] 2.1 In `AddVehicleScreen.kt`, reorder the form's content to `picture, name, license plate, unit, odometer,
      type, color` (currently `picture, name, license plate, type, color, unit, odometer`).
- [x] 2.2 Change the `Name` field's label to `"Name *"`; change `OdometerField`'s `label` to `"Current odometer *"`.
- [x] 2.3 Drop the license plate field's `(optional)` suffix from its label (back to plain `"License plate"`).
- [x] 2.4 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace`.

## 3. Edit-vehicle form

- [x] 3.1 In `EditVehicleScreen.kt`, change the `Name` field's label to `"Name *"`.

      Also dropped the license plate field's `(optional)` suffix (back to plain `"License plate"`), matching the add
      form — the spec only named this for the add form, but the same "two competing conventions" rationale in
      design.md applies to edit's identical field; leaving it only on one form would read as inconsistency, not a
      deliberate choice. Updated the `vehicles` spec delta's "Edit a vehicle" requirement and its "required field is
      marked" scenario to match.
- [x] 3.2 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace`.

## 4. Log event form

- [x] 4.1 In `LogEventScreen.kt`, change the distance/odometer field's `label` expression to
      `if (state.way == LogWay.TRIP_DISTANCE) "Trip distance *" else "New odometer *"`.
- [x] 4.2 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace` (or
      immediately after the note field, whichever reads better in place — check both).

      Placed right after `NoteField`, the last item in the form's content.

## 5. Verification

- [x] 5.1 Run `maestro/run.sh distance vehicles` and confirm they still pass. Check whether any flow's element
      lookups rely on exact label text (`assertVisible: "Name"`, `"Current odometer"`, etc., not just test tags) that
      the added `*` would break, and update those flows if so.

      No flow asserted exact label text (checked by search before running). All 5 flows passed (`log-distance`,
      `log-from-home`, `landing`, `add-and-browse`, `edit`), no flow edits needed.
- [x] 5.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Both pass. `openspec validate` shows the same pre-existing, unrelated `add-event-pictures` failure noted
      earlier; untouched by this change.
