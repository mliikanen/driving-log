# Tasks

## 1. Close icon

- [x] 1.1 Add `CloseButton(onClose: () -> Unit)` next to `BackButton` in
      `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/ui/Components.kt`: an `IconButton` showing
      `Icons.Filled.Close`, `contentDescription = "Close"`, `Modifier.testTag("back")` (same tag `BackButton` uses,
      per design.md). `BackButton` itself is untouched.
- [x] 1.2 Switch the log event form's `navigationIcon` (`LogEventScreen.kt`) from `BackButton(onBack)` to
      `CloseButton(onBack)`. Verify with a quick manual check (or an existing Compose test that already asserts the
      `back` tag, if one covers this screen) that the action is still wired to the same "leave without saving"
      behavior — only the icon changes.

      The note editor's own `navigationIcon = { BackButton(onAttach) }` (same file, a different `TopAppBar`) is
      untouched, per design.md — its back navigation saves, not discards.
- [x] 1.3 Switch the add-vehicle form's `navigationIcon` (`AddVehicleScreen.kt`) the same way.
- [x] 1.4 Switch the edit-vehicle form's `navigationIcon` (`EditVehicleScreen.kt`) the same way.

## 2. Verification

- [x] 2.1 Run `maestro/run.sh distance vehicles` (the manifests covering the three changed screens) and confirm they
      still pass — the `back` test tag is unchanged, so no flow file should need editing.

      All 5 flows passed (`log-distance`, `log-from-home`, `landing`, `add-and-browse`, `edit`), no flow edits needed.
- [x] 2.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Both pass. `openspec validate` shows the same pre-existing, unrelated `add-event-pictures` failure noted when
      `add-event-editing` was archived; untouched by this change.
