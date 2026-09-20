# Design

## Context

- After `add-vehicle-picture` a vehicle is a row in `vehicle` (`id`, `name`, `license_plate`, `odometer_unit`, `created_at`, `updated_at`,
  `log_distance_tenths`, `picture_id`; schema version 4), and after `add-vehicle-palette` it also has `main_color` and `palette` (version 5).
  This change is planned for after both, in the order picture, palette, type; if palette is not applied first, only the version numbers below shift.
- The picture change draws every vehicle without a picture with one **generic car icon** (`VehicleIcons.Car`, an `ImageVector` built from the
  path data of Phosphor's `car-fill`, its decision 12): the composable is `VehiclePicture(uri, modifier)`, and the view states of the list, the
  details screen and the forms carry a `pictureUri: String?` for it. The `AddVehicleState` and `EditVehicleState` are serializable Kide states.
- Project rules that apply: stored data is locale-agnostic (enum codes); Android is primary; business logic and its tests live in commonMain; every
  behavior change goes through `openspec/changes/`.

## Goals / Non-Goals

**Goals:**
- A fixed, small set of vehicle types with an icon each, required on add, editable later, and never clearable once set.
- The placeholder icon follows the type everywhere a vehicle without a picture is drawn.
- Existing vehicles keep working without a type; nothing is forced on them.
- Icons from one consistent, permissively licensed set, with the license kept in the repository.

**Non-Goals:**
- Type-specific behavior (fuel, units, defaults), custom or user-added types, localized names, a type line on the details screen, pickups and motorhomes (below).

## Decisions

### 1. The set of types: eight, with stable codes

| Code | Name | Icon (Phosphor `*-fill`) | Why |
|---|---|---|---|
| `CAR` | Car | `car` | the common case; also what a vehicle without a type shows |
| `SUV` | SUV | `jeep` | distinct silhouette from a car, common in registries |
| `VAN` | Van | `van` | vans and minibuses |
| `TRUCK` | Truck | `truck` | lorries and delivery trucks |
| `BUS` | Bus | `bus` | buses and coaches |
| `MOTORCYCLE` | Motorcycle | `motorcycle` | |
| `SCOOTER` | Scooter | `scooter` | mopeds and scooters, a different odometer world from motorcycles |
| `OTHER` | Other | `steering-wheel` | everything else, so the required choice never forces a wrong answer |

The order above is the order in the choice. The **code** is what is stored; it never changes once released, and adding a type later is adding an enum entry, an icon and a row in this table (no migration). The names are English strings on the enum for now (localizing is a later,
app-wide change). Why not more: every extra type is another tile on a small screen and another icon to keep consistent; eight fit in one row-wrapped choice on a phone. **Considered and left out:**
- *Pickup truck.* Phosphor has none (`pickup-truck-fill` is a 404 on its repository), and lookups by name in Tabler, Lucide, Bootstrap Icons and Material Icons found none either (not an exhaustive search of their catalogues), so a pickup owner picks **Truck** or **SUV** for now.
- *Motorhome/caravan.* Phosphor has none (`caravan`, `rv`, `motorhome` are 404s). Permissively licensed candidates elsewhere: Tabler `caravan` (MIT) and Lucide `caravan` (ISC), both stroked outline icons that would not match Phosphor's filled style, and Material Icons `rv_hookup` (Apache-2.0, filled, 24 px grid). Adding
  `MOTORHOME` later with the Material glyph is possible (one more license entry) but the set would mix two styles, so it is left to a follow-up decision.
- *Tractor, taxi, ambulance, police car:* Phosphor has them; they are special uses of the types above or off-road, not what a mileage log needs.

### 2. Icons: one set, MIT, one path each

**Phosphor Icons** (github.com/phosphor-icons/core, **MIT**, (c) 2023 Phosphor Icons) is the source, in its `fill` weight: for all eight glyphs (`car`, `jeep`, `van`, `truck`, `bus`, `motorcycle`, `scooter`, `steering-wheel`) the raw file exists and is a **single `<path>` on a `0 0 256 256` view box** with `fill="currentColor"`, so each becomes
an `ImageVector` from its path data the same way the generic car icon already does (verified by fetching all eight). One set means one visual weight and one license notice. **CC0 alternatives were looked at and rejected:** FreeSVG and SVG Silh are CC0 but a heap of unrelated styles with per-file provenance to verify; Simple Icons is CC0 but is brand logos, not vehicle types.
What the other permissive sets have, by name lookup on their repositories: Tabler (MIT) has car, car-suv, truck, bus, caravan, motorbike, scooter and tractor but no van, and Lucide (ISC) has car, truck, bus, van, caravan, bike and tractor but no motorcycle or scooter; both are stroked outlines, so either would mean a different visual style and,
for Tabler, a gap at Van. Material Icons (Apache-2.0) has `directions_car`, `local_shipping`, `two_wheeler`, `directions_bus` and `rv_hookup` as filled glyphs, but the van (`airport_shuttle`) was not found at the expected path and there is no SUV; mixing it in would also need Apache-2.0 notices.

Kept in the repository: the eight original SVGs in `docs/icons/phosphor/` (`car-fill.svg` is already there from the picture change), the license text `docs/icons/phosphor/LICENSE`, and one entry in `THIRD_PARTY_NOTICES.md` naming Phosphor, its copyright and license and the icons used, which is what the MIT license asks for. If a later icon comes from another set (a motorhome, say),
that set's license text and notice are added the same way.

### 3. Data: one nullable `vehicle_type` column

Migration `N.sqm` (the next number at apply time): `ALTER TABLE vehicle ADD COLUMN vehicle_type TEXT;` Null means "no type". `enum class VehicleType(val code: String, val label: String)` with `fromCode(code): VehicleType?` returning **null for an unknown code**, so a database written by a newer app degrades to "no type" and never crashes. `Vehicle.type: VehicleType?`. The queries that select a
vehicle carry the column; the code is written as `type?.code`. No default and no backfill: existing rows stay null, which is the specified behavior for existing vehicles.

### 4. Repository

`addVehicle(name, plate, type: VehicleType, unit, initialOdometer, picture)` takes a **non-null** type: the required-type rule lives in the add processor, and the type system makes the repository unable to save a new vehicle without one. `updateVehicle(id, name, plate, type: VehicleType?, picture)` writes the given type, which may be null only because an existing vehicle
without a type can be saved unchanged; the edit processor never sets a type back to null once the state has one (there is no "none" tile). Both run inside the transactions the earlier changes already use. A test asserts the log is unchanged by a type edit.

### 5. The icon lookup

`object VehicleIcons` holds one lazily built `ImageVector` per type and `fun of(type: VehicleType?): ImageVector` that returns the car icon for null. The generic `VehicleIcons.Car` of the picture change stays as the `CAR` entry and the null entry, so nothing that used it changes. A test checks the lookup is **total** (every `VehicleType`, and null, yields an icon), that the
eight icons are pairwise different (compared by their path data), and that each builds with a non-empty path. `VehiclePicture(uri, type, modifier)` gains the type: for a null URI (or while loading and on error) it draws the icon of the type in the same tile, with the accessibility label `Vehicle type: <label>` or `Vehicle type: none`
(`contentDescription`, which Maestro and TalkBack read), and a `null` type draws the car.

### 6. The state and the choice

The add and edit states gain `type: VehicleType?` (serializable) and, on the add state, `typeError: Boolean`. Intents: `TypeSelected(type)`. On add, `Save` validates name, **type** and odometer together (all errors show at once, like name and odometer today); a missing type sets `typeError`, `TypeSelected` clears it. On edit the state starts with the saved type
(possibly null), `TypeSelected` replaces it, and saving passes it on; a null stays null (no error), which is how a legacy vehicle's unrelated edit is never blocked. The view states of the list, the details screen and the forms carry `vehicleType: VehicleType?` next to their `pictureUri`.

`VehicleTypeChoice(selected, onSelect, error)` is one composable used by both forms: a `FlowRow` of selectable tiles (a 56 dp icon over the name, `selectable` with radio semantics, the selected tile with a `primaryContainer` fill and a check), the error text under it in the error color. Test tags `vehicle_type_<CODE>` (`vehicle_type_VAN`), `vehicle_type_error`, and the choice's `vehicle_type_choice`. Eight tiles wrap to
two rows on a phone and one row in landscape; the form scrolls as it does today.

### 7. Where the placeholder shows

The list rows, the details header and the form previews already draw through `VehiclePicture`; they pass the vehicle's type. On the add form the preview follows the current choice live (before a type is chosen it shows the generic car). Because a picture wins, nothing changes for vehicles that have one.

### 8. Testing

- Enum: codes are stable (a test that pins each code string), `fromCode` round trips every entry, an unknown or empty code gives null, the order and the names are as specified.
- Migration: a JVM test from the previous version to the new one keeps the vehicle and gives it a null type; a fresh database has the column.
- Repository on real SQL: add with each type reads it back, edit changes it, edit with null leaves null, an unknown stored code reads as null, the log is unchanged by a type edit.
- Processors (kide-test): add requires a type (error, no repository call), the error clears on selection, a rotation keeps the choice (state restore), edit starts with the saved type or none, changing it saves it, saving a legacy vehicle without choosing keeps null, the view states carry the type.
- Icons: the lookup is total, distinct and non-empty (above).
- Maestro (Android): create one vehicle of each of the eight types and assert the list item's `Vehicle type: <label>` description, the required-type error, changing a type on the edit screen and seeing the list icon change, a picture winning over the icon, and rotation with a chosen type. A legacy vehicle without a type is
  covered by unit and migration tests (a Maestro flow cannot create one).

## Risks / Trade-offs

- **Two icon gaps (pickup, motorhome)** are left to users' judgment (Truck/SUV, Other) until a matching icon is found or drawn; the design records the candidates so the follow-up is a quick decision.
- **A vehicle without a type looks like a Car.** Deliberate (that is what every vehicle looked like before) but a user cannot tell "Car" from "not chosen" in the list; the edit screen shows the difference (nothing selected).
- **Icon legibility at 56 dp.** Phosphor's fill glyphs are designed to read at 24 dp and up, so 56 dp is comfortable; the placeholder tile keeps the same contrast tokens as the car icon already does.
- **A required field on add adds a step.** One tap, on visible tiles, with no default to correct; the alternative (preselecting Car) was rejected because the request is that the user choose.
- **The names are English.** Consistent with the rest of the app today; the whole app's localization is a separate change and the codes make it safe.
