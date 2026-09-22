# Tasks

## 1. Restyle the note element

- [x] 1.1 In `LogEventScreen.kt`, replace `NoteField`'s `Row`/`Text`/`IconButton` implementation with
      `OutlinedTextFieldDefaults.DecorationBox`: `value = pendingNote ?: ""`, `label = { Text("Note") }`,
      `placeholder = { Text("Add a note...") }`, `innerTextField = { Text(pendingNote.orEmpty(), maxLines = 2,
      overflow = TextOverflow.Ellipsis) }`, `trailingIcon` holding the existing trash-can `IconButton` (shown only
      when `pendingNote != null`, tagged `log_note_remove`, unchanged behavior), `enabled = true`, `singleLine =
      false`, a throwaway `remember { MutableInteractionSource() }`, `VisualTransformation.None`. Wrap the whole
      `DecorationBox` in `Modifier.clickable(onClickLabel = "Add a note", role = Role.Button, onClick = onOpen)`,
      keep the `log_note` test tag on the same outer element it is on today.

      Revised during implementation (see design.md): verified against the pinned material3 1.9.0 sources that
      `DecorationBox`'s `placeholder` slot only shows while the field has real keyboard focus, which this field
      never gets, so `placeholder` is not used — the prompt and the real note text both render through
      `innerTextField`, and `value` is that same non-empty text (kept non-empty purely to hold the "Note" label
      minimized/floated). `DecorationBox` also has no `modifier` parameter, so the tap target, width and `log_note`
      tag are on a wrapping `Box` instead. Same visible result, corrected wiring.
- [x] 1.2 Verify it compiles and the existing `log_note`/`log_note_remove` test tags resolve to the same composable
      roles as before (a clickable "button-like" element and, once a note is pending, a nested icon button) —
      `./gradlew :androidApp:assembleDebug`.

## 2. Verify nothing else changed

- [x] 2.1 Run `maestro/run.sh distance`: `log-distance.yaml` and `log-from-home.yaml` exercise the note element
      (add, edit, discard, remove with confirmation, the row icon) by the same `id`/text selectors as before; confirm
      both still pass unchanged. This is the check that the restyle did not alter behavior, since no unit test
      exists for a screen's pure visual structure yet (`docs/test-strategy.md`'s Compose-integration/Roborazzi tiers
      are "not yet adopted").

      Revised during implementation: the `log_note`-tagged element's own accessible text does not surface into this
      environment's UiAutomator dump (`content-desc`/`text` both empty on the tagged node, confirmed via `adb shell
      uiautomator dump`) — the same class of limitation already hit by the `AlertDialog` buttons in `add-event-notes`.
      Confirmed with two independent semantics attempts (`mergeDescendants = true`, then an explicit
      `contentDescription`) that this is an environment/tooling limitation, not a code bug; kept the
      `contentDescription` in code regardless (harmless, likely helps real screen readers). Fixed at the flow level
      instead: `log-distance.yaml`'s combined `{id: log_note, text: X}` assertions became plain-text-only
      `assertVisible: X` (taps by `id` are unaffected). Passed: `log-distance (3m 7s)`, `log-from-home (2m 36s)`.
- [x] 2.2 Run `maestro/run.sh resilience rotation`: the note editor's own rotation coverage
      (`resilience/rotation.yaml`) is unaffected by this change (the editor itself is untouched), but the note
      element it opens from is now the restyled one — confirm the flow still passes.

      Revised during implementation: same id/text limitation as 2.1, same fix in `rotation.yaml` (one assertion).
      Passed: `setup (427ms)`, `rotation (4m 21s)`.
- [x] 2.3 Verify on-device (Android), in light and dark mode: the note element shows an outline and a floating
      "Note" label like the Kind and Vehicle selectors, "Add a note..." shows where a field's placeholder would, a
      long multi-line note still truncates to two lines with a trailing ellipsis, tapping the field opens the
      full-screen editor with no text cursor or keyboard ever appearing on the log event form itself, and the
      trailing trash-can action still shows only once a note is pending and still asks for confirmation.
      Confirmed: light and dark mode both match the Kind/Vehicle selectors' look, the field fills the row's full
      width (after the `propagateMinConstraints` fix, see design.md), the ellipsis truncation works, tapping opens
      the editor with only the editor's own keyboard ever appearing, and remove-with-confirmation reverts the field
      to "Add a note..." with the trailing icon gone.

## 3. Regression

- [x] 3.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Confirmed: `BUILD SUCCESSFUL in 13s` (65 actionable tasks: 8 executed, 57 up-to-date), and `openspec validate
      --all --strict` reports `14 passed, 0 failed` (only pre-existing INFO-level "requirement text is very long"
      notices, unrelated to this change).
