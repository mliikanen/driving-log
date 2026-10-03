# Proposal

## Why

`add-odometer-ocr-capture` lets a photo fill in an odometer or trip reading, but when the log event form was opened
from the Home screen (not from a specific vehicle's details screen), the user still has to pick which vehicle the
reading belongs to themselves, from the vehicle selector. For a household with more than one vehicle, a detected
odometer value is itself a clue: a reading that closely matches one vehicle's own current known odometer, and not
another's, is probably that vehicle's. This is an idea to make that suggestion, not yet a designed feature — recorded
now, while the OCR capture flow it depends on is being proposed, so it isn't lost.

**This depends on `add-odometer-ocr-capture` existing first** — there is nothing to suggest a vehicle from until a
value can be detected at all.

## What Changes (Sketch — Not Yet a Committed Design)

- After a scan detects a candidate odometer reading, while the log event form's vehicle selector is showing (the
  Home-screen route only — a vehicle's own details screen route already fixes the vehicle, so this wouldn't apply
  there), compare the candidate's value against every vehicle's own current known odometer and suggest the closest
  plausible match.
- The suggestion is offered for the user to accept, not applied silently — switching which vehicle an entry is
  logged against from a guess, without the user noticing, risks a reading landing on the wrong vehicle's log by
  mistake.

## Open Questions (To Resolve Before This Can Be Applied)

- **Matching rule**: closest known odometer overall, or only vehicles where the candidate is a plausible small
  amount above their known odometer (the same "plausible trip" reasoning `add-odometer-ocr-capture`'s design uses
  for odometer-vs-trip classification, applied across vehicles instead of within one)? Needs picking, and needs
  real multi-vehicle data to sanity-check against (the single-vehicle test photos this session used don't exercise
  it).
- **Ambiguous matches**: two vehicles with similar mileage could both plausibly match a given reading. Does the
  suggestion pick the single closest one, or show a short list to choose from?
- **Presentation**: a pre-selected suggestion in the vehicle selector the user can change, a separate confirmation
  step, or something else — not designed here.
- **Trip-classified candidates**: a small trip-distance value carries much weaker vehicle-matching signal than an
  odometer-like one (many vehicles could plausibly have driven any given short trip); this idea may end up scoped to
  odometer-like candidates only.

## Capabilities

### Modified Capabilities
- `distance-logging`: the log event form's vehicle selector can offer a suggested vehicle when a scanned reading
  plausibly matches exactly one vehicle's own known odometer. The delta below captures only this much — a single
  unambiguous match, ignorable, never applied silently — leaving the open questions above (the exact matching rule,
  ambiguous-match handling, presentation) for whoever picks this up to resolve before implementing.

## Impact

- Depends on `add-odometer-ocr-capture` (specifically its candidate classification and the vehicle selector's
  existing "last vehicle logged for" logic in `distance-logging`) being implemented first.
- No impact until the open questions above are resolved and this proposal is revisited.
