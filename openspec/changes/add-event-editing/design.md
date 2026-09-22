# Design (stub)

## Context

The project context (`openspec/config.yaml`) states a project-wide principle: *"The data model shall be additive,
not accumulative: every trip, odometer reading, refueling and diary entry is stored as its own record, and records
are only added, never merged into running totals."* The storage layer takes this literally: `VehicleEvent.sq`'s
comment says *"The log is append-only: there is deliberately no update or delete query for events"*, and
`distance-logging`'s "Logging only adds to the log" and `vehicle-log`'s "The log is not changed by editing the
vehicle" both codify it as a spec requirement today. `add-event-details-view` gives a place to open an edit from;
this change is what happens when the user acts on it.

## The central open question: mutate in place, or append a correction

**Option A — mutate the stored row.** An `updateEvent(...)` query is added; the edited fields are overwritten in
place. Simple, and matches what a user pictures happening. But it breaks the append-only invariant the storage layer
and two existing spec requirements currently guarantee, and every place that derives something from the log by
folding over it in order (the current odometer, the previous known odometer at a time, the remembered tenths choice)
needs re-auditing: none of them were written expecting an event's fields to change after the fact, only for new ones
to be added after existing ones.

**Option B — append a correction record that supersedes the original.** A new kind of event (or a `supersedes`
reference on a new row of the same kind) is added; the original event stays exactly as saved, and the derivation
logic is taught to use the latest correction of an event instead of its original fields wherever it reads one. This
keeps the additive principle intact and gives a natural audit trail for free, but is a materially bigger change: the
current-odometer and previous-known-odometer folds (`vehicle-log`) need a "latest version of this event" step, and a
correction of an old entry can retroactively change every current-odometer value computed since — including ones
already shown or exported, if exporting exists by the time this is built.

Both read identically to the user in the common case (the figure they see is now what they typed). They differ in
what the rest of the system can assume about the log, and in how much of `vehicle-log`'s derivation logic needs to
change. **This is not a detail to settle while writing tasks — it changes the spec delta above and the shape of the
implementation, so it must be decided before `/opsx:apply`, with the developer, not guessed.**

## Open questions

1. Mutate in place (Option A) or append a correction (Option B)? This is the question that gates everything else
   below.
2. Which fields can be corrected — everything the log event form captures (figure, way, moment, zone, unit, note,
   and eventually a picture), or a narrower set (say, not the vehicle it belongs to, not its kind)?
3. Does correcting a "New odometer" entry re-run the "is this higher than the previous known odometer" check
   (`distance-logging`, "The new odometer must be higher than the previous known odometer") against the log as it
   now stands, and what happens if the correction would fail it?
4. Is there a limit on what can be corrected once other events have been logged after it (say, correcting an old
   odometer anchor whose reading many later distance entries build on)?
5. Should the original figures remain visible anywhere (an audit trail), or does the corrected value simply replace
   what is shown, with no record of the original kept? Option B gives this for free; Option A does not, unless a
   history is added on top of it.
6. Does this reuse the log event form as-is (pre-filled) or need its own, simpler form?

## Risks

- **This is the biggest architectural departure of the three follow-ups.** `add-event-details-view` and
  `add-event-pictures` are additive; this one either breaks or reinterprets an explicit, currently-enforced
  invariant. It should not be started until question 1 is answered, and answering it may be worth a design
  discussion on its own rather than a quick pick during `/opsx:apply`.
