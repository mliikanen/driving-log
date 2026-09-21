# Design

## Context

Observed today: `App.kt` starts the Kide back stack at `VehicleListNavKey`; the list screen is a `Scaffold` with a centered "Driving Log" top app bar, an extended "Add vehicle" button (`add_vehicle`), the list (`vehicle_list`, `vehicle_item`) and an empty state (`empty_state`). Every Maestro flow starts with `launchApp` and then works on the
list (`assertVisible: No vehicles yet`, `tapOn: id: add_vehicle`), and the shared `subflows/add-vehicle.yaml` taps `add_vehicle` first and ends by waiting for the list. Screens are `ScreenNavKey`s with value-based equality, registered in `registerVehicleNavKeys` so Kide can restore them; the processors are built from `AppGraph`.
Vehicles cannot be deleted, so once the first vehicle exists the "no vehicles" state never comes back. The specs call the start screen "Home screen" (`app-shell`, `vehicles`).

## Goals / Non-Goals

**Goals:**
- A landing screen that is the app's start, with four tiles in a 2 x 2 grid, of which one works now and three are placeholders whose changes are separate.
- The vehicle list unchanged in content, moved behind the first tile.

**Non-Goals:**
- The log form's vehicle selector and the remembered vehicle (`add-direct-logging`), trips (`add-trip-logging`), a use for the fourth tile.
- Any storage or schema change.

## Decisions

1. **One processor, a state that decides the tiles.** `LandingProcessor(repository)` observes the vehicles and keeps `LandingState(isLoading, hasVehicles)`; the tiles are derived from it by a pure function (`landingTiles(hasVehicles, isLoading)`) giving each tile its id, icon, whether it is enabled and its **name** (the description a screen reader reads; the tiles have no visible text), so the rules (the first tile is not enabled until it is known whether there are vehicles; the placeholders are disabled)
   are unit tests and the screen only draws them. Intents `OpenVehicles` and `AddVehicle` lead to effects `ShowVehicles` and `ShowAddVehicle` that the nav key turns into `navigateTo`, as the list screen does. Nothing typed is kept, so nothing needs saving across rotation: the state is rebuilt from the repository, as for the list.
2. **The first tile looks the same whatever the vehicles are, and only its destination differs.** It is always the car icon with the tag `landing_vehicles`; its name is "Vehicles" or, with no vehicle, "Add vehicle" (the destination is chosen from `hasVehicles` when it is tapped: the list, or the add screen). While loading it is not enabled, so a tap cannot pick the wrong destination. The other tags are `landing_log_event`, `landing_trip` and `landing_more`.
3. **The grid: two rows of two tiles sized from the space.** `BoxWithConstraints` sizes a tile as half the width minus the gap (at most a set limit, at least 96 dp) and shrinks it in landscape so that two rows fit under the app bar; inside a `verticalScroll`, so a large font scale or a small window scrolls instead of clipping (the project rule that every screen that can outgrow the screen scrolls, `ScreenBottomSpace` at the end). Each tile is a Material 3 `Card` (clickable, `enabled` for the disabled look) with one icon of 64 dp centred in it and **no visible text**; the whole card is the tap target. The tile's name is the icon's own `contentDescription` (Compose's construct for it: the clickable card merges it into the tile's semantics), so a screen reader and the Maestro flows read it, and it is empty only while the first tile loads.
   Alternatives: `LazyVerticalGrid` (a lazy list for four fixed items, and its `GridCells.Fixed(2)` gives no control over the row height in landscape) and a `FlowRow` (wraps to one column when narrow, which breaks "2 x 2").
