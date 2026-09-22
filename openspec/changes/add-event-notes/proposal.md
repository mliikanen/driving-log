# Proposal

## Why

A logged distance entry or odometer reading is currently just a number, a date and a unit. Users often want to
attach a short reminder to why an entry exists ("borrowed to Sam", "trip meter reset by mistake", "started tracking
mileage for tax purposes") without that reminder living outside the app. There is nowhere on the log event form or in
the log itself to record that context today.

## What Changes

- The log event form gains an unobtrusive note element, placed below the odometer/distance field and above the Save
  action. Before a note exists it prompts "Add a note..."; once one exists it shows up to two rendered lines of the
  note text, end-ellipsized.
- Tapping the element opens a full-screen note editor (a plain multi-line text field, seeded with whatever note is
  already pending). Back navigation (the toolbar's back arrow, the system back gesture or button) attaches the typed
  text to the event being logged as its pending note and returns to the form. An explicit "Discard" action returns to
  the form instead, dropping whatever was typed or changed in that editing session and leaving the previously pending
  note (if any) untouched.
- Once a note is pending, the note element also shows a trash-can action. Tapping it asks for confirmation
  ("Remove this note?" / Remove / Cancel); confirming clears the pending note and the element reverts to the
  "Add a note..." prompt. This is the quick way to drop a note without opening the full-screen editor.
- Saving the log event form stores the pending note with the new event (a distance entry or an odometer anchor —
  whichever the form is about to save). Leaving the whole form without saving discards the pending note along with
  the rest of the unsaved entry, exactly as it does today for every other field.
- Every event row in the vehicle's recent events (details screen) and full log shows a small icon when its event has
  a non-empty note, and no icon otherwise. The note's text is not shown on the row and cannot be read back once
  saved — that is explicitly left to a follow-up change (see below).
- **Out of scope, explicitly**: reading a saved note back (seeing its full text after the form that created it is
  gone), editing a note after the event is saved, and notes on the "Initial odometer" event created when a vehicle is
  added. Three follow-up proposals are filed instead of folding this in:
  1. Viewing a logged event's full details (including its saved note) after the fact.
  2. Attaching pictures to a logged event.
  3. Editing a previously logged event's info (which would include editing or removing its note after saving).

## Capabilities

### Modified Capabilities
- `distance-logging`: the log event form gains the note element and the full-screen note editor (add, edit before
  saving, and remove-with-confirmation, all before the event is saved); saving the form now also saves the pending
  note with the new event.
- `vehicle-log`: a distance entry or an odometer anchor event can carry a note, stored and shown like the rest of the
  append-only log; the recent events section and the full log show a note icon on any row whose event has one.

## Impact

- **Schema**: `vehicle_event` gains a nullable `note` column (a new migration); `insertDistanceEntry` and
  `insertEvent`/anchor-insert queries and the `selectRecentEvents`/`selectLog` queries carry it.
- **Domain**: `VehicleEvent.DistanceEntry` and `VehicleEvent.OdometerAnchor` gain a `note: String?`;
  `VehicleRepository.addDistanceEntry`/`addOdometerAnchor` gain a `note` parameter.
- **Log event form**: `LogEventState`/`LogEventContract`/`LogEventProcessor`/`LogEventScreen` gain the pending note,
  the full-screen editor's own state (open/closed, its draft text) and the remove confirmation; all persisted so they
  survive rotation and process death like the rest of the form.
- **Log rows**: `EventRowContent`/`EventRow` (shared by the details screen's recent events and the full log screen)
  gain the note icon.
- **Assets**: a new Phosphor icon for "has a note" (distinct from the note-and-pencil glyph already used for the
  "Log event" action), added under `docs/icons/phosphor` per the project's existing icon convention.
- **Vehicle switching**: the Home-screen route's "choosing another vehicle keeps what was typed" behavior
  (`distance-logging`, "The vehicle is chosen with a selector...") is extended to also keep a pending note.
- **Tests**: `LogEventProcessorTest`, the vehicle-log migration/repository tests, `EventRowContent` formatting tests,
  and the `distance` and `resilience` Maestro manifests (a note added, previewed, edited, removed with confirmation,
  shown as an icon in the log, and kept across a rotation while the editor is open).
