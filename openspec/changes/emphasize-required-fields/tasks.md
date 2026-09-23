# Tasks

## 1. Shared piece

- [ ] 1.1 Add `RequiredFieldNote()` next to `BackButton`/`CloseButton` in
      `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/ui/Components.kt`: a single
      `Text("* indicates a required field", style = MaterialTheme.typography.bodySmall, color =
      MaterialTheme.colorScheme.onSurfaceVariant)`.

## 2. Add-vehicle form

- [ ] 2.1 In `AddVehicleScreen.kt`, reorder the form's content to `picture, name, license plate, unit, odometer,
      type, color` (currently `picture, name, license plate, type, color, unit, odometer`).
- [ ] 2.2 Change the `Name` field's label to `"Name *"`; change `OdometerField`'s `label` to `"Current odometer *"`.
- [ ] 2.3 Drop the license plate field's `(optional)` suffix from its label (back to plain `"License plate"`).
- [ ] 2.4 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace`.

## 3. Edit-vehicle form

- [ ] 3.1 In `EditVehicleScreen.kt`, change the `Name` field's label to `"Name *"`.
- [ ] 3.2 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace`.

## 4. Log event form

- [ ] 4.1 In `LogEventScreen.kt`, change the distance/odometer field's `label` expression to
      `if (state.way == LogWay.TRIP_DISTANCE) "Trip distance *" else "New odometer *"`.
- [ ] 4.2 Add `RequiredFieldNote()` as the last item in the form's scrolling content, before `ScreenBottomSpace` (or
      immediately after the note field, whichever reads better in place — check both).

## 5. Verification

- [ ] 5.1 Run `maestro/run.sh distance vehicles` and confirm they still pass. Check whether any flow's element
      lookups rely on exact label text (`assertVisible: "Name"`, `"Current odometer"`, etc., not just test tags) that
      the added `*` would break, and update those flows if so.
- [ ] 5.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
