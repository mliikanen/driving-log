# Design

## Context

`vehicle-picture`'s pipeline (`shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/picture/`) already
provides most of what a picture needs, and almost none of it is actually vehicle-specific in its logic despite living
in a `vehicle.picture` package:
- `ImageCodec` (+ Android/iOS impls) — generic decode/crop/scale/encode on raw bytes; reused as-is.
- `pictureSides`/`downscaleSteps` (`PictureSweep.kt`) — generic pixel-size math; reused as-is.
- `VehiclePictureStore`/`FileVehiclePictureStore` — a store keyed entirely by opaque "picture id"/"pending id"
  strings, with a `root: Path` constructor parameter. Nothing in its logic is vehicle-specific; only its name and
  package are. It already mints a **fresh random UUID on every promote from pending to permanent**
  (`Uuid.random().toString()`, wired in `AppGraph.kt`), so a slot's filename is never reused for different content —
  removing a picture just stops referencing its id; the id itself is retired, not recycled.
- The 256 px / 1024 px caps are top-level `const val`s (`SMALL_SIDE`, `LARGE_SIDE` in `PictureSweep.kt`), not
  parameterized — a different cap needs a new constant, not a config value.
- `CropScreen`/`CropDialog` — generic on `DecodedImage`/`CropRect`; **not reused here**, since this change's photos
  are not cropped (see Decisions).

The developer has confirmed that editing this existing picture-storage code (not just adding beside it) is in scope
for this change, which changes the shape of the design from "build a parallel copy" to "generalize the one piece
that is not already generic (its name) and reuse it."

See proposal.md for the motivation and scope; see `event-pictures`, and the `distance-logging`/`vehicle-log`/
`event-details` deltas, for the exact required behavior.

## Goals / Non-Goals

**Goals:**
- Up to 5 photos per event, stored uncropped (scaled only), with size caps proposed and justified below.
- A filename/id scheme that already anticipates a later cloud upload of both stored versions, without inventing
  anything beyond what the vehicle picture already does.
- No filename collisions, within one event's photos, across events, or with vehicle pictures — by construction, not
  by coincidence (see Decisions).

**Non-Goals:**
- Cropping (`vehicle-picture`'s square crop is not reused; see Decisions).
- OCR of any photo's content — a later, separate change, per the project context.
- Cleanup of unreferenced photo files — filed as `sweep-event-pictures`, a follow-up, per the developer's explicit
  instruction. Until it lands, photo files an abandoned edit or a removal leaves behind are not deleted.

## Decisions

### No crop: scaled to fit a cap, aspect ratio kept
The original stub's open question 2 asked whether to reuse the square crop screen. Rejected: a vehicle's picture is
shown everywhere as a small square avatar, so cropping it serves that use; an event photo (a dashboard, a fuel pump,
a receipt) is looked at *for its content*, and a forced square crop risks cutting off the very reading or line of
text the photo exists to capture. Instead, a chosen photo is decoded (respecting its stored orientation, like
`vehicle-picture` already does), scaled down only if it exceeds this change's caps (never enlarged, never cropped),
and stored as-is. There is no crop screen and no extra confirmation step in the compose-time flow — the chooser
returning a photo is the only interaction before it appears in the form's photo strip.

### Size caps: 2048 px large / 256 px small, both on the longer side
`vehicle-picture`'s 1024 px cap is sized for a small, always-square decorative avatar (a vehicle's own picture),
never something the user zooms into to read. An event photo can be exactly that: a fuel pump's digits, a receipt's
line items, an odometer's readout. A 1024 px long edge is comfortably above any current phone's *logical* resolution
but can look soft once a user pinch-zooms into a detail — which an avatar is never asked to survive. Proposed instead:
- **Large (full-view) cap: 2048 px on the longer side.** This exceeds the long edge of every current phone's usable
  screen in device-independent pixels, giving headroom for a modest zoom into a detail without visible softness,
  while staying well short of a typical modern phone camera's native resolution (often 3000-4000 px+), so a real
  photo is still meaningfully downscaled and kept storage- and upload-cheap. At WebP quality 80 (`vehicle-picture`'s
  own setting, reused), a 2048 px long-edge photo is still comfortably under a megabyte for a typical photo.
- **Thumbnail cap: 256 px on the longer side.** Reuses `vehicle-picture`'s existing `SMALL_SIDE` constant as-is —
  the row indicator is icon-only (no thumbnail there at all; see the row-icon decision below), so the thumbnail's
  only job is the details/edit screens' photo strip, where a small grid tile is all that is needed, exactly the same
  size class `vehicle-picture` already established for its own list-row picture.
