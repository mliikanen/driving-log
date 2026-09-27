# Proposal

## Why

A note (`add-event-notes`) covers text, but some context is easier to capture as a photo: a dashboard shot for an
odometer count, a fuel pump or receipt for a refueling once that exists, proof of damage. The project context
already plans for pictures at the fuel-pump/receipt stage (ML Kit OCR); this is the plain "attach photos to what you
just logged" version, with reading them back (`add-event-details-view`, filed alongside this change) and managing
them after saving (`add-event-editing`, also filed alongside this change) both included from the start.

## What Changes

- The log event form gains a photo element, below the note element: up to 5 photos, added through the system's own
  chooser of image sources (the same mechanism as the vehicle's own picture), each stored **without a square crop** —
  unlike a vehicle's one picture, an event photo (a dashboard, a fuel pump, a receipt) is looked at for its content,
  and cropping it to a square risks cutting off exactly the part the user meant to capture. A photo is only scaled
  down when it exceeds this change's size caps (proposed below), never enlarged and never cropped.
- Saving the event stores whatever photos are attached (0 to 5); leaving the form without saving discards them, the
  same way an unsaved picture is discarded today for a vehicle.
- A logged event's photos can be added or removed later too, from its details screen's "Edit" action
  (`add-event-editing`), the same one now used for the note — up to the same 5-photo cap.
- The event details screen (`add-event-details-view`) shows a photo's attached photos as thumbnails, each opening a
  full-size viewer.
- Event lists (recent events, full log) show only a small icon indicating "this event has one or more photos" — no
  thumbnail and no "hero" image in the row itself, matching the note's existing presence-only icon. Both the note
  icon and the new photo icon move to (and are shown together at) the lower-end (bottom-trailing) corner of the row,
  replacing the note icon's current placement as the row's leading content.
- Storage is designed with a later cloud upload of both stored versions in mind (see design.md): every photo gets
  its own freshly-minted, globally unique id that is never reused for different content, exactly the scheme the
  vehicle's own picture already uses — nothing new needs inventing here, but it is confirmed explicitly because this
  change is the first time *several* pictures share one owning record.
- Cleanup of photo files that end up unreferenced (an abandoned edit session's picks, a removed photo's old files) is
  **out of scope for this change** — filed as its own follow-up, `sweep-event-pictures`, mirroring how the vehicle
  picture store's own sweep was built. Until it lands, such files simply accumulate.

## Capabilities

### New Capabilities
- `event-pictures`: up to 5 photos attached to a logged event, their storage (uncropped, scaled, size-capped), and
  how they are added or removed while composing an event.

### Modified Capabilities
- `distance-logging`: the log event form gains the photo element; saving stores the attached photos with the new
  event.
- `vehicle-log`: a "Distance" or "Odometer reading" event's row shows a photo-presence icon alongside the note icon,
  both moved to the row's lower-end corner (was: the note icon alone, as leading content).
- `event-details`: the details screen shows photo thumbnails and a full-size viewer; the "Edit" action
  (`add-event-editing`) is extended to also manage photos, not only the note.

## Impact

- `shared/`: a new `event_picture` table (id, event id, position, created-at) and migration; a repository read for a
  saved event's photos and writes to attach/detach one; the image pipeline (`ImageCodec`, decode/scale/encode)
  reused as-is; the picture *storage* class (currently `VehiclePictureStore`/`FileVehiclePictureStore`, despite
  having no vehicle-specific logic — see design.md) generalized and reused for a second, event-scoped instance,
  since the developer has confirmed touching vehicle picture storage code is in scope for this change.
- New size caps distinct from the vehicle picture's 256/1024 square caps, proposed and justified in design.md.
- The log event form, the details screen, and the "Edit" screen (`add-event-editing`) all gain a photo-strip UI
  element; `EventRow`'s layout changes from a `ListItem` with `leadingContent` to one with both indicator icons
  anchored at the row's bottom-end corner.
- `maestro/`: cases added to the `distance` manifest (attach/remove while composing, the row icon) and to whatever
  manifest covers the details/edit screens once `add-event-details-view`/`add-event-editing` land.
