# Proposal

> **Split in three.** The request behind this change was one idea (a new landing page with four actions, logging from it, and trips); it is planned as three changes so each can be built and checked alone:
> this one (**the landing screen and its four tiles**), `add-direct-logging` (the "Log event" tile made to work: a vehicle selector and the remembered last vehicle) and `add-trip-logging` (a stub for the "Trip" tile).
> In this change the "Log event", "Trip" and fourth tiles are present but not yet usable.

## Why

The app opens on the vehicle list, which makes "look at my vehicles" the only thing the app is about. The next features (logging without first opening a vehicle, trips) need a place to start from. A **landing screen** with the app's main
actions gives them that place, keeps the vehicle list one tap away, and makes the first thing a new user sees an invitation to add a vehicle instead of an empty list.

## What Changes

- **A new Home screen: a 2 x 2 grid of four actions**, each **an icon and no visible text** (every tile has a name for screen readers), in a top app bar titled "Driving Log":
  1. **Vehicles** (always the **car icon**, whether or not there are vehicles): opens the vehicle list; when the user has **no vehicles** it opens the add vehicle screen directly. Only the destination depends on the vehicles; its name for a screen reader says which ("Vehicles" or "Add vehicle").
  2. **Log event** (a pencil-and-note icon): present, **not usable yet** (shown in the theme's disabled colors, not in the action colors); `add-direct-logging` makes it open the log form.
  3. **Trip** (a route icon): present, **not usable yet** (in the disabled colors); it becomes start and end trip in `add-trip-logging`.
  4. **Placeholder** (a question mark icon): holds the place of a fourth action, in the disabled colors; the three disabled tiles **stay disabled** until the changes that build them are applied.
- **The app starts on the landing screen** instead of the vehicle list. The **vehicle list becomes a screen of its own**, reached from the first tile: it keeps its list, its "Add vehicle" button and its empty state, gains a back arrow (back returns to the landing screen) and is titled "Vehicles".
- The grid stays 2 x 2 in portrait and in landscape; it survives rotation; tiles are at least 48 dp. **The tiles are Material 3 `Card`s used as they are meant to be**: an available tile takes the action colors of the theme (primary container with its on-color) and a disabled one is a `Card` with `enabled = false`, so Material's own disabled colors, semantics (it is announced as disabled) and input handling apply, with no color, alpha or state re-implemented here; the two states cannot be mistaken for each other, the available tiles' text and icons meet the app's contrast rules, and the disabled ones are exempt, as Material Design exempts inactive components.
- Three new icons (note-pencil, route, question) from the Phosphor set already used for the vehicle icons (MIT, in `THIRD_PARTY_NOTICES.md`); the car icon is the existing vehicle car.
- The Maestro flows start from the landing screen: the shared `add-vehicle` steps get to the list (or straight to the add screen when there are no vehicles) from the landing tile, and a new flow follows the landing screen's happy path.

Out of scope: the log form and the remembered vehicle (`add-direct-logging`), trips (`add-trip-logging`), what the fourth tile will be, a per-vehicle theme for the landing screen (`add-vehicle-color-theme` and its follow-ups), a settings or account entry, and iOS on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities

None. The Home screen and the vehicle list are already specified; this change modifies them.

### Modified Capabilities

- `app-shell`: "Home screen lists vehicles" is removed and replaced by "Home screen offers the main actions" (the grid, the tiles, the empty-state variant of the first tile); the launch and rotation requirements already say "Home screen" and now mean the landing screen.
- `vehicles`: "Vehicle list" describes the list as a screen of its own, reached from the Home screen, instead of the Home screen's content.

## Impact

- `shared/` commonMain: `LandingProcessor`, `LandingContract`, `LandingScreen` (grid, tiles, disabled state), `LandingIcons`; `LandingNavKey` registered and made the start destination in `App.kt` and `VehicleNavKeys.kt`; `VehicleListScreen` (title, back arrow); the tile-to-screen navigation.
- Tests: `LandingProcessorTest` (state from the vehicle count, loading, intents to effects), icon path tests like `PhotoIconsTest`.
- `docs/icons/phosphor` (four SVGs), `THIRD_PARTY_NOTICES.md` (no new licence: Phosphor is already listed).
- `maestro/`: `subflows/add-vehicle.yaml` and every flow's start (they all begin on the list today), a new `vehicles/landing.yaml`; per `docs/test-strategy.md` the manifests to run are the ones the shared subflow touches (all four), see the design.
- No storage or schema change, no new dependency, no permission.
