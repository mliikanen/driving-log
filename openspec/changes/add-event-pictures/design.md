# Design (stub)

## Context

`vehicle-picture` already establishes the pattern: the system's chooser (Android intent chooser including the camera
app; iOS source sheet), crop to a square, two sizes (small/large) in a size-efficient format, stored as files in
private storage with only an id in the database, loaded asynchronously with Coil. `add-event-notes` is the first
change to attach optional, per-event content to a `vehicle_event` row (a nullable `note` column) and is a close
precedent for "attach X to the event being logged, stored when the event is saved, shown as an indicator on the
row."

## Decisions (proposed, to be confirmed)

1. **Reuse the picture pipeline's storage conventions** (private-storage files, an id-only database column, Coil
   loading) rather than inventing a new one, following `vehicle-picture`'s design.
2. **Not necessarily a square crop.** A vehicle's picture is cropped to a square because it is shown as a small round
   or square avatar everywhere; a fuel pump, dashboard or damage photo is looked at for its content, so keeping its
   original aspect ratio (as `vehicle-picture`'s design also considered before settling on square) may serve this
   feature better. *Open question 2.*

## Open questions

1. One picture per event, like a vehicle's one picture, or several? Proposed: one, to start.
2. Cropped to a square (reusing `CropScreen` as-is) or kept at its original aspect ratio (a new, simpler "confirm
   this photo" step without cropping)?
3. Row indicator: a small thumbnail (more useful, more to build and to keep in sync) or a plain icon like the note's
   (`add-event-notes`), consistent with it but less informative?
4. Does this precede or follow `add-event-details-view` (where the full picture would presumably be viewed) and
   `add-event-editing` (replacing or removing a picture after saving)?
5. Any relation to the planned OCR pipeline (ML Kit, fuel pump and receipt reading) — does this change's storage need
   to anticipate that, or is OCR entirely a later, separate change that reads whatever picture this one already
   stores?

## Risks

- **Storage growth**: a picture per event, unlike one per vehicle, could accumulate quickly; large/small sizing
  mitigates this the way it already does for vehicles, but retention policy is not addressed here.
- **Sequencing**: see `add-event-details-view`'s design — the three follow-ups of `add-event-notes` touch the same
  form and rows, and none is applied yet.
