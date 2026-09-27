# Design

## Context

This is an idea split out of `add-odometer-ocr-capture`'s proposal, recorded before that change is even built, so it
isn't lost — not a committed design. It depends entirely on that change's candidate detection and classification
existing first; there is nothing to suggest a vehicle from until a reading can be detected and classified at all.

`distance-logging` already has a "vehicle last logged for" memory and a vehicle selector shown when the log event
form is opened from the Home screen (not from a specific vehicle's details screen, where the vehicle is already
fixed and this idea wouldn't apply). This idea adds a second, reading-driven signal alongside that existing
"last logged for" default — it does not replace it.

## Goals / Non-Goals

**Goals:**
- Reduce the chance of an odometer photo landing on the wrong vehicle's log in a multi-vehicle household, by
  suggesting (never silently choosing) a vehicle when a detected reading clearly points to one.

**Non-Goals:**
- Not designed here: the exact matching rule, how an ambiguous multi-vehicle match is handled, or how the suggestion
  is presented — see the open questions below. This change should not be applied until those are resolved.
- Not extending the suggestion to trip-classified candidates (see open questions) unless a later revision of this
  design decides a trip value carries useful vehicle-matching signal after all.

## Open Questions

These need answers — informed by real multi-vehicle usage data, which the single-vehicle test photos used for
`add-odometer-ocr-capture` don't provide — before this can move from idea to a real design:

1. **What counts as "a plausible match"?** The closest vehicle by known odometer, or only vehicles where the
   candidate is a small, plausible amount above their known odometer (reusing `add-odometer-ocr-capture`'s own
   "plausible trip" reasoning, applied across vehicles instead of within one)? A pure "closest wins" rule risks
   confidently picking the wrong vehicle when two happen to have similar mileage.
2. **What happens when more than one vehicle plausibly matches?** Offer nothing (conservative, what the spec delta
   above currently requires), or show a short ranked list instead of a single suggestion?
3. **How is the suggestion presented?** Pre-select it in the vehicle selector with a visible "why" (so the user
   isn't confused about why their usual vehicle isn't chosen), a separate one-time prompt, or something else?
4. **Does a trip-classified candidate carry any useful signal at all?** Many vehicles could plausibly have driven
   any given short trip distance, unlike an odometer reading, which is close to unique per vehicle. Likely scoped to
   odometer-like candidates only, but not decided.

## Risks / Trade-offs

- **[Risk]** A wrong suggestion, if ever presented too confidently, could mislead a user into logging against the
  wrong vehicle just as easily as no suggestion at all → **Mitigation, once designed**: the spec delta already
  requires the suggestion be accept-or-ignore, never silently applied; open question 3 (presentation) should keep
  that visible, not bury it.
