# Design

## Context

Each of the three forms already has a pure, side-effect-free validity check it runs when Save is tapped:
- Log event form: `validateLogDistance(way, entry, moment, now, known)` (`LogDistanceRules.kt`) returns
  `Invalid`/`Valid`/`Anchor`; its first check is `entry.toDistance() ?: return Invalid(FieldEmpty)`.
- Add/edit vehicle forms: `validateVehicleFields(name, licensePlate)` (`VehicleInput.kt`) returns
  `NameRequired`/`Valid`; the add form separately checks `initialOdometer == null` (the entry's `toDistance()`).

Today each of these runs only inside the processor's `save()` action, in response to a `Save` intent, and only then
sets the field-level error flags (`LogEventState.error`, `AddVehicleState.nameError`/`odometerError`,
`EditVehicleState.nameError`) the screens render.

## Goals / Non-Goals

**Goals:**
- Save is disabled while a required field is empty, on all three forms, matching M3's full-screen dialog guidance.
- Reuse the existing pure validators; no new validation logic or duplicated rules.

**Non-Goals:**
- Not touching *non-emptiness* validation (zero distance, odometer not higher than known, a future moment) — see
  proposal.md for why those stay as on-tap errors.
- Not adding a new "why is Save disabled" hint or tooltip; a disabled button plus the (unchanged) placeholder text
  ("Add a note...", empty odometer field, etc.) is the whole affordance, matching how Material's own disabled
  components communicate elsewhere in this app (`app-shell`'s not-yet-available Home actions, `distance-logging`'s
  single-item Kind selector).

## Decisions

- **Compute an `isValid` (or a form-specific name) `Boolean` on each screen, derived the same way `save()` already
  validates, and use it for `enabled` alongside the existing `isSaving`/`isLoading`/`notFound` checks.** Simplest
  option: a `val` computed inline in the `@Composable` screen function from the same state fields `save()` reads
  (`entry.isEmpty`, `name.isBlank()`), not a new field stored in the MVI `State` itself — it is a pure function of
  state that already exists, so storing it too would just be a second copy to keep in sync. Log event form:
  `!state.activeEntry.isEmpty`; add-vehicle form: `state.name.isNotBlank() && !state.entry.isEmpty`; edit-vehicle
  form: `state.name.isNotBlank()`.
- **Remove `AddVehicleState.nameError`/`odometerError` and `EditVehicleState.nameError`, and the `save()` branches
  that set them.** Their only trigger was the same emptiness Save is now disabled for (`validateVehicleFields`'s
  only failure is `NameRequired`; the add form's odometer check is only ever "is it empty"), so once Save can't be
  tapped in that state, `save()`'s `Invalid`/`NameRequired` branch — and the error UI it fed — can never run. Kept
  in place, they would be dead code asserting a state that can no longer occur; deleting them is simpler than
  leaving a branch nothing can reach.
- **`LogEventState.error` stays, unchanged in shape.** Unlike the vehicle forms, `validateLogDistance` still has
  reachable `Invalid` branches once the field is non-empty (`DistanceNotPositive`, `OdometerNotHigher`, plus the
  separate future-moment check in `save()`), so `error` keeps doing real work; only the `FieldEmpty` branch becomes
  unreachable via a tap (Save can no longer be tapped while the field that would produce it is empty).

## Risks / Trade-offs

- Removing `nameError`/`odometerError` touches existing tests that assert those flags after a `Save` intent with an
  empty field; those tests need rewriting to assert the intent is simply not reachable that way (or, if the test
  harness dispatches `Save` directly without going through the disabled button, to assert `save()` now leaves state
  unchanged rather than setting an error flag it no longer has). Named as its own task below.
- A user who somehow reaches a `Save` dispatch while a required field is empty (a compose test bypassing the
  button's `enabled`, or a future caller) now saves nothing silently instead of showing an error — acceptable, since
  the UI path that intent represents no longer exists, but worth a one-line comment at each `save()` so a future
  reader isn't surprised the early-return has no visible feedback.
