# Design

## Context

`EventRow`/`EventRowContent` (shared by the details screen's recent events and the full log) currently render a
one-line-plus-supporting-text summary of an event (`shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/ui/EventRow.kt`)
and are not tappable. `VehicleRepository` has no "one event by id" read; `observeLog`/`observeRecentEvents` return
lists. `vehicle_event` already has a stable `id TEXT NOT NULL PRIMARY KEY` (`VehicleEvent.sq`), so a lookup by id
needs no schema change. `add-event-notes` added `note: String?` to `VehicleEvent`, shown today only as a presence
icon, never as text.

This change is filed and reviewed alongside `add-event-editing` and `add-event-pictures`, which both extend it
(edit action, photo thumbnails/viewer) once it exists. See their own proposals for why editing and photos are split
into their own changes rather than folded into this one.

## Goals / Non-Goals

**Goals:**
- A read-only screen showing everything currently stored about one event, for every event kind.
- Live data: re-observed from the repository, not a snapshot from navigation time.

**Non-Goals:**
- Anything editable (`add-event-editing`).
- Photos (`add-event-pictures`); this delta's requirement text intentionally does not mention them.

## Decisions

### A new nav key, not a dialog
`EventDetailsNavKey(graph, vehicleId, eventId)`, following the existing `VehicleLogNavKey` pattern. Unlike the note
editor (`add-event-notes`, a dialog-like full-screen overlay for a value not yet saved), this is a destination for
data that already exists and that the user plausibly returns to directly; a full screen of unbounded detail (once
`add-event-pictures` lands) fits a screen better than a dialog.

### Re-observe from the repository by id, not handed-in row data
`observeLog`/`observeRecentEvents` already carry every field a details screen needs, so the simplest option would be
finding the tapped event in the already-loaded list the calling screen holds and passing it through nav args. That
was flagged as an open question when this change was still a stub, contingent on whether anything could ever change
an event after it was logged. It now can — `add-event-editing` and `add-event-pictures` both do — so a new
`VehicleRepository.observeEvent(vehicleId: String, eventId: String): Flow<VehicleEvent?>`, backed by a new
`selectEventById` SQLDelight query, is added instead. The `Flow<VehicleEvent?>` shape (nullable) also gives a
well-defined "this event no longer exists" case for free, though nothing can remove an event yet.

### Every event kind gets a details screen
Resolves the stub's third open question: consistency (every row behaves the same way when tapped) beats the small
saving of excluding "Initial odometer," whose screen is simply sparser (no note section, and no photo section once
`add-event-pictures` lands, since that capability also excludes it — see `vehicle-log`'s existing "The 'Initial
odometer' event created when a vehicle is added SHALL NOT carry a note").

## Risks / Trade-offs

- **[Risk]** Re-observing by id adds a new repository method and query where reusing loaded data would have added
  none → **Mitigation**: accepted; it is the only option that stays correct once `add-event-editing`/
  `add-event-pictures` exist, and both are proposed in the same sitting as this change, not hypothetically later.
