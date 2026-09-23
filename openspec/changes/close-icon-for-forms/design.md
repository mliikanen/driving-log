# Design

## Context

`BackButton` (`shared/src/commonMain/kotlin/com/mikonoma/drivinglog/ui/Components.kt`) is a single shared composable
— an `IconButton` showing `Icons.AutoMirrored.Filled.ArrowBack`, test-tagged `back` — used as the top app bar's
`navigationIcon` on every screen that has one: the vehicle list, vehicle details, event details, the full log, the
crop screen, and the three forms this change covers (log event, add vehicle, edit vehicle). All of them currently
look identical regardless of what tapping them actually does.

M3's full-screen dialog guidance (`m3.material.io/components/app-bars/guidelines` component set) distinguishes two
button-in-top-left situations: ordinary hierarchical navigation uses a back arrow ("go to where I came from"); a
full-screen dialog — a screen that suspends the rest of the app until it is completed or dismissed, with nothing
"back" to return to inside the flow — uses a close "X" ("leave without keeping this"). The log event form and the
add/edit vehicle forms fit the dialog case: each is a single-purpose, modal form whose only ways out are Save or
leaving without saving (already the discard behavior everywhere it's ever been tested), matching the "X" exactly. The
note editor is the same shape of screen but is excluded (see proposal.md): its back navigation *saves*, which the
close icon does not represent.

## Goals / Non-Goals

**Goals:**
- Give the three forms the close "X" dismiss icon M3 specifies for full-screen dialogs.
- Leave every other screen's back arrow, and every existing behavior (what tapping the icon actually does), unchanged.

**Non-Goals:**
- Not changing the note editor's icon (its back navigation saves, not discards — a different requirement, out of
  scope here; the proposal names it explicitly as excluded).
- Not touching Save's enabled/disabled state or field validation (a separate change).
- Not changing the crop screen, whose own cancel/confirm affordances are a full-screen editor, not a form, and
  weren't part of the original request.

## Decisions

- **New `CloseButton` composable, not a parameter on `BackButton`.** `BackButton(onBack: () -> Unit)` stays exactly
  as it is (icon, tag, behavior) so no existing caller or test is at risk. `CloseButton(onClose: () -> Unit)` is a
  new, separate composable in the same file: an `IconButton` showing `Icons.Filled.Close`, `contentDescription =
  "Close"`, same `Modifier.testTag("back")` as `BackButton` — the tag names the action ("the way to leave this
  screen"), which every Maestro flow already taps by that id, not the icon glyph, so no flow needs to change. A
  shared parameter (`BackButton(icon = ...)`) was considered and rejected: it would let a future caller pick either
  icon for either meaning by accident, which is exactly the ambiguity this change removes.
- **Only the three named forms switch.** `LogEventScreen.kt`, `AddVehicleScreen.kt` and `EditVehicleScreen.kt` change
  their `navigationIcon = { BackButton(onBack) }` to `navigationIcon = { CloseButton(onBack) }` (the callback itself
  is unchanged — same "leave without saving" lambda already wired). Every other `BackButton(...)` call site is left
  untouched.
- **`contentDescription = "Close"`.** Matches the icon's actual action for screen readers, distinct from "Back" on
  every other screen.

## Risks / Trade-offs

- Two composables (`BackButton`, `CloseButton`) that are nearly identical is minor duplication; kept them separate
  anyway because it makes each call site self-documenting (which icon a screen gets is a one-word decision at the
  call site) and keeps this change from touching `BackButton`'s existing callers at all.
- Purely visual, and low risk: no Roborazzi baselines exist yet for these screens (the project has no Roborazzi
  tests wired up currently, despite `docs/test-strategy.md` naming it as a future option), so there is nothing
  automated to update — only the manual Maestro/visual check in tasks below.
