# Tasks

`add-vehicle-color` is applied and archived first (the color, `VehicleTones`, `HctColors` and `rememberAnimatedColor`).

## 1. The derived scheme

- [ ] 1.1 Extract the list of text and control pairs of `ThemeColorsTest` into one shared definition used by that test and the new one, and verify `ThemeColorsTest` still passes unchanged in what it checks
- [ ] 1.2 Add `VehicleScheme.of(seed, dark): ColorScheme` (HCT tonal palettes with Material's tone table, chroma caps, every role set, the error roles copied from the app's schemes; use the library's `SchemeTonalSpot` if it matches), and verify `VehicleSchemeTest`: every role is set, the error roles equal the app's, the icon colors equal `VehicleTones`, the pairs of task 1.1 reach 4.5:1 and 3:1 for every preset, white, black, greys and a grid of 36 hues x 5 chroma levels in light and dark, the result is deterministic, and a guard that one derivation takes well under a frame (a few ms on the JVM)
- [ ] 1.3 Verify that intermediate colors of the animation keep the scheme legible: `VehicleSchemeTest` samples `lerpHct` at several progress values across a large hue jump (and across 0/360) and checks the same pairs

## 2. The screens

- [ ] 2.1 Add `VehicleTheme(color: Rgb, content)` (one `rememberAnimatedColor`, the derived scheme for the device mode, `MaterialTheme(colorScheme = …)`, app bar and system bars untouched) and wrap `AddVehicleScreen` and `EditVehicleScreen` in it (the edit screen shows the app theme until the vehicle has loaded, then starts in its color without animating), and verify it compiles for Android and iOS, the existing add and edit processor and screen tests pass, and no screen below animates a color of its own
- [ ] 2.2 Check the themed screens by hand on the emulator (add and edit, every preset, light and dark, landscape, keyboard open, error states, the crop-to-color flow), including a frame sequence of a color change and an interrupted one, the step from a Petroleum screen to the add screen and back, and fix what looks wrong (tones, the chroma cap, or the default-color decision of the design)
- [ ] 2.3 Measure the animation on the emulator (`adb shell dumpsys gfxinfo` janky frames and the 90th to 99th percentile frame times over a series of color changes) and record the numbers; if it janks, apply the caching of the design and measure again

## 3. Flows, context and findings

- [ ] 3.1 Add Maestro flows: on the add screen choose a color and complete and save a vehicle (every control still reachable and legible: the flow reads texts and taps controls), the same on the edit screen, a rotation with a color chosen, and back on the list (assert the list is unaffected); verify they pass together with the flows of the `vehicles` and `appearance` manifests
- [ ] 3.2 Record the findings in the design (a "Findings" section): how the scheme looks in light and dark, the default color next to Petroleum, the measured cost, the fixed app bar, accents on a themed screen, the navigation seam, and propose the next change for the other vehicle-specific screens from them
- [ ] 3.3 Add to the project context in `openspec/config.yaml` that the add and edit vehicle screens are themed by the vehicle's color (derived scheme, animated as one, app bar and other screens not themed), and verify `openspec validate --all --strict` passes
- [ ] 3.4 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, and verify all pass (Maestro is not part of the final regression run: the manifests this change touches, `vehicles` and `appearance`, were run in 3.1)
