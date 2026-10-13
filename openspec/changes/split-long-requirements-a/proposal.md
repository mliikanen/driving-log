## Why

OpenSpec warns about a requirement over 500 characters, so the main specs can't be validated with `--strict` (issue
#13). `ratchet-spec-validation` stopped the backlog from growing; it still has 76 long requirements. This is the first
of four batches that shorten them: the three specs no in-flight change branch touches, `odometer-ocr-capture` (10
long), `vehicle-log` (8) and `refueling-logging` (6), 24 requirements in all.

## What Changes

- Every requirement over 500 characters in the three specs is rewritten to 500 characters or fewer, **with no change
  in behavior**: every SHALL, SHALL NOT and MAY in the old text is still stated, by the same requirement or by a new one.
- Most only get shorter: repeated cross-references, restated context and parenthetical asides are cut. Each keeps its
  name and every scenario, word for word.
- Four that can't fit are also split. Each keeps its name, its scenarios and its main behavior, and the rest becomes a
  new requirement with its own scenario:
  - `refueling-logging`, "The fuel type remembers the last choice, independent of the vehicle": the fixed list of fuel
    types becomes "The fuel type is chosen from a fixed list".
  - `vehicle-log`, "Recent events on the details screen": what a row shows becomes "An event row shows when it
    happened" and "An event row shows its figure by kind".
  - `odometer-ocr-capture`, "A detected reading is classified as a fuel amount, by label only": the MAY about a price
    sharing the volume label becomes "A number as near the volume label may also be a fuel-amount candidate".
  - `odometer-ocr-capture`, "An accepted scan's photo and detections are kept only if the entry is saved": which event
    a scan goes under becomes "A scan is stored under the event whose number it filled", and that it's never shown
    and outlives its event becomes "A stored scan is never shown and outlives its event".
- No app code changes. A test is added only where no existing test checks one of the six new scenarios. Requirement
  and scenario names stay as they are, so the code comments and Maestro flows that quote them still match.

Out of scope: the other 52 long requirements (batches B to D of #13), any change in behavior, rewording requirements
that are already short enough, and bringing back `openspec validate --all --strict` (the last batch does that).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `odometer-ocr-capture`: 10 requirements shortened, 2 of them split into 3 new requirements.
- `vehicle-log`: 8 requirements shortened, 1 of them split into 2 new requirements.
- `refueling-logging`: 6 requirements shortened, 1 of them split into 1 new requirement.

## Impact

- `openspec/specs/odometer-ocr-capture`, `vehicle-log` and `refueling-logging` once archived. The long-requirement
  count goes from 76 to 52.
- No app change: merging this publishes no build (`release-only-app-changes`) and needs no release notes.
- Branches that later change these specs base their MODIFIED blocks on the shorter text.
