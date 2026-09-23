# Proposal

## Why

The log event form, and the add/edit vehicle forms, are full-screen, modal, single-purpose forms: opening one
suspends the rest of the app until the user saves or leaves it, exactly what Material Design 3 calls a full-screen
dialog. M3's guidance for that pattern is specific: the dismiss action in the top-left is a close "X", not a back
arrow, because a full-screen dialog can only be completed or dismissed — there is nothing "back" to go to within the
dialog itself. Today these three forms use the same arrow-in-a-circle `BackButton` as ordinary hierarchical
navigation (the vehicle list, vehicle details, event details), which blurs a real distinction the icon should make:
an arrow says "return to where you were," an X says "leave without keeping this." No behavior changes — leaving any
of the three forms this way already discards unsaved input — only the icon that communicates it.

## What Changes

- Add a `CloseButton` composable (a close "X" icon button), alongside the existing `BackButton` (arrow), in
  `com.mikonoma.drivinglog.ui.Components.kt`.
- Swap the log event form, the add-vehicle form and the edit-vehicle form from `BackButton` to `CloseButton` for
  their top app bar's dismiss action. No other screen changes: `BackButton` (arrow) stays exactly as it is for actual
  hierarchical navigation (vehicle list, vehicle details, event details, the full log, the crop screen).
- The note editor (`distance-logging`'s "The full-screen note editor") is deliberately left out: its own back
  navigation *saves* the typed text rather than discarding it (see its requirement), which is not what M3's "X"
  represents (a discard action) — changing its icon without changing that behavior would misrepresent it.

## Capabilities

### Modified Capabilities
- `distance-logging`: the log event form's dismiss action is a close "X", not a back arrow.
- `vehicles`: the add-vehicle and edit-vehicle forms' dismiss action is a close "X", not a back arrow.

## Impact

- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/ui/Components.kt`: new `CloseButton` composable.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/distance/LogEventScreen.kt`,
  `vehicle/add/AddVehicleScreen.kt`, `vehicle/edit/EditVehicleScreen.kt`: use `CloseButton` instead of `BackButton`.
- No Maestro flow changes: `CloseButton` keeps the existing `back` test tag (Maestro flows tap the dismiss action by
  that id already; the tag names the action, not the icon).
- Visual only: no data, navigation destination or persistence change.
