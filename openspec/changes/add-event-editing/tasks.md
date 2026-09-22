# Tasks

## 1. Storage

- [ ] 1.1 Add an `updateEventNote` SQLDelight query (`UPDATE vehicle_event SET note = ? WHERE id = ?`) and
      `VehicleRepository.updateEventNote(vehicleId, eventId, note: String?)` (SqlDelight and Fake implementations).
      Verify with unit tests: updates the target event's note only, leaves every other event and every other field
      of the target event unchanged, and clears the note when passed `null`.

## 2. Editing UI

- [ ] 2.1 Add the "Edit" action to the event details screen (`add-event-details-view`), shown only for "Distance"
      and "Odometer reading" events. Verify with a unit test of the details processor's state (action present/absent
      by event kind).
- [ ] 2.2 Wire "Edit" to open the existing note editor (`add-event-notes`) in an "edit a saved note" mode: seeded
      with the event's current note, back navigation calls `updateEventNote` with the typed text (blank/whitespace
      as `null`), "Discard" returns without calling it. Verify with unit tests of the details processor covering
      add/change/clear/discard.

## 3. Verification

- [ ] 3.1 Add a case to the `distance` Maestro manifest: log a distance entry, open its details, use "Edit" to add a
      note, confirm it shows; edit again to change it; edit again to clear it and confirm the note icon disappears
      from the event's row. Run `maestro/run.sh distance` and confirm it passes.
- [ ] 3.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
