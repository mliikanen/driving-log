# Proposal

## Why

Today, typing a "New odometer" count that isn't higher than the vehicle's currently known odometer is refused
outright — an error names the known odometer, and nothing can be saved until the user types a higher number. That's
right for a typo, but too rigid for a real, if unusual, situation: a replaced instrument cluster reading lower than
the app expects, a vehicle bought used whose true mileage the app hasn't seen yet, or the user simply being sure the
app's own record is wrong. The fix is a warning, not a wall: let the user say "yes, save it anyway" instead of
blocking them.

## What Changes

- **Only a strictly lower count changes behavior.** A count *equal* to the known odometer is unaffected — still
  refused outright with today's error, unchanged. (The request said "less than," not "equal to"; treating them the
  same would be a bigger, unrequested change. If equal readings should also get this treatment, that's a follow-up,
  not assumed here.)
- Typing a new odometer count **lower** than the known odometer and tapping Save shows a confirmation dialog
  (matching the existing "Remove this note?" dialog's own pattern: a Material `AlertDialog`, a cancel action and a
  confirm action) naming both the typed count and the known odometer, instead of an error. Nothing is saved until
  the user confirms. **This confirmation only applies when the entry would become the vehicle's new current
  odometer** — logged for now, or for any moment at or after the vehicle's latest logged event. A backdated entry
  (logged for a moment before the vehicle's latest event) that is lower than the odometer known *at that earlier
  moment* is not a correction to today's figure, so it is saved outright, without a confirmation dialog — the same
  as any other backdated anchor already is, higher or lower; later events, if any, recalculate forward from it
  exactly as they already do today.
- Confirming saves the typed count as an **odometer anchor** — the exact mechanism already used when no odometer is
  known yet at the entry's time (`distance-logging`'s existing "A new odometer count without a known odometer is
  saved as an odometer anchor," and `vehicle-log`'s "Odometer anchor events," which already supports an anchor
  replacing the running total with no constraint that it be higher). No new domain concept, no new repository
  method — the same `addOdometerAnchor` call, from a new trigger.
- Cancelling or dismissing the dialog saves nothing and returns to the form exactly as it was, with the typed count
  still there — the same shape as cancelling the existing note-removal confirmation.

## Capabilities

### Modified Capabilities
- `distance-logging`: a new odometer count lower than the known odometer is confirmed, not refused, when it would
  become the vehicle's current odometer; a backdated one below the odometer known at its own moment is saved outright;
  equal is unaffected either way.

## Impact

- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/distance/LogDistanceRules.kt`: `validateLogDistance`
  gains a case for "lower, not yet confirmed" (distinct from the unchanged "equal, refused" case) that applies only
  when the entry is at or after the vehicle's latest logged event; a lower, backdated entry is saved directly. A new
  `mostRecentKnown` parameter (the vehicle's actual current odometer, independent of the chosen moment) tells
  `known == mostRecentKnown` apart from a backdated insertion. An explicit `confirmed` parameter lets a second call,
  after the user confirms, produce the same `Anchor` result the no-known-odometer path already does.
- `LogEventContract.kt`/`LogEventProcessor.kt`: new state (mirroring `noteRemovalPending`) and two new intents
  (mirroring `NoteRemoveConfirmed`/`NoteRemoveCancelled`) for the dialog's confirm/cancel.
- `LogEventScreen.kt`: a new dialog composable, structurally identical to the existing `RemoveNoteDialog`.
- `VehicleRepository.addOdometerAnchor`'s doc comment ("for a new odometer count logged where no odometer is known")
  becomes slightly inaccurate once this ships from a second trigger; a wording fix, not a signature change.
- No database schema change: an odometer anchor is already a fully general event type; this only changes when the
  log event form chooses to create one.
