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

1. **One processor, a state that decides the tiles.** `LandingProcessor(repository)` observes the vehicles and keeps `LandingState(isLoading, hasVehicles)`; the tiles are derived from it by a pure function (`landingTiles(hasVehicles, isLoading)`) giving each tile its id, label, icon, whether it is enabled and its accessibility name, so the rules ("Add vehicle" only when it is known that there are none; the placeholders are disabled)
   are unit tests and the screen only draws them. Intents `OpenVehicles` and `AddVehicle` lead to effects `ShowVehicles` and `ShowAddVehicle` that the nav key turns into `navigateTo`, as the list screen does. Nothing typed is kept, so nothing needs saving across rotation: the state is rebuilt from the repository, as for the list.
2. **The first tile has one identity in both variants.** Test tag `landing_vehicles` whatever it says ("Vehicles" or "Add vehicle"), so a flow can tap the tile without knowing which; while loading it is shown without a label and is not tappable, so the empty variant never flashes at a user who has vehicles. The other tags are `landing_log_event`, `landing_trip` and `landing_more`.
3. **The grid: two rows of two tiles sized from the space.** `BoxWithConstraints` sizes a tile as half the width minus the gap (at most a set limit, at least 96 dp) and shrinks it in landscape so that two rows fit under the app bar; inside a `verticalScroll`, so a large font scale or a small window scrolls instead of clipping (the project rule that every screen that can outgrow the screen scrolls, `ScreenBottomSpace` at the end). Each tile is a Material 3 `Card` (clickable, `enabled` for the disabled look) with an icon of 48 dp over a one-line label; the whole card is the tap target.
   Alternatives: `LazyVerticalGrid` (a lazy list for four fixed items, and its `GridCells.Fixed(2)` gives no control over the row height in landscape) and a `FlowRow` (wraps to one column when narrow, which breaks "2 x 2").
4. **Colors from the theme only, by Material 3's roles: action colors when available, disabled colors when not.** An available tile is a Material 3 `Card` with the *action* roles: container `primaryContainer`, icon and label `onPrimaryContainer` (the same pair the app's other prominent actions draw from; the tile is not `surface`-coloured, so it reads as an action).
   A disabled tile takes Material 3's **disabled treatment, which is not a dimmed primary**: container `onSurface` at 12% alpha and content (icon and label) `onSurface` at 38% alpha, as `CardDefaults.cardColors` gives (`disabledContainerColor`, `disabledContentColor`) and as the specification of disabled components in Material Design says; both derive from the theme's `onSurface`, so they follow the light and dark schemes
   and the Petroleum palette with no color of their own. The app's theme defines no separate "disabled" roles because Material 3 has none: disabled is `onSurface` with those two alphas. The tiles are given these colors explicitly (`CardDefaults.cardColors(containerColor = primaryContainer, contentColor = onPrimaryContainer, disabledContainerColor = onSurface 12%, disabledContentColor = onSurface 38%)`)
   in one place, `LandingTileColors`, so a check can read them. Tests in the style of `ThemeColorsTest`: the available pair reaches 4.5:1 for the label and 3:1 for the icon in both schemes, and a disabled tile's container and content differ from the available ones (the two states are told apart by color as well as by the label). Disabled tiles are exempt from contrast (Material Design and WCAG exempt inactive components) but keep a readable name for a screen reader ("Log event, not available yet").
5. **Icons.** The vehicles tile uses the existing generic car icon; "Add vehicle" a Phosphor `plus-circle-fill`, "Log event" `note-pencil-fill`, "Trip" `path-fill` and the fourth `question-fill`, each a single filled path on the 256 view box like `PhotoIcons`, in `LandingIcons`, with the SVGs in `docs/icons/phosphor` (MIT; already covered by `THIRD_PARTY_NOTICES.md`). A test builds each path and checks it is non-empty, like `PhotoIconsTest`.
6. **Navigation.** `LandingNavKey` (serial key `landing`, no arguments) is the start destination and is registered with the others. `VehicleListNavKey` takes an `onBack`, shows a back arrow (the shared `BackButton`) and the title "Vehicles"; back pops to the landing screen. After "Add vehicle" from the landing screen saves the first vehicle the add screen pops back to the landing screen, whose first tile has by then become "Vehicles", because the state follows the repository.
   A back stack saved by the previous version of the app (the list as the only entry) still restores: the list is then the root and back leaves the app; nothing is migrated.
7. **The Maestro flows.** They all begin on the list today. One shared step gets there: `subflows/add-vehicle.yaml` starts on the landing screen, taps `landing_vehicles` and, if the list (not the add screen) is open, taps `add_vehicle`; it ends on the vehicle list, as before, so the assertions after it stay. A flow that asserts "No vehicles yet" first taps `landing_vehicles` (which is "Add vehicle" then): the flows that start with the empty state are updated one by one (the task lists them).
   One new flow (`vehicles/landing`) follows the happy path: the four tiles, the vehicles tile to the list and back.

## Risks / Trade-offs

- [Every flow's first steps change] → the shared subflow absorbs most; the flows that assume the list at start are listed in the tasks and edited; because the shared subflow is touched, all four plain manifests are run once (a consequence of the change, not a whole-suite regression run: `docs/test-strategy.md`).
- [Three disabled tiles look unfinished] → deliberate and temporary: they hold the places of `add-direct-logging`, `add-trip-logging` and the fourth action; a screen reader says they are not available, and their disabled colors tell them from the available tile.
- [The landing screen's tile sizing in landscape on a small phone] → checked on the emulator in both orientations (the tasks).
- [A restored old back stack has no landing screen] → accepted (see decision 6).

## Open Questions

- What the fourth tile becomes is undecided ("Placeholder" is the label until then); a settings entry or reports are candidates, and the label changes with it.