- Both caps bound the **longer** side (not both sides, since there is no square crop to make them equal); the
  shorter side scales proportionally, and a photo already smaller than a cap is stored at its own size, never
  enlarged — the same "never enlarge" rule `vehicle-picture` already states.
- The existing `MAX_DECODE_SIDE = 3072` decode-time bound (`PictureSweep.kt`) is reused unchanged as the ceiling the
  raw photo is decoded to before any scaling step, to bound memory regardless of the camera's actual resolution.

### One fresh id per photo; no filename can ever collide
Every attached photo mints its own random UUID (the same `Uuid.random()` pattern `AppGraph.kt` already wires for
vehicle pictures), stored as the primary key of a new `event_picture` row and used to name that photo's files —
never derived from the event's id, its position among the event's photos, or anything else that could repeat. This
is what actually answers "avoid filename conflicts... also with vehicle photos": collision is avoided by every
picture (vehicle or event) drawing from the same effectively-collision-free random id space, the same way two
vehicles' pictures already never collide today, not by any new namespacing scheme. It also directly serves the
cloud-upload goal: once a picture's small/large files are written, their id/filename never changes and is never
reused for different content, so they are already fit to be treated as immutable, cacheable blobs once a sync
backend exists — nothing about this change's storage layout needs revisiting when that happens.

### The picture store is generalized and reused, not duplicated
`VehiclePictureStore`/`FileVehiclePictureStore` (interface and implementation) are renamed to a vehicle-agnostic
`PictureStore`/`FilePictureStore` — their logic was already generic (see Context); only the name and package
implied a vehicle-only scope. Two instances are wired in `AppGraph.kt`: the existing one, kept pointed at its
current root for vehicle pictures, and a new one pointed at a separate root (e.g. `pictures/events/`) for event
pictures. A separate root, rather than sharing one directory, is chosen so a future "delete all pictures of kind X"
or an independent sweep policy per kind never has to filter one shared directory by prefix — organizational, not a
correctness requirement (the id scheme above already makes a shared directory collision-safe too).

### The row shows an icon only, both note and photo together, at the bottom-end corner
"No hero icon for log item rows right now" rules out showing a thumbnail in the row itself. `vehicle-log`'s existing
note-presence icon is today the `ListItem`'s `leadingContent` (`EventRow.kt`); this change moves it, together with
the new photo-presence icon, to a small icon cluster anchored at the row's bottom-end (bottom-trailing) corner —
requiring `EventRow` to move from a plain `ListItem` to one wrapped in a `Box` that overlays the icon cluster on top
of (or below) the `ListItem`'s own content, aligned `Alignment.BottomEnd`. The note icon is drawn first (it shipped
first), the photo icon after it, when both apply; only the icons that apply are shown (an event with a photo but no
note shows only the photo icon, and vice versa).

### Photo editing extends the same "Edit" action `add-event-editing` builds for the note
`add-event-editing` (filed alongside this change) adds an "Edit" action on the details screen that today opens the
note editor directly. This change modifies that action once `event-pictures` exists: "Edit" opens a small edit
screen holding both the note element (tap it to open the same full-screen note editor as before) and a photo strip
(the same control the log event form uses, up to 5, each with its own remove action that asks for confirmation, and
an "Add photo" affordance shown while under 5) — not the full log event form, which also shows non-editable fields
(figure, moment, unit) this capability does not make editable. An explicit "Save" action commits the note text and
the final photo set together; leaving without saving (back navigation, with a confirmation if anything changed)
discards any photos picked during the session, the same way leaving the compose-time form does.

## Risks / Trade-offs

- **[Risk]** Renaming `VehiclePictureStore` touches existing, working, tested code rather than adding beside it →
  **Mitigation**: the developer explicitly authorized this; the class's own logic does not change, only its name,
  package and the number of instances wired in `AppGraph.kt`, so the risk is mechanical (a rename + an extra DI
  wire), not behavioral. Existing vehicle-picture tests continue to exercise the same code under its new name.
- **[Risk]** No cleanup of unreferenced files until `sweep-event-pictures` lands → **Mitigation**: accepted, per the
  developer's explicit choice to scope that separately; `vehicle-picture` itself shipped its own sweep as part of
  its original change, so this is a deliberate, temporary asymmetry, not an oversight.
- **[Risk]** A 2048 px cap is a new, event-picture-specific constant, diverging from `vehicle-picture`'s 1024 px →
  **Mitigation**: justified above by a real difference in how the two kinds of picture are used (avatar vs.
  document-like content); revisit only if it turns out to still be too small for OCR once that change is built.
