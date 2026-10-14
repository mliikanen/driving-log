# Proposal

## Why

The specs quote a refueling's amount on screen as "42.3 L", but the app shows every fuel amount to two decimal places,
"42.30 L" (#26). The app is right: fuel amounts are entered and stored in hundredths of their unit, and the unit tests
and the `log-distance` Maestro flow already check "42.30 L". The specs are wrong, and a test written from them would
fail against a correct app.

## What Changes

- The on-screen strings the specs quote for a refueling's amount change from "42.3 L" to "42.30 L":
  - the example in "An event row shows its figure by kind";
  - the THEN of "A refueling's row" and "A refueling with mileage shows two rows";
  - the THEN of "A refueling's details".
- Nothing else changes. Every other scenario of these requirements is kept as it is.

Out of scope:
- A WHEN clause or a stored quantity written "a refueling of 42.3 L" (an amount, not text on screen), which stays.
- Any change to the app: its behavior already matches the corrected specs.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-log`: "Recent events on the details screen" (the THEN of two refueling scenarios) and "An event row shows its
  figure by kind" (the example).
- `event-details`: "The details screen shows a "Refueling" event's fuel details" (the THEN of "A refueling's details").

## Impact

Specs only. No code, test or Maestro flow changes: `EventRowContentTest` and `maestro/distance/log-distance.yaml`
already expect "42.30 L", and the details screen shows the same figure as the event row. Testers get nothing new.
