# Proposal

> **Ordering.** This change is applied **after `add-vehicle-picture`** (which gives vehicles a picture and a single generic car icon as
> the placeholder) and after `add-vehicle-palette` if that one is applied first: the order assumed is picture, then palette, then
> type. The MODIFIED requirement texts in `specs/vehicles/spec.md` are written against the text those two changes leave behind, and
> the schema version and migration number are whatever is next at apply time (picture makes it version 4, palette proposes 5).
> When this change is applied, `add-vehicle-picture` must already be archived: one requirement of the `vehicle-picture` capability
> ("A vehicle can have one optional picture", which says the placeholder is the same car icon "until vehicles have a type") then
> needs a one-line update, which cannot be written as a delta before that capability exists (see task 1.1).

## Why

A vehicle without a picture is drawn as one generic car icon, which is wrong for a motorcycle or a van and makes a list of
different vehicles look alike. A **vehicle type** lets the app draw an icon that says what kind of vehicle it is, right in the
list, without the user having to take and crop a photo. It is also the first piece of knowledge about a vehicle beyond its name and
plate, which later features (fuel and consumption defaults, vehicle matching) can build on.

## What Changes

- Every vehicle has a **type**, chosen from a fixed set of **eight**: **Car, SUV, Van, Truck, Bus, Motorcycle, Scooter, Other**. Each has its own icon.
- The type is **required when a vehicle is added**: the add screen shows the types as selectable icon tiles with labels, none preselected,
  and saving without a choice shows the error "Choose the vehicle type" and saves nothing.
- The type **can be changed later** on the edit screen. It cannot be cleared: once a vehicle has a type it always has one.
- **Vehicles that existed before this change have no type.** They keep showing the generic car icon, the edit screen offers the type
  choice with nothing selected, and saving the edit does **not** force a choice, so unrelated edits are never blocked.
- The **placeholder follows the type**: where a vehicle without a picture is drawn (the vehicle list, the details screen and the add
  and edit form previews), it shows the icon of its type; a vehicle without a type shows the generic car icon. A picture, when the
  vehicle has one, still wins over the icon. The icon carries the accessibility label "Vehicle type: Van" (and so on).
- The icons are **MIT-licensed glyphs from the Phosphor icon set**, one consistent set of single-path filled icons; their SVG files
  and the license are kept in the repository (`docs/icons/phosphor/`) and named in `THIRD_PARTY_NOTICES.md`. They are drawn in the
  app from their path data, as the generic car icon already is.
- Storage: one nullable `vehicle_type` column on the vehicle holding the type's **code** (`CAR`, `SUV`, ...; locale-agnostic), one
  migration. An unknown code (from a newer app version) is read as "no type" and never crashes.
- Type names are English strings for now, like the rest of the app; localizing them is not part of this change.
- Out of scope: pickup trucks and motorhomes (no permissively licensed icon that matches the set was found, see the design), custom
  types, anything that behaves differently by type (fuel, odometer or unit defaults, consumption), showing the type as text on the
  details screen, and iOS verification on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities
- `vehicle-type`: the fixed set of types and their icons, choosing the type when adding and changing it later, vehicles without a
  type, the placeholder icon following the type, and how the type is stored.

### Modified Capabilities
- `vehicles`: adding a vehicle requires choosing its type, and the edit screen lets the user change it.

## Impact

- `shared/` commonMain: a `VehicleType` enum with stable codes and English labels; `Vehicle.type: VehicleType?`; a `VehicleIcons`
  object mapping each type (and no type) to an `ImageVector` built from Phosphor path data; a `VehicleTypeChoice` composable
  (selectable tiles); the add and edit processors and states (`type`, `typeError`, a `TypeSelected` intent, the required-type
  validation on add); the placeholder in `VehiclePicture` takes the type; the list, details and form view states carry it;
  `SqlDelightVehicleRepository` writes and reads `vehicle_type`; one migration.
- Repository: `addVehicle` takes the type (required), `updateVehicle` takes the (nullable) type.
- Files: eight SVGs, the Phosphor MIT license text and `THIRD_PARTY_NOTICES.md` entries; `maestro/` flows for creating a vehicle of each
  type, the required-type error, and changing the type.
- No new dependency.
- `openspec/config.yaml`: the project context notes that a vehicle has a type from a fixed set and that placeholders follow it.
