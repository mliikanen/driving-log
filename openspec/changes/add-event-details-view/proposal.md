# Proposal (stub)

> **Stub.** Short on purpose: it fixes the scope and lists the open questions, to be settled before `/opsx:apply`. Filed as a follow-up of `add-event-notes`, which explicitly leaves reading a saved note back to this change. Nothing here is built yet.

## Why

`add-event-notes` lets a note be attached while logging an event, but only shows that a note exists (an icon on the
row) — its text cannot be read again once the form that created it is gone. More generally, a log row today shows
only what fits on one line (its kind, its moment, its figure); there is no way to see everything about one logged
event in one place.

## What Changes

- Tapping an event row (in the vehicle's recent events or its full log) opens a read-only details screen for that
  event: its kind, its full date, time and time zone, its figure, and — once `add-event-notes` ships — its full,
  untruncated note.
- Going back from the details screen returns to whichever list opened it.
- Nothing on this screen is editable; correcting a previously logged event is `add-event-editing`, filed separately.

Out of scope for the stub: editing anything shown here (`add-event-editing`), showing a picture attached to an event
(`add-event-pictures`, itself also a stub), and any list-level change (search, filtering) beyond making a row open
its details.

## Capabilities

### New Capabilities
- `event-details`: a read-only screen showing everything stored about one logged event.

### Modified Capabilities
- `vehicle-log`: rows in the recent events and the full log become tappable, opening the details screen.

## Impact

- `shared/`: a details screen and processor, a new nav key, and a repository read for one event by id.
- `EventRow`: gains a tap handler (kept a `ListItem` otherwise; no visual change to the row itself).
- `maestro/`: likely a case added to the `distance` manifest rather than a new manifest.
