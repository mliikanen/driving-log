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
- A fixed, small set of vehicle types with an icon each, preselected as Car on add, editable later, and never clearable once set.
- The placeholder icon follows the type everywhere a vehicle without a picture is drawn.
- Every vehicle always has a type, enforced by the data model (a `NOT NULL` column and non-null types in the code); existing vehicles are given Car by the migration.
- Icons from one consistent, permissively licensed set, with the license kept in the repository.

**Non-Goals:**
- Type-specific behavior (fuel, units, defaults), custom or user-added types, localized names, a type line on the details screen, pickups and motorhomes (below).

## Decisions

### 1. The set of types: eight, with stable codes

| Code | Name | Icon (Phosphor `*-fill`) | Why |
|---|---|---|---|
| `CAR` | Car | `car` | the common case; also the type existing vehicles are given |
| `SUV` | SUV | `jeep` | distinct silhouette from a car, common in registries |
| `VAN` | Van | `van` | vans and minibuses |
| `TRUCK` | Truck | `truck` | lorries and delivery trucks |
| `BUS` | Bus | `bus` | buses and coaches |
| `MOTORCYCLE` | Motorcycle | `motorcycle` | |
| `SCOOTER` | Scooter | `scooter` | mopeds and scooters, a different odometer world from motorcycles |
| `OTHER` | Other | `steering-wheel` | everything else, so the choice never forces a wrong answer |

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

### 3. Data: a `NOT NULL` `vehicle_type` column, default Car

Migration `4.sqm` (the next number at apply time, giving schema version 5): `ALTER TABLE vehicle ADD COLUMN vehicle_type TEXT NOT NULL DEFAULT 'CAR';` SQLite adds a `NOT NULL` column with a default and gives every existing row
that default, so **every existing vehicle becomes a Car** in the migration, which is how those vehicles were drawn before (the car icon); nothing else is needed to backfill. A fresh database declares the same column (`TEXT NOT NULL DEFAULT 'CAR'`),
so a migrated and a fresh database are identical. **The column cannot hold null, so a vehicle cannot exist without a type.** `enum class VehicleType(val code: String, val label: String)` with `fromCode(code): VehicleType?`: the
repository maps what it reads with `VehicleType.fromCode(code) ?: VehicleType.OTHER`, so a code this app does not know (written by a newer version) reads as **Other**, a valid type, and never crashes and never yields "no type". `Vehicle.type: VehicleType` is non-null.
The queries that select a vehicle carry the column; the code is written as `type.code`. There is deliberately no `CHECK` on the codes: adding a type later is an enum entry and an icon, not a migration.

### 4. Repository

`addVehicle(name, plate, type: VehicleType, unit, initialOdometer, picture)` and `updateVehicle(id, name, plate, type: VehicleType, picture)` both take a **non-null** type. The add state starts with `CAR`, so a save always has a type; below it, the repository signatures and the `NOT NULL` column make it impossible to save a
new vehicle, or an edit, without a type. There is no way to clear a type: there is no "none" tile in the form and no nullable parameter. An edit always writes the type it was given, inside the transactions the earlier changes already use. A test asserts the log is unchanged by a type edit.

### 5. The icon lookup

`object VehicleIcons` holds one lazily built `ImageVector` per type and `fun of(type: VehicleType): ImageVector`. The generic `VehicleIcons.Car` of the picture change stays as the `CAR` entry. A test checks the lookup is **total** (every `VehicleType` yields an icon), that the
eight icons are pairwise different (compared by their path data), and that each builds with a non-empty path. `VehiclePicture(uri, type, modifier)` gains the type: for a null URI (or while loading and on error) it draws the icon of the type in the same tile, with the accessibility label `Vehicle type: <label>`
(`contentDescription`, which Maestro and TalkBack read). Its `type` parameter is nullable for one reason only: the edit form has no type until the saved vehicle has loaded, and then shows the generic car icon (labelled by the form as "Change picture"/"Add picture"); every other place passes a non-null type.

### 6. The state and the choice

The add state gains `type: VehicleType = CAR` (serializable, never null: Car is preselected, which is how a vehicle always has a type when saved); the edit state has `type: VehicleType?` that is null only until the saved vehicle has loaded, and is always a type afterwards. Intents: `TypeSelected(type)`, which replaces the choice. On add, `Save` validates name and odometer as it does today and passes the chosen type on. On edit the state starts with the saved type, `TypeSelected` replaces it, and saving passes it on. The view states of the list and the details screen carry `type: VehicleType` (non-null) next to their `pictureUri`.

`VehicleTypeChoice(selected, onSelect)` is one composable used by both forms: a `FlowRow` of selectable tiles (a 56 dp icon over the name, `selectable` with radio semantics, the selected tile with a `primaryContainer` fill and a check) . Test tags `vehicle_type_<CODE>` (`vehicle_type_VAN`) and the choice's `vehicle_type_choice`. Eight tiles wrap to
two rows on a phone and one row in landscape; the form scrolls as it does today.

### 7. Where the placeholder shows

The list rows, the details header and the form previews already draw through `VehiclePicture`; they pass the vehicle's type. On the add form the preview follows the current choice live (the car at first). Because a picture wins, nothing changes for vehicles that have one.

### 8. Testing

- Enum: codes are stable (a test that pins each code string), `fromCode` round trips every entry, an unknown or empty code gives null, the order and the names are as specified.
- Migration: a JVM test from every previous version to the new one keeps the vehicle and gives it the type Car; a fresh database has the same `NOT NULL` column; inserting a vehicle with a null type fails at the database.
- Repository on real SQL: add with each type reads it back, edit changes it, an unknown stored code reads as Other, the log is unchanged by a type edit, and the column rejects null.
- Processors (kide-test): add starts with Car and saves it when the choice is untouched, choosing replaces it, a rotation keeps the choice (state restore), edit starts with the saved type, changing it saves it, saving an edit without touching the type keeps it, the view states carry the type.
- Icons: the lookup is total, distinct and non-empty (above).
- Maestro (Android): create one vehicle of each of the eight types and assert the list item's `Vehicle type: <label>` description, changing a type on the edit screen and seeing the list icon change, a picture winning over the icon, and rotation with a chosen type. A migrated vehicle becoming a Car is
  covered by the migration tests (a Maestro flow cannot create one).

## Risks / Trade-offs

- **Two icon gaps (pickup, motorhome)** are left to users' judgment (Truck/SUV, Other) until a matching icon is found or drawn; the design records the candidates so the follow-up is a quick decision.
- **Existing vehicles all become Car.** Some are not cars, and the migration cannot know; it is the value they were drawn with, and the user can change it on the edit screen (a vehicle is not "unset", so nothing nags). The alternative, leaving them unset, was rejected: every vehicle must have a type.
- **Icon legibility at 56 dp.** Phosphor's fill glyphs are designed to read at 24 dp and up, so 56 dp is comfortable; the placeholder tile keeps the same contrast tokens as the car icon already does.
- **Car is a default the user may not notice.** A user who adds a motorcycle and never looks at the choice gets a Car icon. The choice is a visible row of tiles with Car marked selected, the type is editable at any time, and the alternative (nothing preselected, a required error) was rejected because the request is that Car is selected by default.
- **The names are English.** Consistent with the rest of the app today; the whole app's localization is a separate change and the codes make it safe.
