# Proposal

> **Builds on `add-landing-screen`** (archive that one first: it creates the Home screen's "Log event" tile that this change makes usable). The request behind both was one idea, planned as separate changes: the landing screen, this change (**logging directly from the landing screen**) and
> `add-trip-logging` (a stub).

## Why

Logging a distance today takes four steps before the first digit: open the vehicle list, open the vehicle, tap "Log distance", then type. Most people log for the same vehicle most of the time. The landing screen makes a direct route possible: a "Log event" action that opens the log form at once, with the
vehicle chosen in the form itself and, to save a tap, **already set to the vehicle the user last logged for**.

## What Changes

- **The Home screen's "Log event" tile works** (it is enabled when the user has at least one vehicle; with none it stays disabled, as the landing screen already exposes any disabled tile, with no extra hint). It opens the **log distance form with a vehicle selector at the top**: a dropdown listing the user's vehicles by name (the same order as the vehicle list, each with its icon or picture and, when it has one, its plate), showing the chosen vehicle.
- **The selector starts on the vehicle the user last logged for.** That vehicle is **remembered in storage of its own**, not worked out from the logged events: a small key-value table (`app_state`) written when an entry is saved, in the same transaction as the entry. With nothing remembered (a new install, or an update from a version without it) or when the remembered vehicle is
  gone, the selector starts on the first vehicle by name. No backfill from the log.
- **Changing the vehicle in the form** shows that vehicle's known odometer and log, and sets the unit the way opening the form for that vehicle would (its family and its remembered tenths choice); the digits typed so far are kept (converted to the unit, as when the unit is changed by hand), and so are the way, the date, the time and the time zone.
- **From the vehicle details screen the form is as today**: the vehicle is fixed and there is **no selector**. Saving an entry from there also makes that vehicle the remembered one.
- Saving from the Home screen route returns to the Home screen; saving from the details returns to the details.

Out of scope: other kinds of event (refueling, notes; the tile's name is ready for them, the form stays "Log distance" until they exist), trips (`add-trip-logging`), remembering anything else (the trip's vehicle, the last way or unit), hiding vehicles from the selector (there is no hiding yet), and a per-vehicle theme for the form.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `distance-logging`: adds the requirements for logging from the Home screen, for the vehicle selector, and for the remembered vehicle (all ADDED; the existing requirements are unchanged, and the details route keeps working as specified).

## Impact

- `shared/` commonMain: `LogDistanceProcessor` (a chosen vehicle, switching, the selector's list; the `vehicleId` argument may be empty for "choose"), `LogDistanceContract` (state and intent for the chosen vehicle), `LogDistanceScreen` (the selector), `LogDistanceNavKey` (an empty vehicle id means the selector), `LandingProcessor` and `landingTiles` (the tile enabled), the landing screen's navigation.
- Data: migration `6.sqm` (schema version 7): `CREATE TABLE app_state (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)`; `AppState.sq` queries; `VehicleRepository.observeLastLoggedVehicleId()` and the two logging writes (`addDistanceEntry`, `addOdometerAnchor`) also writing it, in their transactions; `SqlDelightVehicleRepository` and the fake.
- Tests: processor tests for the selector, repository and migration tests for the table.
- `maestro/`: a new `distance/log-from-home.yaml` (the happy path from the Home screen and back), listed in the `distance` manifest.
- No new dependency, no permission. Offline-first: nothing here needs a network.
