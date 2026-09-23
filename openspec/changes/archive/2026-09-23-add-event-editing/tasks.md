# Tasks

## 1. Storage

- [x] 1.1 Add an `updateEventNote` SQLDelight query (`UPDATE vehicle_event SET note = ? WHERE id = ?`) and
      `VehicleRepository.updateEventNote(vehicleId, eventId, note: String?)` (SqlDelight and Fake implementations).
      Verify with unit tests: updates the target event's note only, leaves every other event and every other field
      of the target event unchanged, and clears the note when passed `null`.

      Query scoped `WHERE vehicle_id = ? AND id = ?` (not just `id`), matching `updateVehicle`'s convention and
      giving a free "wrong vehicle id" safety check. Tests: changes only the target's note (other event's note and
      the target's other fields untouched), clears with `null`, adds a note where none existed,
      `observeEvent` sees the update live, and a mismatched `vehicleId` touches nothing. All pass.

## 2. Editing UI

- [x] 2.1 Add the "Edit" action to the event details screen (`add-event-details-view`), shown only for "Distance"
      and "Odometer reading" events. Verify with a unit test of the details processor's state (action present/absent
      by event kind).

      Made `NoteEditorContent` (`LogEventScreen.kt`) `internal` (was `private`) and reused it as-is, per design.md.
      "Edit" is a pencil `IconButton` (`edit_event`) in the details screen's top bar, shown when
      `state.event is DistanceEntry || is OdometerAnchor`. Added `anOdometerAnchorEventCarriesItsNote` to close a
      gap (only Distance/InitialOdometer had a state-level test before); all three kinds now covered.
- [x] 2.2 Wire "Edit" to open the existing note editor (`add-event-notes`) in an "edit a saved note" mode: seeded
      with the event's current note, back navigation calls `updateEventNote` with the typed text (blank/whitespace
      as `null`), "Discard" returns without calling it. Verify with unit tests of the details processor covering
      add/change/clear/discard.

      `EventDetailsState.noteDraft` (same shape as `LogEventState.noteDraft`), `EventDetailsIntent.{EditClicked,
      NoteDraftEdited, NoteAttached, NoteDiscarded}`. `NoteAttached` writes via `async` then closes the editor only
      after the write completes, so the details screen never shows a stale note even briefly. Tests: seeded
      empty/non-empty, add/change/clear (asserting the exact `updateNoteCalls` made), and discard (asserting no
      call at all). All pass.

## 3. Verification

- [x] 3.1 Add a case to the `distance` Maestro manifest: log a distance entry, open its details, use "Edit" to add a
      note, confirm it shows; edit again to change it; edit again to clear it and confirm the note icon disappears
      from the event's row. Run `maestro/run.sh distance` and confirm it passes.

      Added to `log-distance.yaml`, reusing the entry with a note from `add-event-details-view`'s case just before
      it. Found and fixed two bugs along the way, both in the reused `NoteEditorContent` (`LogEventScreen.kt`),
      never exercised before with pre-existing text since every prior use only ever opened it empty:
      1. `OutlinedTextField(value: String, ...)`, freshly focused, put the cursor at position 0 (Compose's own
         default), not the end of the seeded text — `eraseText` then had nothing before the cursor to erase, so the
         typed replacement got prepended instead of replacing it.
      2. Tried fixing (1) with a `TextFieldValue`-backed field (`TextFieldValue(text, TextRange(text.length))`,
         `remember`ed once per open). This reproduced a second, worse bug: after any edit, the whole screen's touch
         and back handling froze — Discard, the back arrow, and the hardware back key all stopped doing anything.
         Confirmed independent of the selection value (a default, zero-selection `TextFieldValue` froze it too), and
         via `adb logcat` correlated with `FrameTracker: force finish cuj, time out: IME_INSETS_ANIMATION` errors
         recurring for as long as the screen stayed stuck — a library-level `TextFieldValue`/IME interaction issue
         on this Compose Multiplatform version, not this screen's own state. Fixed by switching to the newer
         `TextFieldState`-based `OutlinedTextField` overload (`rememberTextFieldState`, both available in this
         project's Compose Foundation/Material3 versions), whose own default `initialSelection` already places the
         cursor at the end of the seeded text — no explicit `TextRange` needed, and no freeze.
      Verified: `./gradlew :androidApp:assembleDebug` builds; `maestro/run.sh distance` passes (`log-distance` and
      `log-from-home`, including the pre-existing discard/typing cases that had never touched pre-filled text).
- [x] 3.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Both pass. `openspec validate` shows one pre-existing, unrelated INFO note on `add-event-pictures` (its delta
      depends on this change's new requirement, which doesn't exist in the main spec until this change is archived).
