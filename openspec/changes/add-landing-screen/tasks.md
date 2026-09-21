# Tasks

## 1. The state and the tiles

- [x] 1.1 Add `LandingContract` (state `LandingState(isLoading, hasVehicles)`, intents `OpenVehicles` and `AddVehicle`, effects `ShowVehicles` and `ShowAddVehicle`) and `LandingProcessor` observing the vehicles, and the pure `landingTiles(...)` giving each tile its id, accessible name, icon key and enabled flag; verify by `LandingProcessorTest` and a tiles test: loading shows the first tile without a label and not tappable, no vehicles gives "Add vehicle", vehicles give "Vehicles", exactly the other three tiles are not enabled, the intents lead to the right effects, and the state follows a vehicle being added
- [x] 1.2 Add the icons: the Phosphor SVGs (`note-pencil-fill`, `path-fill`, `question-fill`; the vehicles tile is the generic car) in `docs/icons/phosphor` and `LandingIcons` with their path data; verify by a test that every path parses to a non-empty vector (like `PhotoIconsTest`) and that the Phosphor licence is still the one in `THIRD_PARTY_NOTICES.md`

## 2. The screen and the navigation

- [x] 2.1 Add `LandingScreen`: the top app bar "Driving Log" and the two-by-two grid of Material 3 `Card(onClick, enabled)` tiles (one icon each and no visible text, named for screen readers by the icon's `contentDescription`, sized from the space, scrolling when it cannot fit, `ScreenBottomSpace`; `cardColors(primaryContainer, onPrimaryContainer)` for the action look and **no disabled colors, alphas or states written by hand**: `enabled = false` gives them; test tags `landing_vehicles`, `landing_log_event`, `landing_trip`, `landing_more`); verify it compiles for Android and iOS, and that a test (in the style of `ThemeColorsTest`) holds in both schemes: the available tile's label reaches 4.5:1 and its icon 3:1 on `primaryContainer`; `git grep -n "alpha\|copy(alpha" shared/src/commonMain/.../landing` finds no hand-made disabled color
- [x] 2.2 Add `LandingNavKey`, register it, make it the start destination in `App.kt`, and give `VehicleListScreen` the title "Vehicles" with a back arrow (the shared `BackButton`); verify with `VehicleNavKeysTest` (the key restores and has value equality), and on the emulator that the app starts on the landing screen, "Vehicles" opens the list, back returns, "Add vehicle" (with no vehicles) opens the add screen and after saving the landing screen's first tile reads "Vehicles", and that rotation keeps the screen in both orientations

- [x] 2.3 Icon-only tiles (the developer's decision during apply): remove the visible labels, name each tile by its icon's `contentDescription`, keep the first tile's icon the car whatever the vehicles, and remove the plus icon (`LandingIcon.ADD_VEHICLE`, its SVG, path and the notice); verify by the unit tests (the first tile's icon is the car with and without vehicles, its name is "Add vehicle" without), by the emulator screenshots in both orientations and by the `vehicles` manifest reading the tiles by name

## 3. Flows

- [x] 3.1 Update `subflows/add-vehicle.yaml` to start on the landing screen (tap `landing_vehicles`; tap `add_vehicle` when the list is what opened) and end on the vehicle list, and update the flows that assume the list at their start (`vehicles/add-and-browse`, and any other that asserts "No vehicles yet" or taps `add_vehicle` directly: find them with `git grep -n "empty_state\\|No vehicles yet\\|id: add_vehicle" maestro`); verify by running the `vehicles` manifest
- [x] 3.2 Add `maestro/vehicles/landing.yaml` (the four tiles are there, found by their names, the disabled ones do nothing, "Add vehicle" with no vehicles opens the add screen, after adding a vehicle "Vehicles" opens the list and back returns) and list it in `manifests/vehicles.yaml`; verify it passes
- [x] 3.3 Check by hand on the emulator (light and dark, portrait and landscape, a large font scale) that the grid looks right, is two by two, does not clip and scrolls when it must, that "Vehicles" is in the action colors and the three disabled tiles are visibly in Material's disabled colors (not a paler primary) in both schemes, and that a screen reader announces them as disabled, and that a screen reader (TalkBack or the accessibility labels Maestro reads) names every tile

## 4. Final verification

- [ ] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, and verify all pass; run the four plain Maestro manifests once while applying (the shared subflow they all use changed), not as part of the final regression run
