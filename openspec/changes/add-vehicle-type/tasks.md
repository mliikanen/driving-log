# Tasks

## 1. Ground and data

- [x] 1.1 Check the ordering and update the one requirement this change depends on: confirm `add-vehicle-picture` is archived (and `add-vehicle-palette` if it was applied first), re-read the post-picture and post-palette text of "Add a vehicle" and "Edit a vehicle" in `openspec/specs/vehicles/spec.md` and refresh the MODIFIED blocks here if they drifted, and add a MODIFIED "A vehicle can have one optional picture" to a `specs/vehicle-picture/spec.md` delta whose placeholder sentence says the icon follows the vehicle's type (the car icon when there is none) instead of being the same for every vehicle; verify `openspec validate add-vehicle-type --strict` passes
- [x] 1.2 Add the eight Phosphor `fill` SVGs (`car`, `jeep`, `van`, `truck`, `bus`, `motorcycle`, `scooter`, `steering-wheel`) to `docs/icons/phosphor/` with the MIT `LICENSE`, and the Phosphor entry in `THIRD_PARTY_NOTICES.md` (copyright, license, the icons used); verify with a check that every SVG is one `<path>` on a `0 0 256 256` view box and that the license text and the notice are present
- [x] 1.3 Add `VehicleType` (codes `CAR`, `SUV`, `VAN`, `TRUCK`, `BUS`, `MOTORCYCLE`, `SCOOTER`, `OTHER`, English labels, the specified order, `fromCode` returning null for an unknown or empty code), and verify unit tests that pin every code and label, the order, the round trip of every entry, and null for unknown codes
- [x] 1.4 Add the migration for the `vehicle.vehicle_type` column (`TEXT NOT NULL DEFAULT 'CAR'`, the next number and schema version, the same declaration in the fresh schema), `Vehicle.type` (non-null, an unknown stored code reading as Other) and the queries that select and write it, and verify the JVM migration test from every previous version (the vehicle is intact and has the type Car), that a fresh database has the same column, and that the database rejects a vehicle with a null type
- [x] 1.5 Give `addVehicle` and `updateVehicle` a required non-null `type`, updating the fake repository, and verify repository tests on real SQL: each type is written and read back, an edit changes it, a stored unknown code reads as Other, the log is unchanged by a type edit, and a failed save changes nothing

## 2. Form logic

- [x] 2.1 Add the type to the add processor and state (`type` starting as Car and never null, `TypeSelected`), and verify processor tests: the state starts with Car, saving without touching the choice saves a Car, changing the choice replaces it, the chosen type reaches the repository, and a restored state keeps the choice
- [x] 2.2 Add the type to the edit processor and state (starts with the saved type, `TypeSelected`, saving passes it on and never saves without one), and verify processor tests: the saved type is selected, changing it saves it, leaving without saving keeps it, an edit that does not touch the type keeps it, and a vehicle migrated to Car can be changed
- [x] 2.3 Carry the vehicle's `type` in the view states of the vehicle list, the details screen and the add and edit forms next to the picture URI, and verify processor tests that each state has the vehicle's type and follows a change

## 3. Screens

- [x] 3.1 Add `VehicleIcons` (one `ImageVector` per type from the Phosphor path data, `of(type)` total over the types), and verify tests that the lookup is total, the eight icons are pairwise different by path data and every path is non-empty
- [x] 3.2 Make `VehiclePicture` take the type and draw its icon as the placeholder (for no picture, while loading and on error) with the label `Vehicle type: <name>` (the add form follows the choice, Car at first), use it with the type from the view states in the list rows, the details header and the form previews, and verify it compiles for Android and iOS and a picture still wins over the icon
- [x] 3.3 Add the `VehicleTypeChoice` composable (wrapping selectable tiles with icon and name, the selected tile marked, the error text, the test tags from the design) and use it on the add and edit screens, and verify it compiles for Android and iOS
- [x] 3.4 Check the type choice and the placeholders by hand on the emulator (add, edit, list, details) in light and dark mode, in landscape and with the keyboard open, and fix what looks wrong

## 4. Maestro flows

- [x] 4.1 Update `maestro/subflows/add-vehicle.yaml` to choose a type (a `TYPE` environment variable, default Car) and every flow that adds a vehicle, and verify the whole existing suite still passes
- [x] 4.2 Add flows for creating a vehicle of each of the eight types (the list item's `Vehicle type: <name>` description), Car being selected when the add screen opens and a vehicle saved without touching the choice being a Car, changing a type on the edit screen and the list icon changing, a picture winning over the icon and the icon returning when the picture is removed, the type surviving a restart, and a rotation with a chosen type, and verify they pass

## 5. Project context and final verification

- [x] 5.1 Add to the project context in `openspec/config.yaml` that a vehicle has a type from a fixed set (stored as a code in a `NOT NULL` column, Car preselected when adding, editable, and Car for vehicles that existed before) and that placeholders follow it, and verify `openspec validate --all --strict` passes
- [x] 5.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug`, the whole Maestro suite and `openspec validate --all --strict`, and verify all pass
