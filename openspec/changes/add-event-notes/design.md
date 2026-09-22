# Design

## Context

The log event form (`LogEventState`/`LogEventProcessor`/`LogEventScreen`) already persists everything the user has
typed or chosen as part of its `@Serializable` state, so it survives rotation and process death (see
`update-log-distance-to-log-event` and `add-direct-logging`). The vehicle's log (`vehicle_event` table,
`VehicleEvent` sealed interface, `EventRow`/`EventRowContent`) is append-only by design: there is deliberately no
update or delete query, only inserts and reads. The picture crop screen (`PictureField`/`CropScreen`) is the
project's existing precedent for a full-screen editor that is not its own navigation destination: it is a
Compose `Dialog` shown from form state (`PictureEditState.cropSourceId`), with the crop's cancel/confirm expressed as
two callbacks the form's processor reduces on, and its "which photo is being cropped" persisted as part of the
form's serializable state.

See proposal.md for the feature and its scope; see the `distance-logging` and `vehicle-log` spec deltas for the
exact behavior.

## Goals / Non-Goals

**Goals:**
- Reuse the crop screen's "full-screen `Dialog` driven by persisted form state" pattern instead of introducing a new
  navigation destination, since the note editor has the same shape: open from a form, closed by either an "attach"
  or a "discard" action, nothing to restore once the form itself is gone.
- Keep the note entirely inside the log event form's lifecycle until save: nothing is written to the log until the
  whole entry is saved, matching how every other field on the form already behaves.

**Non-Goals:**
- Reading a saved note back, editing or removing it after the event is saved, or attaching a picture to an event —
  all filed as separate follow-up proposals (see proposal.md).
- Notes on the "Initial odometer" event (created by the add-vehicle flow, a different form entirely).

## Decisions

### The note editor is state-driven full-screen content in the same window, not a new `ScreenNavKey` — and, revised during implementation, not a `Dialog` either
Alternative considered: a separate `NoteEditorNavKey` on the back stack, like `LogEventNavKey` itself. Rejected: the
editor has no identity of its own once the log event form is gone (there is nothing to restore it *into*), and
Kide's back-stack restoration exists to survive process death for destinations a user can return to directly, which
the note editor never is. The "attach on back" and "discard" outcomes are two ordinary callbacks the processor
reduces on either way, with no nav-key wiring.

