# Proposal

## Why

The note element `add-event-notes` shipped as plain, unbordered text (a prompt or a two-line preview) specifically to
keep it unobtrusive. In practice that makes it look like a caption or a piece of read-only information rather than
something tappable — none of the form's other fields look like it, and every other tappable field on the same form
(Kind, Vehicle, the trip distance/new odometer amount, the date/time/zone buttons) has a visible Material 3 border,
fill or outline that signals "you can interact with this." The note element should look at least as interactive as
its neighbors, using Material Design's own affordance for "a field that opens something else on tap": styled like a
text field, without actually becoming an editable text field in place.

## What Changes

- The note element on the log event form is restyled as a Material 3 outlined text field (the same visual family as
  the Kind and Vehicle selectors already on this form): an outline, a floating "Note" label, and — this is a visual
  change only — the current content shown where a field's value normally goes: "Add a note..." when nothing is
  pending, or up to two rendered lines of the pending note, end-ellipsized, when something is.
- It keeps behaving exactly as before: a single tap target that opens the full-screen note editor, never an in-place
  editable field. It SHALL NOT gain a text cursor, a keyboard, or any other affordance of a real editable field on
  tap — only the full-screen editor accepts typing, unchanged from `add-event-notes`.
- The trash-can action, the removal confirmation, the full-screen editor, saving behavior, the row icon on logged
  events — all of that is unchanged. This is a look-only revision of one already-shipped requirement.
- This explicitly supersedes `add-event-notes`'s design note that the element is "not a bordered field like the rest
  of the form" — that was the right call at the time (deliberately unobtrusive) and is now deliberately reversed.

## Capabilities

### Modified Capabilities
- `distance-logging`: the "A note can be added to the log event form" requirement's description of what the note
  element looks like changes from plain, unbordered text to a Material 3 outlined field. No other requirement in
  this capability (the editor, removal, saving) changes.

## Impact

- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/distance/LogEventScreen.kt`: `NoteField`'s
  implementation changes from a plain `Row`/`Text` to a Material 3 outlined-field look. Test tags (`log_note`,
  `log_note_remove`) are unchanged, so no Maestro flow needs new selectors, only a visual re-check.
- No change to `LogEventState`, `LogEventProcessor`, `LogEventIntent`, storage, or any other screen.
- `docs/color-palette.md`/theme: none — the field uses the same `OutlinedTextField` color scheme the Kind and
  Vehicle selectors already use, no new colors.
