# Proposal

## Why

Today a logged event's note, once saved, cannot be added, changed or cleared by anything in the app —
`vehicle-log`'s "Distance and odometer-anchor events can carry a note" requirement says so explicitly ("Once an
event is saved, its note SHALL NOT be changed or removed by any action available today"), and the storage layer has
no update query for events at all. `add-event-pictures` (filed alongside this change) wants exactly this for photos
too: attach or remove one after the event is already saved. This change builds the one piece both need — an edit
action on the event details screen (`add-event-details-view`) — scoped to the note only; `add-event-pictures`
extends the same action to also manage photos once that capability exists.

## What Changes

- The event details screen (`add-event-details-view`) gains an "Edit" action, shown for "Distance" and "Odometer
  reading" events (never for "Initial odometer", which cannot carry a note). It opens the same full-screen note
  editor `add-event-notes` already built, pre-filled with the event's current note (empty if it has none). Back
  navigation from the editor saves the typed text as the event's note immediately (clearing it if left blank, same
  as the compose-time editor's "Attaching blank text clears the note" behavior); "Discard" returns without changing
  anything.
- **The architectural decision this change is built on**: correcting an already-saved event mutates its stored row
  in place (a new, narrowly-scoped `updateEventNote` query), the same way a note is simply overwritten while
  composing an event before it is saved — not by appending a correction record that supersedes the original. No
  audit trail of the previous text is kept. This is the "mutate in place" option the original stub's design.md
  called out as the single gating decision; the developer picked it explicitly, by analogy to how notes already
  work.
- Scoped to the note only: the figure, moment, unit, time zone, vehicle or kind of a logged event remain
  uneditable — none of those participate in this change. Because only the note changes, and the note plays no part
  in the current-odometer or previous-known-odometer derivations, none of that derivation logic needs revisiting:
  the risk the original stub worried about (a correction retroactively changing figures folded over the log) simply
  does not arise for a note-only edit.

## Capabilities

### Modified Capabilities
- `event-details`: adds the "Edit" action and the note editor entry point.
- `vehicle-log`: "The log is not changed by editing the vehicle" gets an explicit carve-out for this one action;
  "Distance and odometer-anchor events can carry a note" drops the "cannot be changed" sentence, since it now can.
- `distance-logging`: "Logging only adds to the log" gets the same explicit carve-out (it still holds for saving a
  new entry; editing an existing one's note is the one, separate exception).

## Impact

- `shared/`: a new `updateEventNote` query and `VehicleRepository.updateEventNote(vehicleId, eventId, note: String?)`
  method; the existing note editor composable/processor logic reused (opened in an "edit an existing note" mode
  instead of "edit the pending note of an event being composed"); the details screen (`add-event-details-view`)
  gains the "Edit" action and wires it to the editor and the new repository method.
- No schema/migration change: `note` is already a nullable column on `vehicle_event`, and an UPDATE against the
  existing `id` primary key needs no new column.
