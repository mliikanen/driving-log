# Design

## Context

- **What OpenSpec allows.** OpenSpec 1.14 measures a requirement's description: the text between its header and its
  first scenario. A MODIFIED block replaces the whole requirement, and both `openspec validate` and `openspec archive`
  refuse one that leaves out a scenario the main spec has ("archive refuses to drop them"). So a MODIFIED requirement
  can't hand a scenario over to a new requirement; every scenario stays where it is.
- **What else quotes these specs.** Requirement and scenario names are quoted in code comments (for example
  `FuelUnit.kt` quotes "The fuel amount's unit remembers the last choice, independent of the vehicle") and in Maestro
  flow comments.
- **No other branch has a delta for these specs.** None of the in-flight `change/*` branches has a delta spec for any
  of the three. `add-fuel-type-ocr` is only a proposal so far (2026-10-03), but it names `refueling-logging` as a
  capability it will modify.

## Goals / Non-Goals

**Goals:**

- Every requirement in `odometer-ocr-capture`, `vehicle-log` and `refueling-logging` is 500 characters or fewer.
- The behavior the specs describe is exactly what it was.

**Non-Goals:**

- Improving, correcting or updating what a requirement says, even where it reads oddly or another change has made it
  stale. That's a behavior change and needs its own change.
- Shortening requirements that are already 500 characters or fewer.
- The other 52 long requirements, and switching back to `openspec validate --all --strict`.

## Decisions

### 1. Shorten first, split only what can't fit

Most of the excess is wording, not behavior:
- cross-references that repeat a requirement's full name where the capability name is enough (`distance-logging`
  instead of four requirement titles);
- context restated from other requirements;
- examples that a scenario already shows (the live scanner's "ODO 71140" is in "Readings are boxed live");
- asides that explain why ("the fuel amount field and the mileage section are independent").

Cutting those brings 20 of the 24 under 500. Four are still too long, so each is split: the requirement keeps its name,
its scenarios and its main behavior, and each behavior moved out becomes an ADDED requirement:

| Requirement (kept) | Moved out into |
|---|---|
| `refueling-logging`: The fuel type remembers the last choice, independent of the vehicle | The fuel type is chosen from a fixed list |
| `vehicle-log`: Recent events on the details screen | An event row shows when it happened; An event row shows its figure by kind |
| `odometer-ocr-capture`: A detected reading is classified as a fuel amount, by label only | A number as near the volume label may also be a fuel-amount candidate |
| `odometer-ocr-capture`: An accepted scan's photo and detections are kept only if the entry is saved | A scan is stored under the event whose number it filled; A stored scan is never shown and outlives its event |

*Alternative:* split every long requirement into one-behavior requirements. That gives more, smaller requirements, but
because every scenario has to stay in its MODIFIED block, each split needs a new scenario that mostly repeats an old
one. Splitting only where needed keeps those repeats to six.

### 2. Every normative statement survives

Each SHALL, SHALL NOT and MAY in the old text is still stated, in the same requirement or in the one it moved to.
Wording may change ("disabled and SHALL NOT be tappable" becomes "disabled (not tappable)"), but conditions, values,
lists and exceptions don't. The fixed lists (fuel types, volume labels, odometer and trip labels) stay word for word
and in the same order. A requirement's own name stays the same, so code and flow comments that quote it still match.

The fuel-amount MAY becomes its own requirement. OpenSpec requires SHALL or MUST in every requirement, so the new one
states the rule the MAY follows from (candidates are told apart only by their nearest label) as a SHALL, keeps the MAY,
and adds the SHALL the old text pointed at: the user resolves it by picking a candidate.

### 3. New requirements get new scenarios, not copies

Each ADDED requirement needs a scenario of its own, and the obvious ones are already in the requirement it came from.
Rather than copying one, each new scenario checks a case the old ones don't: a refueling's row in the full log from
another zone, an "Odometer reading" row's missing plus sign, the full list for an "Other" vehicle, a scan stored under
an "Odometer reading," a scanned refueling whose details show only the photo the user attached, and the user picking
the amount over the price.

### 4. Tests: find the one that already checks each new scenario

The six new scenarios describe behavior that already ships, so most should already be checked somewhere (the
`vehicle-log` rows by the log row tests, the fuel type list by the refueling form tests, the scan links by the
repository tests). Apply looks for the test that checks each one, in the lowest kind of test that can check it
(`docs/test-strategy.md`), and adds a test only where none does. No app code changes.

## Risks / Trade-offs

- **A reworded sentence quietly changes meaning.** → Decision 2 is checked sentence by sentence in apply: one task per
  spec compares the old and the new text and lists, in the task, any statement whose wording changed in a way that
  could be read differently.
- **A new scenario describes something the app doesn't do.** → A new scenario is the one thing here that asserts
  something not already written down. Decision 4's test search shows whether the app does it; a scenario that turns
  out wrong is rewritten to what the old requirement says, never the app changed to fit it.
- **Branches that later change these specs have to base their MODIFIED blocks on the new text.** → None has a delta
  for them yet. `add-fuel-type-ocr` will write its `refueling-logging` delta against whatever `main` holds when its
  specs are written, so the shorter text is what it starts from.
