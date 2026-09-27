# Design

## Context

`LogDistanceRules.kt`'s `validateLogDistance` already has three outcomes for "New odometer": no known odometer at
all → `Anchor(typed)` (unconditional); a higher count → `Valid(distance, loggedOdometer)`; anything else (`typed.meters
<= known.meters`, via `distanceByOdometer` returning null) → `Invalid(OdometerNotHigher(known))`. This change splits
that last case into two: `typed == known` stays `Invalid`; `typed < known` becomes a new, confirmable path.

`LogEventScreen.kt` already has the exact confirmation-dialog shape this needs: `RemoveNoteDialog`, a plain Material
`AlertDialog` with a `TextButton` confirm and a `TextButton` dismiss, driven by one boolean flag in state
(`noteRemovalPending`) and two intents (`NoteRemoveRequested`/`NoteRemoveConfirmed`/`NoteRemoveCancelled`). This
reuses that shape rather than inventing a new one.

`vehicle-log`'s "Odometer anchor events" requirement already places no constraint on an anchor's value relative to
what it replaces ("A later anchor replaces the running total" only demonstrates a higher one, but the requirement
text itself states no direction) — confirmed by reading it directly. So saving a lower anchor needs no change to
`vehicle-log` or to `VehicleRepository.addOdometerAnchor` at all; only a new caller of the same method.

## Goals / Non-Goals

**Goals:**
- Let a deliberately lower new-odometer count be saved, with an explicit, informative confirmation step, using the
  domain model's existing anchor mechanism.

**Non-Goals:**
- Not changing the equal-count case — still refused outright, unchanged. (Recorded as a deliberate scope boundary in
  proposal.md; a literal reading of the request, not something to silently expand.)
- Not adding a new event type, repository method, or database column — the existing anchor mechanism already does
  everything this needs.

## Decisions

### `validateLogDistance` gains a `confirmed` parameter, not a separate validation path
```kotlin
fun validateLogDistance(way, entry, moment, now, known, lowerOdometerConfirmed: Boolean = false): LogDistanceResult
```
For `NEW_ODOMETER` with a known odometer and `typed.meters < known.meters`: return `Anchor(typed)` if
`lowerOdometerConfirmed`, else a new `LogDistanceResult.NeedsLowerOdometerConfirmation` (carrying nothing extra — the
UI already has both values in state: `activeEntry` and `knownOdometer`). The equal case (`typed.meters ==
known.meters`) still falls through to the existing `distanceByOdometer`-returns-null path, unchanged.

Chosen over a second, separate "confirmed save" function that skips validation: routing the confirmed save back
through the *same* function means the future-moment and empty-field checks (earlier in the same function) still run
on confirm, for free — a real, if unlikely, edge case (time passing, or some other field changing) is covered without
extra code, rather than by an easily-forgotten duplicate check.

### State and intents mirror the note-removal dialog exactly
`LogEventState` gains `lowerOdometerConfirmationPending: Boolean = false` (same shape as `noteRemovalPending`).
`LogEventIntent` gains `LowerOdometerConfirmed` and `LowerOdometerCancelled` (`Save` already plays the role
`NoteRemoveRequested` does — the dialog opens as a *result* of tapping Save, not a separate request intent). The
processor's `save()` sets the flag on `NeedsLowerOdometerConfirmation` instead of setting `error`; confirming calls a
`saveConfirmedLowerOdometer()` that re-invokes `validateLogDistance` with `lowerOdometerConfirmed = true` and saves
the resulting `Anchor` exactly like the existing no-known-odometer path already does (same `saving { ... }` helper,
same `repository.addOdometerAnchor` call, same pending-note handling); cancelling just clears the flag.

### The dialog is a new composable, structurally identical to `RemoveNoteDialog`
A plain `AlertDialog`: title and body naming both values (e.g. "Save 44,000 km? That's lower than the vehicle's last
known odometer, 45,230 km."), a confirm `TextButton` ("Save anyway") and a dismiss `TextButton` ("Cancel"), each
test-tagged the same way `RemoveNoteDialog`'s buttons are. No new visual pattern introduced.

## Risks / Trade-offs

- **[Risk]** A user could confirm a genuine typo instead of a deliberate correction, creating an unwanted anchor →
  **Mitigation**: the dialog names both values so the user sees exactly what they're about to do; `add-event-editing`
  already lets a saved event's note be corrected afterward, though not its odometer value — reconsidering an anchor
  after the fact (edit or remove) is not addressed by this change and would need its own proposal if it becomes a
  real need.
- Updating `VehicleRepository.addOdometerAnchor`'s doc comment (it currently says "where no odometer is known," which
  becomes inaccurate) is a one-line wording fix, not a behavior change — included as a task, not a design decision.
