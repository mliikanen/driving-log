# Proposal

## Why

`add-event-notes` lets a note be attached while logging an event, but only shows that a note exists (an icon on the
row) — its text cannot be read again once the form that created it is gone. More generally, a log row today shows
only what fits on one line (its kind, its moment, its figure); there is no way to see everything about one logged
event in one place. This is also the entry point `add-event-editing` and `add-event-pictures` (in flight alongside
this change) both need: an edit action and a photo viewer both have to open from somewhere.

## What Changes

- Tapping any event row (in the vehicle's recent events or its full log) opens a read-only details screen for that
  event: its kind, its full date, time and time zone, its figure, and its note in full when it has one.
- Going back from the details screen returns to whichever list opened it.
- Every event kind gets a details screen, including "Initial odometer" (which never has a note) — one consistent tap
  behavior for every row, even though that screen's content is sparser for that kind.
- The screen re-observes its event live from the repository by id, rather than being handed already-loaded row data
  at navigation time. This is required, not just nicer: `add-event-editing` (proposed alongside this change) lets a
  note be changed after the event is saved, and `add-event-pictures` (also alongside this change) lets photos be
  added or removed after saving too — a screen holding stale, nav-time data would show outdated content after
  either happens and the user returns to (or stays on) the details screen.
- Nothing on this screen is editable yet. `add-event-editing` adds an explicit edit action to it, as its own,
  separately reviewed change.
- Photos are not shown here yet either — `add-event-pictures` extends this screen with thumbnails and a full-size
  viewer once that capability exists. This change's delta covers only kind/moment/figure/note.

## Capabilities

### New Capabilities
- `event-details`: a screen showing everything currently stored about one logged event, re-observed live, opened
  from any event row.

### Modified Capabilities
- `vehicle-log`: rows in the recent events and the full log become tappable, opening the details screen.

## Impact

- `shared/`: a details screen and processor, a new nav key (`EventDetailsNavKey`), and a new repository read for one
  event by id (`VehicleRepository.observeEvent(vehicleId, eventId)`), backed by a new `selectEventById` query —
  `vehicle_event` already has a stable `id TEXT PRIMARY KEY`, so no schema change is needed for this alone.
- `EventRow`/`EventRowContent`: gains a tap handler (kept as `ListItem`s otherwise; no visual change to the row
  itself from this change alone).
- `maestro/`: a case added to the `distance` manifest opening a logged event's details and going back.
