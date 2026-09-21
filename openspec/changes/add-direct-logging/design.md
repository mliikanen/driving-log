# Design

## Context

Observed today: `LogDistanceNavKey(graph, vehicleId)` builds `LogDistanceProcessor` (assisted factory with the vehicle id). The processor combines `observeVehicle(id)` and `observeLog(id)` in one observer, starts the unit from the vehicle's family and its remembered tenths choice once (`unitInitialized`), and saves with `addDistanceEntry` or `addOdometerAnchor`, each of which also writes the vehicle's
remembered tenths choice in the same transaction (a `vehicle.log_distance_tenths` column). The state is `@Serializable` (what the user typed and chose is saved; repository data is `@Transient`). The screen is a `Scaffold` with "Log distance", a back arrow and Save. The database is at schema version 6 (`5.sqm` is the last migration), the vehicle list is ordered by name without regard to case in `VehicleListProcessor`,
and vehicles cannot be deleted (or, yet, hidden).

## Goals / Non-Goals

**Goals:**
- Log from the Home screen with the vehicle chosen in the form, starting on the vehicle last logged for, remembered in storage of its own.
- Keep the details route exactly as it is, without a selector.

**Non-Goals:**
- More event kinds, trips, remembering other choices, hiding vehicles.

## Decisions

1. **"Choose" is an empty vehicle id.** `LogDistanceNavKey(graph, vehicleId = "")` already exists as the default; an empty id now means the form starts from the Home screen and shows the selector, so no new key and no new serial key (a restored back stack keeps working); a non-empty id is the details route and behaves as now. The processor takes the same `@Assisted vehicleId`; `chooseVehicle = vehicleId.isEmpty()` is decided at construction and is not state.
2. **The chosen vehicle is state, and everything else follows it.** `LogDistanceState` gains `selectedVehicleId: String = ""` (saved: it survives rotation and process death) and `@Transient vehicles: List<VehicleChoice>` (id, name, type, color, small picture URI, plate: what the selector draws). The processor keeps the chosen id in a `MutableStateFlow` and observes
   `selected.flatMapLatest { id -> combine(observeVehicle(id), observeLog(id)) }`, so the known odometer, the log and the unit follow a change without a second code path; in the details route the flow is fixed to the id it was built with. The first choice, in order: the restored `selectedVehicleId` if that vehicle exists, else the remembered
   last-logged vehicle if it exists, else the first vehicle by name (the same case-insensitive order as the list, from one shared comparator so the two cannot drift).
3. **Changing the vehicle re-initialises only what belongs to the vehicle.** The unit is set from the new vehicle (its family and its remembered tenths choice) and the two entries change unit with `withUnit`, which already keeps the digits ("changing the unit keeps the number"); the way, date, time and zone are untouched; the error is cleared. A `unitFor: String` in the state (the id the unit was
   initialised for) replaces the meaning of `unitInitialized` (kept in the serialized form for states saved by the earlier build, read as "initialised for the chosen id"): the unit is initialised again whenever `unitFor` differs from the vehicle now observed.
4. **The memory is a key-value table of its own, written with the entry.** `app_state (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)` (migration `6.sqm`, schema version 7) with the key `last_logged_vehicle_id`; `SqlDelightVehicleRepository.addDistanceEntry` and `addOdometerAnchor` upsert it inside their existing transactions, so the entry and the memory both happen or neither does (a failed save leaves it as it was),
   and `observeLastLoggedVehicleId(): Flow<String?>` reads it. It is written for every logging route, including the details. **It is not derived from the events**: the events' dates are the user's (a backdated entry, an entry in another zone), while "the last one logged for" is about when the user acted, which no event column holds; that is the reason for storage of its own,
   and a test pins it (the backdated scenario). No backfill: a database without the row has no memory, and the selector falls back to the first vehicle by name.
   Alternatives: a `last_logged_at` column on `vehicle` (mixes a UI memory into the vehicle record and its `updated_at` rules; rejected), a single-purpose one-row table (works, but each later memory would need its own migration; a small key-value table costs nothing more now), platform preferences (DataStore or `NSUserDefaults`: two implementations behind an interface, not transactional with the entry, and not synced with the
   database when sync arrives; rejected).
5. **The selector is an `ExposedDropdownMenuBox`.** A read-only `OutlinedTextField` labelled "Vehicle" showing the chosen vehicle's name, with the vehicle's small picture or icon (`VehiclePicture`, 24 dp) as its leading icon and the dropdown arrow; the menu lists each vehicle with the same picture or icon, its name and, when it has one, its plate. Test tag `log_vehicle_selector` on the field; the menu is another
   window without tags, so a flow taps an option by its text. In the details route the selector is not composed at all. The form's title stays "Log distance".
6. **The Home tile.** `landingTiles` enables "Log event" when there is at least one vehicle (its name is "Log event" then, and "Log event, add a vehicle first" while disabled); a tap becomes the effect `ShowLogEvent`, which the landing nav key turns into `navigateTo(LogDistanceNavKey(graph, ""))`. The form's `onBack` and the saved effect pop to the Home screen because that is what is below it on the back stack.
7. **Order of the changes.** This change needs the Home screen's tile from `add-landing-screen`. Its spec adds requirements only (no MODIFIED of the landing change's requirement), so it can be validated and archived after the landing change with no merge in the app-shell spec.

## Risks / Trade-offs

- [The processor becomes stateful about a chosen vehicle] → the flow of the chosen id is the single source; the details route is the same code with the id fixed, and its existing tests keep passing unchanged.
- [A restored state saved before this change has no `selectedVehicleId`] → the field defaults to empty and is then chosen as in decision 2; the earlier build's saved forms always were for a fixed vehicle, which the details route still is.
- [Switching vehicles converts typed digits between units] → the existing, tested `withUnit`; the scenario states it.
- [The memory row could name a vehicle that no longer exists] → treated as nothing remembered; vehicles cannot be deleted today, and a later "hide" feature keeps them.

## Migration Plan

`6.sqm` creates the table (no rows). Schema version 7; the migration test moves a version-6 database (with vehicles and events) to 7 and checks the vehicles and log are unchanged and no memory exists. Rollback is a revert (an older app ignores the extra table).

## Open Questions

- The tile is "Log event" while the form is still "Log distance" (the only event kind today). Renaming the form's title to "Log event" now is possible; kept for the moment so the details route's wording does not change here.
