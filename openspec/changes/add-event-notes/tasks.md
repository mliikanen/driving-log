# Tasks

## 1. Storage: the `note` column

- [x] 1.1 Add `migrations/7.sqm` (`ALTER TABLE vehicle_event ADD COLUMN note TEXT;`, schema version 7 → 8) and add the
      same `note TEXT` column to `VehicleEvent.sq`'s `CREATE TABLE`; verify `SqlDelightVehicleRepositoryTest`'s
      schema-version test is bumped to 8 and passes.
- [x] 1.2 Extend `VehicleMigrationJvmTest`: bump every `migrate(it, N, 7)` call to `migrate(it, N, 8)`, add
      `VERSION_7_SCHEMA`/`versionSevenDatabase()`, and add a migration test asserting a version-7 database migrated
      to 8 keeps its existing events (with `note` reading `null`) and gains the column, plus a "fresh database has
      the same schema as a migrated one" test extended to cover `note`.
- [x] 1.3 Add `note` to `insertDistanceEntry` and to `insertEvent` (used by both `addVehicle`'s initial-odometer
      insert and `addOdometerAnchor`), and to the columns selected by `selectRecentEvents` and `selectLog`; verify
      the project builds (`./gradlew :shared:compileKotlinAndroid` or equivalent) with the regenerated SQLDelight
      code.

## 2. Domain and repository

- [x] 2.1 Add `note: String? = null` to `VehicleEvent.DistanceEntry` and `VehicleEvent.OdometerAnchor` (not to
      `InitialOdometer`); add `note` to `VehicleRepository.addDistanceEntry` and `addOdometerAnchor` (default
      `null`, matching existing optional-parameter style on those methods).
- [x] 2.2 Implement the parameter through `SqlDelightVehicleRepository`: pass `note` into `insertDistanceEntry` and
      `insertEvent`, and map the selected column back onto `DistanceEntry`/`OdometerAnchor` in the row mapper
      (`INITIAL_ODOMETER` mapping ignores the column). Verify with a repository test that adds a distance entry and
      an odometer anchor with a note each and reads them back from `observeLog`.
- [x] 2.3 Update `FakeVehicleRepository`'s `addDistanceEntry`/`addOdometerAnchor` to accept and store `note`, so
      `LogEventProcessorTest` can assert on it; verify existing `LogEventProcessorTest` cases still pass unchanged
      (the new parameter defaults to `null`).
- [x] 2.4 Mutation test: temporarily drop the `note` argument from one insert call, confirm the new repository test
      from 2.2 fails, then restore it.

## 3. The log event form: state and processor

- [x] 3.1 Add `pendingNote: String? = null` and `noteDraft: String? = null` to `LogEventState` (both persisted, not
      `@Transient`, following `PictureEditState.cropSourceId`); add `LogEventIntent`s: `NoteEditorOpened`,
      `NoteDraftEdited(text: String)`, `NoteAttached` (back navigation), `NoteDiscarded`, `NoteRemoveRequested`,
      `NoteRemoveConfirmed`, `NoteRemoveCancelled`.
- [x] 3.2 Implement the reducers in `LogEventProcessor`: opening seeds `noteDraft` from `pendingNote ?: ""`;
      `NoteAttached` sets `pendingNote` from `noteDraft!!.trim().ifBlank { null }` and clears `noteDraft`;
      `NoteDiscarded` clears `noteDraft` only, leaving `pendingNote` untouched; the remove flow needs a
      `noteRemovalPending: Boolean` (transient — a confirmation dialog mid-flight is not worth surviving process
      death) that `NoteRemoveConfirmed` uses to clear `pendingNote`. Verify with new `LogEventProcessorTest` cases:
      opening seeds the draft, attaching trims and stores, attaching blank text clears the note, discarding drops
      the draft but keeps the previous note, removing after confirmation clears the note, cancelling removal keeps
      it.
- [x] 3.3 Wire `LogEventIntent.Save` to pass `state.pendingNote` into `addDistanceEntry`/`addOdometerAnchor`; verify
      with a `LogEventProcessorTest` case that a save with a pending note reaches the repository with that note, and
      one that a save with no note reaches it with `null`.
- [x] 3.4 Mutation test: comment out passing `pendingNote` into the save call, confirm the 3.3 test fails, then
      restore it.
