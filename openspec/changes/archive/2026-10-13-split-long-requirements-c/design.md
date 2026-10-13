# Design

Batch C follows batches A and B (`openspec/changes/archive/2026-10-13-split-long-requirements-a/design.md`, and the
`-b` design for the generator and platform notes): shorten first, split only what can't fit, keep every requirement's
name and every scenario word for word, state every old SHALL, SHALL NOT and MAY again, give each new requirement one
new scenario and the test that checks it.

## Context

- `add-vehicle-hiding` (a proposal from 2026-10-03, not applied) has MODIFIED blocks for three of the requirements
  shortened here: "The vehicle is chosen with a selector when logging starts from the Home screen", "The vehicle last
  logged for is remembered apart from the log" and "Vehicle list". Its blocks carry the long text.
  `suggest-vehicle-from-odometer` only adds a requirement to `distance-logging`, so it is not affected.
- Both specs have wording that is stale but not too long (the log event form "for one distance entry", a Kind selector
  rule for a single kind that can no longer happen, "Add a vehicle" returning to the vehicle list where saving from the
  Home screen returns to the Home screen). It stays as it is; it is filed as an issue.

## Decisions

### 1. Shorten the requirements `add-vehicle-hiding` also modifies

Leaving those three long would keep the two specs from reaching zero and would only postpone the same rebase.
`add-vehicle-hiding` has to be re-checked against `main` anyway before it is applied (`atomic-log-events` and the
batches changed much of what it builds on), and the shortened requirements keep their names and scenarios, so its
MODIFIED blocks can be rebuilt on the new text. An issue records it on that branch's behalf.

### 2. What is split out

`distance-logging`: the scan rule out of the trip/odometer choice; the anchor rule out of the lower-count
confirmation; the required-field mark out of the trip distance rule; the remembered tenths and the conversion out of
the unit choice; the past/future rule and the weekday and clock format out of the date and time; the form's
explanation and the count's entry rules out of the anchor requirement; keeping what was entered out of the vehicle
selector; the fallback out of the remembered vehicle; the shared row out of the Kind selector; no in-place editing
out of the note element; and no crop out of the photo element.

`vehicles`: the field order, the required marks and the close icon out of "Add a vehicle" (the close icon shared with
"Edit a vehicle" as one requirement); the display, backspace and clear, the first zero, the accepted characters and
"every odometer field behaves the same" out of the odometer field; the required mark out of "Edit a vehicle"; and the
readings offered and the switch to tenths out of the add-vehicle scan.

## Risks / Trade-offs

- **A reworded sentence quietly changes meaning.** → One task per spec compares the old and the new text.
- **`add-vehicle-hiding` applied without a rebase** would bring the long text back, and the requirement-length check
  fails it (a long requirement that grew). → The issue names the three requirements.
