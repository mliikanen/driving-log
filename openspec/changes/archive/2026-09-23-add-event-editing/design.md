# Design

## Context

The project context states: *"The data model shall be additive, not accumulative: every trip, odometer reading,
refueling and diary entry is stored as its own record, and records are only added, never merged into running
totals."* `VehicleEvent.sq`'s comment says *"The log is append-only: there is deliberately no update or delete query
for events"*, and `distance-logging`'s "Logging only adds to the log" and `vehicle-log`'s "The log is not changed by
editing the vehicle" both codify it today. The previous version of this change's design (written as a stub) called
the choice between mutating a row in place and appending a superseding correction record "the central open
question... not a detail to settle while writing tasks... it must be decided before `/opsx:apply`, with the
developer, not guessed." The developer has now made that decision directly: mutate in place, by the same reasoning
notes already use (a note is a plain field that gets overwritten, not versioned).

See proposal.md for the motivation and the exact scope (note only). See the `event-details`, `vehicle-log` and
`distance-logging` spec deltas for the precise behavior.

## Goals / Non-Goals

**Goals:**
- Let an already-saved event's note be added, changed or cleared, reusing the existing note-editing UI as-is.
- Keep the change small and low-risk by scoping it to the one field (note) that provably cannot affect any
  derivation the log's other requirements depend on.

**Non-Goals:**
- Editing the figure, moment, unit, time zone, way or kind of a logged event. Nothing here touches
  `distance-logging`'s validation requirements ("The new odometer must be higher than the previous known odometer",
  etc.) or `vehicle-log`'s odometer-derivation requirements, because none of the fields those depend on become
  editable.
- Photos — `add-event-pictures` extends this same "Edit" action to manage them, as its own change.
- Deleting an event outright. "Editing... info" is not "removing an event."

## Decisions

### Mutate in place, narrowly scoped to the note field only
The original stub weighed two options for editing *in general* (any field): mutate in place, or append a correction
record that supersedes the original. Scoping this change to the note field only changes the shape of that trade-off
completely: the note plays no part in `vehicle-log`'s current-odometer or previous-known-odometer folds (those read
only `type`, `occurred_at`, `odometer_meters`, `distance_meters`), so a mutated note can never retroactively change a
figure anywhere else in the log — the exact risk that made "append a correction" attractive for the general case
(question 4 of the original design: *"a correction of an old entry can retroactively change every current-odometer
value computed since"*) simply does not exist for a note-only edit. Mutating in place is therefore not just simpler,
it has no downside here: no derivation logic needs re-auditing, and there is nothing to keep a correction history of
that would be lost.

If a future change widens editing to the figure or moment, that trade-off returns and would need its own decision —
this change deliberately does not pre-commit to an approach for that case.

### Reuse the note editor as-is, in an "edit a saved note" mode
`add-event-notes`' full-screen editor (`shared/.../vehicle/distance/LogEventScreen.kt`'s note editor content, and
its processor's pending-note intents) already implements exactly the interaction wanted here: seeded with the
current text, back navigation commits what was typed (attaching blank text as "no note"), and an explicit "Discard"
returns without changing anything. The only difference is what "commit" does: attach to an in-memory pending draft
(composing) vs. call `updateEventNote` directly (editing a saved event). The editor's own requirement text
(`distance-logging`, "The full-screen note editor") is unchanged by this delta; only `event-details` gains a new
requirement describing the "Edit" action that opens it in the second mode.

### No separate "remove note" action on the details screen
The compose-time note element has its own dedicated trash-can action with a confirmation dialog, because on that
screen the note preview and the removal action sit side by side, at all times, as a compact form field. The details
screen is different: "Edit" already opens the same editor, and clearing the text there and navigating back already
removes the note (mirroring the compose-time editor's own "Attaching blank text clears the note" scenario), so a
second, separate removal entry point on the details screen would just be a shortcut for the same two taps — not
worth the extra UI for this change's narrow scope.

### The "Edit" action is available whether or not a note exists yet
Symmetric with the compose-time note element (tapping it opens the editor whether or not a note is currently
pending): "Edit" lets a note be added to an event that was logged without one, not only changed or cleared. This
needs no extra design — it is the same editor, seeded with an empty string when the event has no note.

## Risks / Trade-offs

- **[Risk]** Breaking the storage layer's literal "no update query" comment/convention, even scoped to one column →
  **Mitigation**: the query is named and scoped narrowly (`updateEventNote`, note only, never touching
  `odometer_meters`/`distance_meters`/`occurred_at`/`type`), and the spec carve-outs name exactly this one action,
  so nothing else can widen it by accident without its own spec change.
- **[Risk]** No audit trail — the previous note text is simply gone once overwritten → **Mitigation**: accepted,
  matching how the compose-time editor already works (nothing keeps the text a user types over while composing,
  either); revisit only if a future change asks for one.
