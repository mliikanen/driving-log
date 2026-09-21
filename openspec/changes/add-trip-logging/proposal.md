# Proposal (stub)

> **Stub.** Short on purpose: it fixes the scope and lists the open questions, to be settled before `/opsx:apply`. It is the third part of the request that also gave `add-landing-screen` (the "Trip" tile, present and disabled there) and `add-direct-logging`. Nothing here is built yet.

## Why

The project context has always described a **trip** as *a span of time with a start and an end that groups distance and refueling entries*, and left it as a separate, upcoming change. Today every entry stands alone in a vehicle's log, so "how far did I drive on Saturday's journey, and what did it cost"
cannot be answered. The Home screen now has a "Trip" tile waiting for it: **start a trip** when leaving, **end it** on arrival, and everything logged in between belongs to it.

## What Changes

- The Home screen's **"Trip" tile becomes "Start trip"** when no trip is open and **"End trip"** while one is (the tile toggles, with its icon).
- **Starting a trip** picks the vehicle (the selector and the remembered last vehicle of `add-direct-logging`) and records the start moment; **ending it** records the end moment. A trip has a vehicle, a start and an end, each moment with its time zone as every date and time in the app.
- **Entries logged while a trip is open belong to it** (open question 2), and a trip's log shows its distance total, derived from its entries and never stored (the additive data model: totals are derived).
- Trips persist and work offline, like everything else.

Out of scope for the stub: refueling entries (they do not exist yet; a trip groups them once they do), sharing trips between users, routes or GPS tracking, automatic start and end, reports and exports.

## Capabilities

### New Capabilities

- `trip-logging`: starting and ending a trip, what belongs to it, the tile that reflects its state.

### Modified Capabilities

- `distance-logging` (probably): an entry logged while a trip is open is attached to it. Decided when the open questions are.
- `app-shell` (the tile): "Home screen offers the main actions" changes the "Trip" tile's behavior; a delta is written when this is applied, on top of `add-landing-screen`.

## Impact

- Data: a trip table (migration after `add-direct-logging`'s `6.sqm`), and how events refer to it (open question 2); `VehicleRepository` or a `TripRepository`.
- `shared/` commonMain: trip processors and screens (start with a vehicle selector, the open-trip state, end), `landingTiles` and the Home tile.
- `maestro/`: a `trip` manifest with the happy path (start, log, end).
- Depends on `add-landing-screen` (the tile) and `add-direct-logging` (the vehicle selector and the remembered vehicle); either can be applied first only if this change is ordered after them.