4. **Material 3 components and their defaults, not re-implemented behavior.** Each tile is a Material 3 `Card(onClick = ..., enabled = ...)`; the components supply the states, so **nothing here computes a disabled color, an alpha, a pressed state, a ripple or a semantic role by hand**:
   - **Available:** `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)`, the theme's *action* roles (the tile is not `surface`-coloured, so it reads as an action). Icon and label take the card's content color (`LocalContentColor`), so no color is passed to them.
   - **Disabled:** `enabled = false` and **no disabled colors passed**: Material's `CardDefaults` supply the disabled container and content colors (the theme's `onSurface` at 12% and 38%), so they follow the light and dark schemes and the Petroleum palette, and any change of Material's own definition of "disabled" arrives with the library. The app's theme has no separate disabled roles, and none are added.
   - **Semantics:** the component exposes a disabled tile as disabled to accessibility services and blocks clicks. The tile's name is the icon's `contentDescription`; the only extra semantics are the tile's test tag and, for "Log event" without a vehicle, a `stateDescription` (the Compose semantics property that exists for this) that says a vehicle must be added first (`add-direct-logging`).
   Tests, in the style of `ThemeColorsTest`: the available pair (`onPrimaryContainer` on `primaryContainer`) reaches 3:1 for the icon in both schemes (it has 4.5:1 in `ThemeColorsTest` already); a structural test that `landingTiles` marks exactly the three placeholders as not enabled. The look of the disabled tiles is checked by eye in both schemes (task 3.3). Disabled tiles are exempt from contrast, as Material Design and WCAG exempt inactive components.
   The same rule applies to the rest of the screen: the top app bar is the app's existing `CenterAlignedTopAppBar` with `drivingLogTopAppBarColors()` (as the vehicle list has), and the grid is plain layout (`Row`, `Column`, weights).
5. **Icons.** The vehicles tile uses the existing generic car icon whatever the state (no plus icon); "Log event" is a Phosphor `note-pencil-fill`, "Trip" `path-fill` and the fourth `question-fill`, each a single filled path on the 256 view box like `PhotoIcons`, in `LandingIcons`, with the SVGs in `docs/icons/phosphor` (MIT; already covered by `THIRD_PARTY_NOTICES.md`). A test builds each path and checks it is non-empty, like `PhotoIconsTest`.
6. **Navigation.** `LandingNavKey` (serial key `landing`, no arguments) is the start destination and is registered with the others. `VehicleListNavKey` takes an `onBack`, shows a back arrow (the shared `BackButton`) and the title "Vehicles"; back pops to the landing screen. After "Add vehicle" from the landing screen saves the first vehicle the add screen pops back to the landing screen, whose first tile has by then become "Vehicles", because the state follows the repository.
   A back stack saved by the previous version of the app (the list as the only entry) still restores: the list is then the root and back leaves the app; nothing is migrated.
7. **The Maestro flows.** They all begin on the list today. One shared step gets there: `subflows/add-vehicle.yaml` starts on the landing screen, taps `landing_vehicles` and, if the list (not the add screen) is open, taps `add_vehicle`; it ends on the vehicle list, as before, so the assertions after it stay. A flow that asserts "No vehicles yet" first taps `landing_vehicles` (which is "Add vehicle" then): the flows that start with the empty state are updated one by one (the task lists them).
   One new flow (`vehicles/landing`) follows the happy path: the four tiles, the vehicles tile to the list and back.

## Risks / Trade-offs

- [Every flow's first steps change] → the shared subflow absorbs most; the flows that assume the list at start are listed in the tasks and edited; because the shared subflow is touched, all four plain manifests are run once (a consequence of the change, not a whole-suite regression run: `docs/test-strategy.md`).
- [Three disabled tiles look unfinished] → deliberate and temporary: they hold the places of `add-direct-logging`, `add-trip-logging` and the fourth action; Material's disabled state tells them from the available tile, by color and for a screen reader.
- [The landing screen's tile sizing in landscape on a small phone] → checked on the emulator in both orientations (the tasks).
- [A restored old back stack has no landing screen] → accepted (see decision 6).

## Open Questions

- What the fourth tile becomes is undecided ("Placeholder" is its name until then, with a question mark icon); a settings entry or reports are candidates, and the icon and name change with it.
