# Design (stub)

## Context

`EventRow`/`EventRowContent` (shared by the details screen's recent events and the full log) currently render a
one-line summary of an event and are not tappable. `VehicleRepository` has no "one event by id" read; `observeLog`
and `observeRecentEvents` return lists. `add-event-notes` adds a `note: String?` to `VehicleEvent` that is only
ever shown as a presence icon, never as text, on those rows.

## Decisions (proposed, to be confirmed)

1. **A new nav key**, e.g. `EventDetailsNavKey(graph, vehicleId, eventId)`, following the existing `VehicleLogNavKey`
   pattern, rather than a dialog: unlike the note editor (`add-event-notes`), this is a destination a user plausibly
   returns to directly, and a full row of unbounded detail (once pictures exist, `add-event-pictures`) fits a screen
   better than a dialog.
2. **A repository read for one event.** `observeLog`/`observeRecentEvents` already carry every field a details
   screen needs; the simplest option is finding the event by id in the already-loaded list the calling screen holds,
   passed through nav args, rather than adding a new repository method. *Open question 1 keeps the alternative.*

## Open questions

1. Does the details screen re-observe the event from the repository (a `VehicleRepository.observeEvent(vehicleId,
   eventId)` read, robust to being deep-linked to later) or is it handed the event's already-loaded data at
   navigation time (simpler, but stale if something changed it — moot today, since nothing can, but `add-event-editing`
   would change that)? Bears on whether this change should be ordered before or after `add-event-editing`.
2. Should this ship before `add-event-pictures`, so the details screen's layout is designed once with a picture slot
   already in mind, rather than revisited twice?
3. Is every event kind (including "Initial odometer", which never has a note) worth a details screen, or only the
   kinds that can carry more than the row already shows?

## Risks

- **Sequencing**: this change, `add-event-pictures` and `add-event-editing` all touch the same rows and forms; the
  order they are applied in changes how much each one has to revisit. None is applied yet, so the order is still
  open.
