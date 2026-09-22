# Proposal

> **Decided.** The open questions of the stub were settled by the developer before apply (see the design's Decisions), including one that grew the scope: this change also adds a "Kind" selector to the form, ready for a second kind of event later. It builds on the archived `add-landing-screen` and `add-direct-logging`, which already show "Log event" on the Home screen while the form itself, and the details screen's action, still say "Log distance".

## Why

The Home screen's action is **"Log event"** but the form it opens, and the action on the vehicle details screen that opens it, are **"Log distance"**. That was right while a distance entry was the only kind of thing that can be logged; the app's purpose also includes refuelings, receipts and other contextual notes, all of them events on a vehicle's log. The name should say what the screen is for before more kinds arrive, the same word should be used everywhere the user sees it, and the form should already have a place for the user to see (and later choose) what kind of event they are logging.

## What Changes

- **Names the user sees:** the details screen's action, the form's title and the specs read **"Log event"** everywhere "Log distance" named the screen or the action. **"Trip distance" and "New odometer" keep their names**: they describe how a distance entry is measured, not the screen, and stay meaningful once other kinds of event exist.
- **A "Kind" selector**, at the top of the form: a dropdown (the same `ExposedDropdownMenuBox` pattern as the vehicle selector) listing the kinds of event that can be logged — today only **"Distance"**, selected, and shown **disabled** (Material's own disabled treatment, like the Home screen's placeholder tiles), since there is nothing to choose until a second kind exists. On the Home screen route it **shares one row with the vehicle selector**, split in half, of equal height, the kind on the left and the vehicle on the right; from a vehicle's details screen (no vehicle selector) it takes the row alone. This is scaffolding: nothing about what can be logged changes, and the choice does nothing until a second kind exists.
- **Names in the code and the test tags:** the screen, processor, state, intent, effect and navigation key classes (`LogDistance*` become `LogEvent*`), the dependency graph's factory, and the details screen's `LogDistanceClicked`/`ShowLogDistance`. The Maestro tag for the details action becomes `log_event` — which collides with an existing, unrelated tag on the full log screen (each row of a vehicle's history is also tagged `log_event`, an earlier coincidence); that row's tag is renamed to `log_history_row` to make room, a small extra fix found while grounding this change.
- **Not renamed:** `LogDistanceRules.kt`, `LogDistanceResult`, `LogDistanceError` and `validateLogDistance` (they validate a distance entry's rules, which stay distance-specific, like the way names), the column `vehicle.log_distance_tenths` and its migration, the query `updateLogDistanceTenths`, and the navigation key's `serialKey` string `"vehicle-log-distance"` (so a back stack saved by an earlier build still restores; a comment says why it looks legacy). The package `vehicle.distance` keeps its name too, since the rules and time zone/moment helpers that stay there are still about distance entries.
- The `distance-logging` capability is **not renamed**: it still only covers distance entries.

Out of scope: a second kind of event (refueling, notes: each its own future change, which is what the Kind selector is built for), changing what a distance entry is or how it is validated, a new icon, translations.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `distance-logging`: the requirement that opens the form from the details screen is renamed and reworded ("Log a distance from the vehicle details screen" becomes "Log an event from the vehicle details screen"); the other requirements that say "the log distance form" reword it to "the log event form"; a new requirement adds the Kind selector.
- `vehicles`: the details screen requirement's description and its scenario that name the action and the form.

## Impact

- `shared/` commonMain: `vehicle/distance/LogDistanceContract.kt` → `LogEventContract.kt` (`LogDistanceState`→`LogEventState`, `LogDistanceIntent`→`LogEventIntent`, `LogDistanceEffect`→`LogEventEffect`, plus the new `LogKind` and `KindSelected`), `LogDistanceProcessor.kt`→`LogEventProcessor.kt`, `LogDistanceScreen.kt`→`LogEventScreen.kt` (plus the new `KindSelector` composable and the shared row layout), `VehicleNavKeys.kt` (`LogDistanceNavKey`→`LogEventNavKey`, serial key unchanged), `di/AppGraph.kt` (`logDistanceProcessorFactory`→`logEventProcessorFactory`), `landing/LandingNavKey.kt` (import), `vehicle/details/VehicleDetailsContract.kt`, `VehicleDetailsProcessor.kt`, `VehicleDetailsScreen.kt` (the intent, effect, callback names and the `log_distance`→`log_event` tag), `vehicle/log/VehicleLogScreen.kt` (the freed-up `log_event` row tag→`log_history_row`).
- Tests: `LogDistanceProcessorTest.kt`→`LogEventProcessorTest.kt` and every test referencing the renamed classes; `VehicleNavKeysTest.kt`, `VehicleDetailsProcessorTest.kt`; new tests for the Kind selector.
- `maestro/`: every flow's `id: log_distance` becomes `id: log_event` (`clock/time-format.yaml`, `distance/log-distance.yaml`, `distance/log-from-home.yaml`, `resilience/offline-and-restart.yaml`, `resilience/rotation.yaml`, `theme/state.yaml`); the flows add the Kind selector's presence to what they check.
- No storage or schema change (the new `kind` field is never persisted to the database, only to the screen's own saved state, always `DISTANCE`), no new dependency, no permission.
