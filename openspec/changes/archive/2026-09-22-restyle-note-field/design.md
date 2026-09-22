# Design

## Context

`NoteField` (in `LogEventScreen.kt`, from `add-event-notes`) is today a plain `Row` with a `Text` (the prompt or the
truncated note, `maxLines = 2, overflow = TextOverflow.Ellipsis`, `.clickable(role = Role.Button)`) and, once a note
is pending, a separate `IconButton` (the trash can). It sits below `OdometerField` and above the implicit Save action
in the top bar, and looks unlike everything else on the form: `KindSelector` and `VehicleSelector` are both a real
`OutlinedTextField(readOnly = true)` inside `ExposedDropdownMenuBox`, `WayChoice`/`UnitChoice` are `SegmentedButton`
rows, and `MomentButton` (date/time/zone) is an `OutlinedButton`. Every one of those has a visible Material 3
container; the note element alone does not.

I verified directly against the `org.jetbrains.compose.material3:material3:1.9.0` sources (the version this project
pins) that `OutlinedTextField`'s own `value: String` overload has no ellipsis support: its `maxLines` parameter only
clips, since `BasicTextField` (what it wraps) has no `TextOverflow` parameter at all — text fields are meant to be
edited or scrolled, not ellipsized. So the existing, unchanged requirement that a long note is shown "at most two
rendered lines... end-ellipsized" cannot be met by putting the note's text directly into a real editable field's
`value`. This is the one technical fact this design turns on.

See proposal.md for the motivation; see the `distance-logging` spec delta for the exact behavior (unchanged from
`add-event-notes` except for the look).

## Goals / Non-Goals

**Goals:**
- Make the note element look like the form's other Material 3 fields (an outline, a floating "Note" label), using
  Material's own components for the chrome, per the project's "use Material 3 components and their defaults instead
  of re-implementing what they do" convention — not a hand-drawn border imitating one.
- Keep every existing behavior exactly as `add-event-notes` specified it: one tap target that opens the full-screen
  editor, the two-line end-ellipsized preview, the trash-can action and its confirmation, nothing editable in place.

**Non-Goals:**
- Any change to `LogEventState`, `LogEventProcessor`, `LogEventIntent`, the full-screen editor, storage, or the row
  icon on logged events (`vehicle-log`). None of that is touched.
- Real inline editing of the note. The field still never receives real text input; only the full-screen editor does.

## Decisions

### `OutlinedTextFieldDefaults.DecorationBox` with a plain `Text` as its content, not a real `OutlinedTextField`
Alternative considered: `OutlinedTextField(value = pendingNote ?: "", readOnly = true, label = {Text("Note")},
placeholder = {Text("Add a note...")}, maxLines = 2, ...)`, the same pattern `KindSelector`/`VehicleSelector` already
use. Rejected: confirmed from source (see Context) that this cannot ellipsize a two-line-plus note — it would clip
mid-character instead, which is a regression of an existing, unchanged requirement.

Chosen instead: `OutlinedTextFieldDefaults.DecorationBox` — the same public API `OutlinedTextField` is itself built
on, exposed precisely so a caller can keep Material's exact outline, label-float animation, colors and
enabled/disabled treatment while supplying custom content for the input area. The actual visible content — the one
thing that needs the ellipsis — is supplied as `innerTextField`: a plain `Text(..., maxLines = 2, overflow =
TextOverflow.Ellipsis)`, which is not a `BasicTextField` at all, so it has no cursor, no focus, no keyboard to
accidentally trigger — the "must not become an editable field on tap" requirement holds by construction, not by a
readOnly flag that has to be trusted.

This is still "using the Material 3 component and its defaults": the outline, the label animation, the colors, the
padding and the enabled/disabled treatment all come from `OutlinedTextFieldDefaults` unmodified. Only the strip of
content in the middle is custom, which is exactly what `DecorationBox` is a public, documented API for.

**Two wiring details verified against the pinned sources, not assumed, that change how the box above is used:**
- `DecorationBox`'s `placeholder` slot only renders while the field has *real* keyboard focus (its label-float and
  placeholder-visibility logic is driven by `interactionSource.collectIsFocusedAsState()`, and `1.9.0`'s public
  overload hardcodes a non-minimized label position). Since this box never receives real focus (there is no
  `BasicTextField` behind it to focus), relying on `placeholder` would mean "Add a note..." never actually shows.
  So the `placeholder` slot is not used at all: both the prompt and the real note text are rendered the same way,
  as the `innerTextField` content, and `value` is passed as that same non-empty text purely so the "Note" label
  stays in its minimized, floated position (the state a non-empty field is normally in) rather than the large,
  centered "empty field" position a real, never-focused empty field would otherwise show.
- The public `DecorationBox` has no `modifier` parameter (in the standard pattern it decorates a `BasicTextField`,
  which itself carries the modifier); since nothing here is a `BasicTextField`, the tap target, width and test tag
  go on an outer `Box` wrapping the `DecorationBox` call instead. Confirmed on-device: a plain wrapping `Box` alone
  left the field wrapping its own content's width (narrow, not matching the other fields on the form) — `Box`'s own
  `propagateMinConstraints = true` is what makes a modifier-less child like this one actually stretch to the
  wrapping `Box`'s `fillMaxWidth()`, so it is passed explicitly rather than left at its default `false`.

### The trash-can action moves into the field's own `trailingIcon` slot
Today it is a second `IconButton` next to the field in a `Row`. `DecorationBox` has a `trailingIcon` slot built for
exactly this (a field-level action, like Material's own "clear text" pattern), and placing it there makes the
trash can read as part of the one field rather than a second, adjacent control. The tap target and behavior are
unchanged (still its own `IconButton`, shown only when `pendingNote != null`, tagged `log_note_remove`); this is a
layout detail, not new behavior.

### Tapping the field: one `clickable` around the whole `DecorationBox`, excluding the trailing icon
Because the input-area content is a `Text`, not a `BasicTextField`, there is no focus/IME risk to guard against (unlike
`KindSelector`/`VehicleSelector`, which wrap a real field and rely on `ExposedDropdownMenuBox` to intercept taps
before the field can take focus). A single `Modifier.clickable(onClickLabel = "Add a note", role = Role.Button,
onClick = onOpen)` around the `DecorationBox` is enough; the trailing icon's own `IconButton` consumes its own taps
first, the same nested-tap-target behavior the current implementation already relies on.

### Label and placeholder text
`label = { Text("Note") }`, matching the short, generic labels already used ("Kind", "Vehicle"). `placeholder =
{ Text("Add a note...") }`, unchanged text from `add-event-notes`.

## Risks / Trade-offs

- **[Risk]** `DecorationBox` is a lower-level API than `OutlinedTextField`; its parameter list (interaction source,
  visual transformation, container) is more to wire correctly than a plain `OutlinedTextField` call → **Mitigation**:
  every parameter besides `value`, `innerTextField`, `label` and `placeholder` takes a sensible default or a trivial
  value for non-editable content (an unused `remember { MutableInteractionSource() }`, `VisualTransformation.None`,
  `singleLine = false`); the surface actually touched is small and the outline/label/color logic is entirely
  Material's own, unmodified.
- **[Risk]** A future Compose Multiplatform/Material3 upgrade could change `DecorationBox`'s signature (it is a
  slightly more specialized API than the everyday `OutlinedTextField`) → **Mitigation**: accepted; the same class of
  risk already exists for `ExposedDropdownMenuBox` elsewhere on this form, and `DecorationBox` is `OutlinedTextField`
  own foundation, unlikely to be removed without a deprecation path.
