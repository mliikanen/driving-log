# Tasks

## 1. The theme

- [ ] 1.1 Add the palette constants and derived colors (`ui/theme/Color.kt`, named as in `docs/color-palette.md`), the `contrastRatio` helper, and build the light and dark `ColorScheme`s from the mapping in the design (every role, no Material default left), and verify unit tests: the palette colors are in their roles in each scheme, neither scheme has a Material default purple role, the schemes differ where they should, and every text pair has at least 4.5:1 and every outline and control at least 3:1 in both schemes
- [ ] 1.2 Add `DomainColors` (distance, fuel, on-fuel) provided by `DrivingLogTheme` through a `CompositionLocal` and read as `DrivingLogTheme.domain`, and verify unit tests: the distance color is `#06D6A0` in dark and a deeper shade in light with at least 4.5:1 on the background and on Exhaust Fog, and the fuel color is the same gold with dark text in both
- [ ] 1.3 Add `drivingLogTopAppBarColors()` (Petroleum Deep container, `#F4F7F6` title and icons), use it in the six screens' app bars, draw the 1 dp `outlineVariant` divider under the header in dark mode, and give the vehicle list's floating action button the `primary`/`onPrimary` colors, and verify it compiles for Android and iOS and a unit test that the header title contrast is at least 4.5:1 in both schemes

## 2. Where the colors are used

- [ ] 2.1 Draw the distance figure of a distance entry in the log rows (recent events and the full log) with the distance color, and no other text with it, and verify a unit test of which row content is drawn as a distance, and on the emulator that a distance row shows the accent in light and dark mode
- [ ] 2.2 Make the crop screen's frame white so it is visible on its black surface in both modes, and verify on the emulator in light and dark mode

## 3. Android system surfaces

- [ ] 3.1 Add the window background colors (`values/colors.xml` `#F4F7F6`, `values-night/colors.xml` `#12181B`) to `Theme.DrivingLog`, and configure the edge-to-edge bar styles in `MainActivity` (status bar always light icons, navigation bar following the system setting), and verify on the emulator that a cold start shows the scheme's background before the first frame and that the status bar icons are light over the header and the navigation bar icons follow the scheme, with gesture and three-button navigation
- [ ] 3.2 Record in `iosApp/README.md` that the status bar needs light content over the dark header when the Xcode project is created, and verify the file says so

## 4. Verification and project context

- [ ] 4.1 Review every screen on the emulator in light and dark mode (list, add, edit, details with and without a picture, the log, the log distance form with its pickers and dialogs, the crop screen), in portrait and landscape, and fix any component that looks wrong with an explicit override instead of a change to a role
- [ ] 4.2 Add `maestro/theme/run.sh` that switches the emulator to light and dark mode, takes screenshots of the list, details and add screens and samples their pixels (the header `#0F2027`, the background `#F4F7F6` or `#12181B`, a primary button `#203A43` in light mode), and restores the device's mode, and verify it passes
- [ ] 4.3 Add to the project context in `openspec/config.yaml` that the default theme is Petroleum from `docs/color-palette.md`, the derived colors and where they are recorded, and the rule that any new color is added to the schemes and passes the contrast test, and verify `openspec validate --all --strict` passes
- [ ] 4.4 Run `./gradlew :shared:allTests :androidApp:assembleDebug`, the whole Maestro suite, `maestro/theme/run.sh` and `openspec validate --all --strict`, and verify all pass
