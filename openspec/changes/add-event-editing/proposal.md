# Proposal (stub)

> **Stub.** Short on purpose: it fixes the scope and lists the open questions, to be settled before `/opsx:apply`. Filed as a follow-up of `add-event-notes`, alongside `add-event-details-view` and `add-event-pictures`. Nothing here is built yet, and this one carries a real architectural question — see design.md.

## Why

Mistakes happen: the wrong figure was typed, the wrong date or time zone was picked, a note or a picture (once
`add-event-pictures` exists) needs correcting. Today a logged event, once saved, cannot be changed or removed by
anything in the app — `vehicle-log`'s "The log is not changed by editing the vehicle" requirement and
`distance-logging`'s "Logging only adds to the log" requirement both say so explicitly, and the storage layer has no
update or delete query for events at all.

## What Changes

- Some way to correct a previously logged event's figure, moment, unit, note and (once it exists) picture, from its
  details screen (`add-event-details-view`).

**This is the one open item that most needs a decision before design, not just before tasks**: whether "editing" means
mutating the stored row in place, or adding a correction on top of it (see design.md). The two read the same to a
user but are very different changes to the append-only log and to `distance-logging`'s and `vehicle-log`'s specs.

Out of scope for the stub: bulk edits, an edit history or audit trail beyond whatever the chosen approach implies,
and deleting an event outright (not requested; "editing... info" is not "removing an event").

## Capabilities

### Modified Capabilities
- `vehicle-log`: whichever approach is chosen changes "The log is not changed by editing the vehicle" (or adds a
  requirement beside it) and, if a correction record is chosen, adds a new kind of event.
- `distance-logging`: "Logging only adds to the log" is either narrowed (it still holds for saving a *new* entry,
  but an edit is a different action) or reused as-is (if a correction is itself just another append).

## Impact

- Whichever approach: a form reusing (or copying) the log event form, opened from `add-event-details-view`'s screen.
- If mutating in place: an update path is added to a repository and a query layer that currently has none, and every
  place that currently assumes "an event's fields never change after it is read" (the current-odometer derivation,
  the tenths-remembered-per-vehicle logic, anything caching a log) needs re-checking.
- If a correction record: a new event kind, and the current-odometer/previous-known-odometer derivation
  (`vehicle-log`) needs to fold corrections in — order matters (a correction of an old entry can change every
  current odometer computed since).