**Revised during `/opsx:apply` (task 4.2):** the original plan, following `PictureField`'s `CropScreen`, was a
Compose `Dialog` — a second Android window. On-device Maestro runs surfaced a real platform/tooling limitation:
once that window's one text field had taken and released IME (software keyboard) focus, this app's accessibility
tree stopped exposing the window's content at all — confirmed independently of Maestro with plain
`adb shell uiautomator dump`, so it is not a test-tool quirk. `CropScreen` never hit this because it has no text
field. The fix: the note editor is now full-screen content that replaces the log event form's own content in the
*same* window while open (an early `return` in `LogEventContent`), with `androidx.compose.ui.backhandler.BackHandler`
(Compose Multiplatform's common back-press API, `org.jetbrains.compose.ui:ui-backhandler`) standing in for the
`Dialog`'s `onDismissRequest`. Every other text field in the app already lives in an ordinary single-window screen
and has never shown this failure mode, which is consistent with the diagnosis. The removal-confirmation `AlertDialog`
(no text field, so no IME) was unaffected and is unchanged.

### The editor's open/closed state and its draft text are persisted, not `@Transient`
Following `PictureEditState.cropSourceId`: `LogEventState` gains `noteDraft: String?` (`null` = editor closed;
non-null, including `""`, = editor open with that text) alongside `pendingNote: String?` (the note attached to the
entry so far). Both are part of the serializable state, so opening the editor, typing, and rotating the device (or
losing the process) keeps the editor open with what was typed — the same guarantee every other field on this form
already gives, and the same guarantee `resilience/rotation.yaml` already checks for the crop screen.

### Blank text attaches as no note
Alternative considered: store an empty string as a "note" distinct from no note. Rejected: nothing downstream (the
row icon, a future "view details") gains from that distinction, and it would require the icon logic and any future
"show the note" screen to treat `""` and `null` differently for no reason. `pendingNote` is set from
`noteDraft.trim()`, taking `null` when that trim is empty. Because SQLite stores this as a nullable column, a stored
row's note is either genuinely absent or genuinely non-empty, matching the spec's "a non-empty note" wording exactly.

### The note is a plain column on `vehicle_event`, not a side table
Alternative considered: a `vehicle_event_note` side table (parallel to how `app_state` is a table of its own).
Rejected: a note has no independent lifecycle yet (no separate created/updated time, no history, one-to-one with the
event that owns it, and — per the spec — immutable once the event is saved), so a nullable `note TEXT` column on
`vehicle_event` is simpler and needs no join for the row icon or the future "view details" change to read. If a note
ever grows its own lifecycle (edited independently, timestamped, versioned), splitting it out is a later, additive
migration.

Migration `7.sqm` (schema version 7 → 8) adds the column with `ALTER TABLE vehicle_event ADD COLUMN note TEXT;`. As
with the `app_state` table added in migration 6 (see `add-direct-logging`'s lessons), the column must also be added
directly to `VehicleEvent.sq`'s `CREATE TABLE`, since SQLDelight derives the *current* schema from the `.sq` files,
not from the migration history. `insertDistanceEntry` gains the column. `insertEvent` also gains it: it is the one
query `addVehicle` (for `INITIAL_ODOMETER`) and `addOdometerAnchor` (for `ODOMETER_ANCHOR`) both already call with a
`type` string telling them apart, so it must accept the parameter for both call sites — `addVehicle`'s call simply
passes `null`, since `VehicleEvent.InitialOdometer` gains no `note` property (the spec excludes it). `selectRecentEvents`
and `selectLog` select the column for every row. `VehicleEvent.DistanceEntry` and `VehicleEvent.OdometerAnchor` gain
`val note: String? = null`.

### The trash-can action and the tap-to-open action are separate tap targets on the same row
The note element is a `Row` (leading text: the prompt or the two-line preview; trailing: the trash-can icon button,
shown only while `pendingNote != null`). Tapping the text opens the editor; tapping the trash-can icon shows the
confirmation `AlertDialog` (Material 3, no new component) without opening the editor. This mirrors how
`PictureField`'s preview (tap to change) and its separate "Remove picture" text button already coexist on that form.

### A new Phosphor icon for "has a note"
`LandingIcons.LogEvent` already uses Phosphor's `note-pencil-fill` for the Home screen's "Log event" action; reusing
it for the row icon would visually suggest the row itself is editable, which it is not (reading/editing a saved note
is explicitly out of scope of this change). A plain `note-fill` glyph is added under `docs/icons/phosphor/`,
following the existing convention (`PhotoIcons`, `LandingIcons`, `VehicleIcons`): a single filled SVG path on a
256×256 view box, credited in `THIRD_PARTY_NOTICES.md` (already covers Phosphor's MIT license as a whole).
`EventRow` shows it as a `leadingContent` icon on `ListItem`, present only when `event.note` (added as a `note:
String?` accessor on the sealed interface, `null` for `InitialOdometer`) is non-null.

### The note element's placement
Placed as the last field in the form's `Column`, after the amount entry (`OdometerField`) and before the Save action
in the top bar — the position already used, across the app's forms, for the least load-bearing, most optional
field. This keeps the primary fields (kind, vehicle, way, moment, unit, amount) uninterrupted above it, matching
"unobtrusive".

## Risks / Trade-offs

- **[Risk]** A very long note is still stored and saved in full even though only two lines are ever shown before the
  follow-up "view details" change ships → **Mitigation**: this is intentional (nothing in the proposal caps note
  length) and costs nothing extra: SQLite has no meaningful practical limit for a short free-text note, and the data
  is not lost, only not yet readable back in full.
- **[Risk]** Forgetting to add the `note` column to `VehicleEvent.sq`'s `CREATE TABLE` (only in the `.sqm` migration)
  would silently break on a fresh install while every migrated install works, as already happened once with
  `app_state` → **Mitigation**: called out explicitly above and as its own task; the existing
  `SqlDelightVehicleRepositoryTest`/`VehicleMigrationJvmTest` pattern (a "fresh database has the same schema as a
  migrated one" test) already exists and will be extended.
- **[Risk]** Discovered during `/opsx:apply`: Material 3's `AlertDialog` (used for the removal confirmation) also
  renders through its own Android window, and its buttons' `testTag`s do not come through as the `resource-id`
  Maestro/UiAutomator matches on, though their visible text does → **Mitigation**: the `note_remove_confirm`/
  `note_remove_cancel` tags are kept in the code (useful once a Compose UI test tier reads `testTag` directly,
  `docs/test-strategy.md`'s "not yet adopted" tier), and the Maestro flow taps "Cancel"/"Remove" by their text
  instead, per the project's own flow-authoring rule ("flows read the screen the way the user does: text, and the
  test tags of the app's own screens").
- **[Risk]** Treating whitespace-only text as "no note" could surprise a user who intentionally typed only spaces →
  **Mitigation**: accepted; there is no plausible use for a whitespace-only note, and it keeps "the element prompts
  to add a note" and "a note exists" mutually exclusive everywhere (the row icon, the element's own prompt).

## Migration Plan

Additive only: the new column is nullable and every existing row reads back with `note = null`. No backfill needed.
No rollback concern beyond the existing "migrations only go forward" rule already documented for `app_state`.
