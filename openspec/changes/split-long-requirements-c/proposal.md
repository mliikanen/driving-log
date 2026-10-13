## Why

OpenSpec warns about a requirement over 500 characters, so the main specs can't be validated with `--strict` (issue
#13). Batches A and B brought the count from 76 to 36. This is batch C: `distance-logging` (17 long) and `vehicles`
(7), 24 requirements in all.

## What Changes

- Every requirement over 500 characters in the two specs is rewritten to 500 characters or fewer, **with no change in
  behavior**: every SHALL, SHALL NOT and MAY in the old text is still stated, by the same requirement or by a new one.
- As in batches A and B, each keeps its name and every scenario word for word. Some are only shortened; the rest are
  split, and what moves out becomes new requirements, each with its own scenario (the design lists them).
- `vehicles`' odometer field requirement (2,712 characters, 29 scenarios) becomes the field itself plus requirements
  for how it shows its value, backspace and clear, a first typed zero, what it accepts, and that every odometer field
  behaves the same.
- Tests: each new scenario gets the test that checks it, existing or new.

Out of scope: any change in behavior, fixing wording that is stale but not too long (filed as issues), and batch D.

**In-flight branch:** `add-vehicle-hiding` (proposed 2026-10-03, not applied) modifies three of these requirements
("The vehicle is chosen with a selector when logging starts from the Home screen", "The vehicle last logged for is
remembered apart from the log", "Vehicle list"). Its MODIFIED blocks are based on the long text, so they need to be
rebased onto the shorter text before it is applied. That is filed as an issue rather than done here.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `distance-logging`: 17 requirements shortened, 10 of them split into 14 new requirements.
- `vehicles`: 7 requirements shortened, 4 of them split into 11 new requirements.

## Impact

- The two main specs once archived. The long-requirement count goes from 36 to 12.
- No app change: merging this publishes no build and needs no release notes.
