# Tasks

**Apply order**: this change depends on `add-event-details-view` and `add-event-editing` being applied (and
ideally archived) first — its `event-details` delta modifies the "Edit" action they add, and its details-screen
thumbnails extend the details screen they add. Do not start section 4 before both exist.

## 1. Storage

- [x] 1.1 Rename `VehiclePictureStore`/`FileVehiclePictureStore` to a vehicle-agnostic `PictureStore`/
      `FilePictureStore` (no logic change — see design.md). Verify the existing vehicle-picture test suite still
      passes unchanged under the new name.
- [x] 1.2 Add a new `event_picture` table (id, event id, position, created-at) and its migration, plus queries to
      insert a photo, select a saved event's photos in order, and delete one by id. Add repository methods to read
      an event's photos and to attach/detach one. Verify with unit and migration tests.
- [x] 1.3 Add the new size-cap constants (reuse `SMALL_SIDE = 256`; add a new `EVENT_PHOTO_LARGE_SIDE = 2048`) and an
      aspect-ratio-preserving scale function (bounds the longer side, keeps the ratio, never enlarges — no square
      crop). Verify with unit tests covering a large photo being downscaled, a small photo not being enlarged, and
      orientation being respected.
- [x] 1.4 Wire a second `FilePictureStore` instance in `AppGraph.kt`, pointed at a separate root for event pictures,
      alongside the existing vehicle-pictures instance. Verify with a unit test that an event picture's id and a
      vehicle picture's id are drawn from independent random id generation (never derived from each other or from
      the owning record's id).

## 2. The log event form's photo element

- [x] 2.1 Add the photo strip element (thumbnails + "Add photo") below the note element on the log event form, for
      "Distance"/"Odometer reading" entries only, reusing the system chooser with no crop step, capped at 5. Verify
      with unit tests of the processor: attach, cap enforcement (no "Add photo" at 5, it reappears after a removal).
- [x] 2.2 Add per-photo removal with a confirmation dialog. Verify with unit tests: confirm removes, cancel/dismiss
      keeps it.
- [x] 2.3 Wire save/discard: saving promotes attached photos to permanent ids and `event_picture` rows with the new
      event, in one atomic write; leaving without saving discards the pending photo files. Verify with unit tests
      (saved event holds the photos; a failed or abandoned save leaves no orphaned reference) and that attached
      photos survive a rotation.

## 3. The row icon

- [x] 3.1 Redesign `EventRow` so the note and photo presence icons are anchored together at the row's bottom-end
      corner (replacing the note icon's current `leadingContent` placement), in the order note-then-photo, showing
      only the icons that apply. Verify with a Compose test or the existing `EventRowContentTest` tier (whichever
      `docs/test-strategy.md` calls for) covering all four combinations (neither, note only, photos only, both).

## 4. Details and Edit screens

*(Do not start until `add-event-details-view` and `add-event-editing` are applied — see the apply-order note above.)*

- [ ] 4.1 Add the photo thumbnails and full-size viewer to the event details screen. Verify with unit tests of the
      details processor's state (thumbnails present/absent, in attach order) and a Compose test that tapping one
      opens the viewer.
- [ ] 4.2 Extend the "Edit" screen `add-event-editing` adds with the photo strip (seeded with the event's current
      photos, add/remove up to 5) and an explicit "Save" action committing the note and the photo set together.
      Verify with unit tests: add-after-save, remove-after-save, both together, and leaving without saving changes
      nothing.

## 5. Verification

- [ ] 5.1 Add cases to the `distance` Maestro manifest: attach and remove photos while composing an event; save and
      confirm the row shows the photo icon (and, with a note also attached, both icons together at the row's
      corner). Run `maestro/run.sh distance` and confirm it passes.
- [ ] 5.2 Add a case (to the `distance` manifest, or wherever `add-event-details-view`/`add-event-editing`'s own
      Maestro cases live) opening a photo's details thumbnail, viewing it full-size, and using "Edit" to add/remove
      a photo on an already-saved event. Run that manifest and confirm it passes.
- [ ] 5.3 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
