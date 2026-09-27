# Design

## Context

Material Design 3's guidance for a required field (`m3.material.io/components/text-fields/guidelines`, via search —
the page is JS-rendered and could not be fetched directly for a verbatim quote, so this is a synthesized summary,
not a literal citation) is specific: mark the label with a trailing "*", and explain the convention once, typically
near the bottom of the form ("* indicates a required field"). Color is a secondary detail of that same convention
(if the label text itself is colored, the asterisk matches it) — not a standalone substitute for the asterisk, since
color alone is invisible to a screen reader and unreliable for colorblind users.

Today this app does the reverse: the one optional field on the vehicle forms (license plate) carries a `(optional)`
suffix; every required field (name, odometer) is unlabeled. That works when there is exactly one field to call out,
but it is a *negative* signal (marks what you don't have to do) rather than the *positive* one M3 recommends (marks
what you do), and it doesn't extend to the log event form's distance/odometer field at all — nothing there indicates
it's required.

The add-vehicle form's field order compounds this for the odometer specifically: today it's `picture, name, plate,
type, color, unit, odometer` — the one field that blocks saving comes after two that never can (type and color both
always have a default). The log event form doesn't have this problem: its distance/odometer field already sits
ahead of everything but the note, once the kind/vehicle/way/unit context it depends on is chosen — so only the
marking convention applies there, not a reorder.

## Goals / Non-Goals

**Goals:**
- Every required field across the add-vehicle, edit-vehicle and log event forms is positively marked, per M3.
- The add-vehicle form's required fields (name, odometer) come before its always-satisfied ones (type, color).

**Non-Goals:**
- Not touching Type or Color (add-vehicle) or any other field that always has a default and can never be emptied —
  they are not "required" in the sense this convention marks.
- Not changing validation, error messages or when Save is enabled — this pairs with `disable-invalid-save`'s
  disabled-Save behavior (a separate, already-proposed change) but doesn't depend on code from it; the marking here
  is static UI, independent of whether Save happens to be tappable at any given moment.
- Not reordering the log event form — its distance/odometer field is already well-placed.
- Not retrofitting every other form in the app (there are none with more than one required field today outside
  these three); a future form follows the same convention as it's built, not as a batch retrofit here.

## Decisions

- **A small shared `RequiredFieldNote()` composable**, in `com.mikonoma.drivinglog.ui.Components.kt` alongside
  `BackButton`/`CloseButton`: a single `Text("* indicates a required field", style = MaterialTheme.typography.bodySmall,
  color = MaterialTheme.colorScheme.onSurfaceVariant)`, placed as the last item in each form's scrolling content
  (before `ScreenBottomSpace`, matching the project's existing scroll-content convention). One composable, one
  wording, so the three forms can't drift into slightly different phrasings.
- **The asterisk goes directly in the label string**, not a separate icon or color treatment: `"Name *"`,
  `"Current odometer *"`, and for the log event form's dynamic label, `"Trip distance *"` / `"New odometer *"`. This
  is the simplest reading of M3's own guidance and needs no new component — every field here already takes a plain
  `String` or `@Composable` label.
- **Drop the license plate's `(optional)` suffix.** Keeping both conventions on the same form (one field marked
  optional, two marked required) would read as two competing systems for the same fact. Once every required field
  carries its own mark, the absence of a mark already says "optional" — matching M3's own framing (mark the
  required ones; unmarked is the default, understood meaning).
- **Field order**: `picture, name, license plate, unit, odometer, type, color`. Unit moves up with odometer, not
  odometer alone — a reading is meaningless without knowing its unit, so the two stay adjacent; type and color
  (cosmetic, always defaulted) move to the end. Confirmed with the user directly rather than assumed, given a
  layout reorder is materially observable behavior with more than one reasonable answer.
- **One change, two capabilities.** `vehicles` (add/edit vehicle forms) and `distance-logging` (log event form) both
  get the identical convention in the same change, per the user's own call to fold them together — reviewing and
  applying "mark required fields with *" once is more coherent than as two separate change proposals that would
  each re-derive the same M3 research and design decisions.

## Risks / Trade-offs

- Dropping `(optional)` is a small regression for a user who specifically scans for that word; the trailing `*` on
  the required fields is the intended replacement signal, consistent with M3's own recommended pattern.
- Purely visual and layout: no Roborazzi baselines exist for these screens yet (as noted in `close-icon-for-forms`),
  so nothing automated needs updating beyond the manual/Maestro check in tasks below.