- [x] 3.5 Confirm the vehicle-switch reducer (the `"vehicles"`/`"vehicle"` observers in `LogEventProcessor`) does not
      touch `pendingNote`/`noteDraft`; verify with a `LogEventProcessorTest` case (`distance-logging`, "A pending
      note survives a vehicle change") that choosing another vehicle in the selector keeps a pending note.

## 4. The log event form: UI

- [x] 4.1 Add the note element composable (a `Row`: leading text — "Add a note..." or, once `pendingNote != null`,
      that text with `maxLines = 2, overflow = TextOverflow.Ellipsis`; trailing trash-can `IconButton`, shown only
      when `pendingNote != null`) below `OdometerField` in `LogEventContent`, tagged `log_note` (text) and
      `log_note_remove` (trash-can action); tapping the text dispatches `NoteEditorOpened`.
- [x] 4.2 Add the full-screen note editor as a `Dialog` (following `PictureField`'s `CropScreen` dialog), shown when
      `noteDraft != null`: a `Scaffold` with a top bar (back icon dispatching `NoteAttached`, a "Discard" text action
      dispatching `NoteDiscarded`) and a full-size multi-line `OutlinedTextField` bound to `noteDraft`/
      `NoteDraftEdited`, tagged `note_editor_field`; the system back gesture/button also dispatches `NoteAttached`
      (not `NoteDiscarded`), matching the spec.
- [x] 4.3 Add the removal confirmation `AlertDialog` (Material 3), shown when `noteRemovalPending`: "Remove this
      note?", a confirm action dispatching `NoteRemoveConfirmed` and a cancel action (and dismiss) dispatching
      `NoteRemoveCancelled`, tagged `note_remove_confirm`/`note_remove_cancel`.
- [x] 4.4 Verify on-device (Android): add a note, see the two-line ellipsized preview with a long multi-line note,
      edit it, attach via the system back gesture, discard an edit, remove with confirmation and with cancellation —
      in light and dark mode.

## 5. Log rows: the note icon

- [x] 5.1 Add `note: String?` to the `VehicleEvent` sealed interface itself (each implementation returning its own
      field, `InitialOdometer` returning `null`), so `EventRowContent`/`EventRow` can read it without a `when`.
- [x] 5.2 Add the new Phosphor `note-fill` icon under `docs/icons/phosphor/note-fill.svg` and as
      `LandingIcons`-style path data (a small dedicated object or an addition to an existing one — follow whichever
      reads more naturally once the surrounding icons are re-read); verify a test on its path data the way
      `LandingIcons`' icons are tested, if such a test exists, or add one.
- [x] 5.3 Add `hasNote: Boolean` to `EventRowContent` (from `event.note != null`) and show the icon as `EventRow`'s
      `leadingContent` when true, nothing when false; verify with an `EventRowContentTest`-style test (or wherever
      existing formatting is tested) for a `DistanceEntry`/`OdometerAnchor` with and without a note, and for
      `InitialOdometer`.
- [x] 5.4 Mutation test: force `hasNote` to always be `false`, confirm the 5.3 test fails, then restore it.

## 6. Maestro: the `distance` and `resilience` manifests

- [x] 6.1 Extend `maestro/distance/log-distance.yaml` (or add a case to an existing flow in that manifest): add a
      note while logging a distance, see the preview, edit it via the full-screen editor, save, and see the note
      icon on the new row in the details screen's recent events and in the full log.
- [x] 6.2 Add a case removing a pending note with the trash-can action and confirmation before saving, and one
      discarding an in-progress edit, within the same flow or a new one in `maestro/distance/`.
- [x] 6.3 Extend `maestro/resilience/rotation.yaml`'s log event section: open the note editor, type a note, rotate
      the device, and assert the editor is still open with that text (matching the same survives-rotation coverage
      the form's other fields already get in that flow).
- [x] 6.4 Run `maestro/run.sh distance resilience` and confirm it passes. (Ran `distance`'s two flows and
      `resilience`'s `setup`+`rotation` individually while iterating on a real Maestro/accessibility-tooling issue
      found along the way — see design.md — all green; `maestro/run.sh distance resilience` re-run below for the record.)

## 7. Regression

- [x] 7.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
