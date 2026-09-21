# Design

## Context

- `ui/theme/Theme.kt` builds the app's `ColorScheme` with `lightColorScheme()` and `darkColorScheme()` (Material 3's defaults, purple) and wraps the app
  in `MaterialTheme`. `App()` calls `DrivingLogTheme`. Every screen already draws through Material color roles (`primary`, `surfaceVariant`,
  `onSurfaceVariant`, `error`, ...); the only fixed colors are on the crop screen (a black surface with white text) and the vehicle icon (a black
  vector that is always tinted). Each screen has a default `TopAppBar` or `CenterAlignedTopAppBar`.
- `docs/color-palette.md` defines the Petroleum palette: Petroleum Deep `#0F2027`, Oil Slick Blue `#203A43`, Refinery Teal `#2C5364`, Fuel Gauge Gold
  `#FFB703`, Road Trip Emerald `#06D6A0`, Asphalt Dark `#12181B`, Cool Platinum `#F4F7F6`, Exhaust Fog `#E0E6ED`, each with a stated usage.
- Android: `MainActivity` calls `enableEdgeToEdge()` with defaults; the theme is `Theme.DeviceDefault.DayNight` with no window background of its own.
- Project rules: business logic and UI live in commonMain; Android is primary; a behaviour change needs a change in `openspec/changes/`.

## Goals / Non-Goals

**Goals:**
- Make the Petroleum palette the app's default look, light and dark, through Material 3 color roles so every screen follows without per-screen colors.
- Keep every text and control accessible (4.5:1 and 3:1), proved by a test, even where the raw palette colors cannot be used.
- Give distance and fuel their semantic accent colors, and dark headers as the palette asks.
- Make the Android launch window and system bars agree with the theme.

**Non-Goals:**
- Per-vehicle palettes and theming from them (`add-vehicle-palette`), user-selectable or custom themes, dynamic (wallpaper) color, a high-contrast mode,
  fonts, shapes, the app icon, and running the iOS system bars on a device.

## Decisions

### 1. Palette constants and the role mapping

`ui/theme/Color.kt` holds the eight palette colors under the document's names (`PetroleumDeep`, `OilSlickBlue`, `RefineryTeal`, `FuelGaugeGold`,
`RoadTripEmerald`, `AsphaltDark`, `CoolPlatinum`, `ExhaustFog`) and the derived colors named for what they derive from (`OilSlickTint`, ...). `Theme.kt`
builds the two schemes from them. The mapping follows the palette's stated usage: Oil Slick Blue is the primary (buttons, active elements, containers), Refinery
Teal the interactive accent (switches, secondary actions), Fuel Gauge Gold the tertiary (fuel), Exhaust Fog the divider and variant surface, Cool Platinum and
Asphalt Dark the backgrounds.

| Role | Light | Dark |
| --- | --- | --- |
| background, surface | `#F4F7F6` Cool Platinum | `#12181B` Asphalt Dark |
| onBackground, onSurface | `#12181B` | `#E1E8EA` |
| surfaceVariant / onSurfaceVariant | `#E0E6ED` Exhaust Fog / `#3F4D55` | `#26343A` / `#B7C4CA` |
| primary / onPrimary | `#203A43` Oil Slick Blue / `#FFFFFF` | `#8FC3D7` (tint of Refinery Teal) / `#0F2027` |
| primaryContainer / onPrimaryContainer | `#CFE3EC` / `#0F2027` | `#203A43` Oil Slick Blue / `#D2E5EE` |
| secondary / onSecondary | `#2C5364` Refinery Teal / `#FFFFFF` | `#A3C6D4` / `#0F2027` |
| secondaryContainer / onSecondaryContainer | `#D5E7EF` / `#0F2027` | `#2C5364` Refinery Teal / `#D8E8F0` |
| tertiary / onTertiary | `#FFB703` Fuel Gauge Gold / `#12181B` | `#FFB703` / `#12181B` |
| tertiaryContainer / onTertiaryContainer | `#FFE3A0` / `#3B2A00` | `#5A4200` / `#FFE3A0` |
| outline / outlineVariant | `#66767F` / `#E0E6ED` Exhaust Fog | `#7C8E97` / `#2C3A41` |
| error / onError | `#B3261E` / `#FFFFFF` | `#F2B8B5` / `#601410` |
| errorContainer / onErrorContainer | `#F9DEDC` / `#410E0B` | `#8C1D18` / `#F9DEDC` |
| surface containers (lowest to highest) | `#FFFFFF`, `#EFF3F3`, `#E8EDEE`, `#E0E6ED`, `#D9E0E5` | `#0D1215`, `#161E22`, `#1A2328`, `#222D33`, `#2A363D` |
| inverseSurface / inverseOnSurface / inversePrimary | `#202B30` / `#E1E8EA` / `#8FC3D7` | `#E1E8EA` / `#263238` / `#203A43` |

The error colors are not in the palette (it has no red); the Material defaults are kept, since an error must not look like the blue-greens. The values in
the table are the design's starting point: the implementation may adjust a value to keep a contrast target, and the contrast test is what fixes it.

### 2. Where the raw palette fails, and what replaces it

Contrast ratios (WCAG relative luminance) computed for the palette's own pairs:

| Pair | Ratio | Use |
| --- | --- | --- |
| white on Petroleum Deep / Oil Slick Blue / Refinery Teal | 16.7 / 12.0 / 8.3 | fine as fills with white text |
| Asphalt on Gold / Emerald | 10.3 / 9.5 | fine as fills with dark text |
| Emerald on Asphalt Dark | 9.5 | fine as text in dark mode |
| **Emerald on Cool Platinum** | **1.75** | too pale: light-mode distance text uses `#006F53` (5.7 on Platinum, 4.9 on Exhaust Fog) |
| **Gold on Cool Platinum** | **1.62** | never text in light mode; fill with dark text, or `#7A5600` (6.2) if ever a text is needed |
| **Oil Slick Blue on Asphalt Dark** | **1.49** | too dark to be the dark-mode primary: dark primary is the tint `#8FC3D7` (9.4), Oil Slick Blue becomes its container |
| **Refinery Teal on Asphalt Dark** | **2.16** | same: dark secondary is `#A3C6D4` (9.9) |
| Exhaust Fog on Cool Platinum | 1.17 | a divider is not required to reach 3:1 (decorative), but a control's outline is: `outline` is `#66767F` (4.4) |
| **Petroleum Deep on Asphalt Dark** | **1.07** | the dark header is almost the background: a 1 dp `outlineVariant` divider (1.5) below it separates them |

The candidate schemes in decision 1 were checked pair by pair: every text pair is at least 4.5:1 (lowest: light `onSurfaceVariant` on `surfaceVariant`, 6.95; dark
`onSecondaryContainer` on `secondaryContainer`, 6.6) and every outline, icon and control at least 3:1 (lowest: light `outline` on the background, 4.4; dark `outline` on the
background, 5.3).

### 3. Dark headers

The palette wants dark navigation bars and dominant headers (Petroleum Deep). Material's app bars take the `surface` color by default, so the theme provides
`drivingLogTopAppBarColors()` (container Petroleum Deep, title, navigation and action icons `#F4F7F6`, 15.5:1) and each of the six screens' app bars passes it; a small `DrivingLogTopAppBar`
wrapper would have hidden the differences between the centre-aligned list header and the others, so the colors are shared and the bars stay as they are. In dark mode the header is followed by a 1 dp divider in `outlineVariant`
(decision 2). The floating action button on the vehicle list uses `primary`/`onPrimary` (Oil Slick Blue in light) instead of Material's default container colors, because the palette names Oil Slick Blue for primary
button backgrounds.

### 4. Distance and fuel accents

`DomainColors(distance, fuel, onFuel)` is provided by `DrivingLogTheme` through a `CompositionLocal` (`LocalDomainColors`, read as `DrivingLogTheme.domain`): distance is `#06D6A0` in dark and `#006F53`
in light; fuel is `#FFB703` with `onFuel` `#12181B` in both, and is not used by any screen yet (refueling does not exist). The trailing figure of a distance entry (`+30 km`) in the log rows
takes the distance color; an initial odometer reading and an odometer anchor keep the normal color, since they are not distances. A composable that needs a domain color reads it from the theme and
never hard-codes it.

### 5. Android launch window and system bars

- **Window background.** `Theme.DrivingLog` gets `android:windowBackground` from a color resource: `values/colors.xml` `#F4F7F6`, `values-night/colors.xml` `#12181B`. Before this the window
  was the DayNight default (white or black), so the first frame flashed a color that is not the theme's.
- **System bars.** `MainActivity` calls `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))`.
  The status bar is always drawn with light icons because the header behind it is always dark (with `auto` it would follow the system setting and show dark icons on the dark header in light mode). The
  navigation bar sits over the app background, so it follows the system setting: dark icons in light mode, light icons in dark mode. The three-button and gesture navigation bars both take this.
- **iOS.** Compose Multiplatform hosts the UI in a `UIViewController`; over a dark header the status bar needs light content (`UIStatusBarStyleLightContent`, through the view controller's
  preferred status bar style or `Info.plist`). There is no Xcode project, so this is recorded in `iosApp/README.md` and verified when the project exists.

### 6. The crop screen

The crop screen is a fixed black surface (photos are judged on black) with white text. Its frame used `primary`, which is Oil Slick Blue in light mode and almost invisible on black (1.75:1); the frame becomes white
(21:1) in both modes. The screen keeps its own colors on purpose and does not follow the scheme.

### 7. Room for the vehicle palette

`drivingLogColorScheme(darkTheme)` stays a pure function of the theme mode and the only place a scheme is built. `add-vehicle-palette` will later feed a vehicle's colors into a variant of it; this change makes
no provision beyond keeping that function pure and the constants and derived colors named, so a later scheme can be built by replacing them.

### 8. Testing and verification

- **Pure, in commonTest:** a `contrastRatio` helper (WCAG relative luminance) and tests that every text pair meets 4.5:1 and every outline and control 3:1 in both schemes; that the light scheme has Cool
  Platinum, Oil Slick Blue, Refinery Teal and Exhaust Fog in their roles and the dark scheme Asphalt Dark; that neither scheme contains any of Material's default purple roles; that the two schemes differ where they should
  (background, primary) and share the tertiary gold; that the header color is Petroleum Deep and its title contrast at least 4.5:1 in both; that the light distance color reaches 4.5:1 on the background and on Exhaust Fog and the dark
  one on Asphalt Dark; that `DomainColors` differs between light and dark for distance only.
- **On the emulator:** a script, `maestro/theme/run.sh`, switches the device to light and to dark mode (`cmd uimode night`), opens the list, details and add screens with a flow, takes screenshots and samples their
  pixels with a small PNG reader: the header area is `#0F2027`, the background area `#F4F7F6` or `#12181B`, a primary button `#203A43` in light mode. The launch window color is checked by taking a screenshot right after a cold start
  (`am start` then an immediate capture) against the background color. Screenshots of every screen in both modes are reviewed by eye for what a script cannot judge (balance, the dark header's divider, the gold's absence).
- The existing Maestro suite must still pass: colors change no behaviour.

## Risks / Trade-offs

- **Light-mode header is dark on a light screen.** It follows the palette's brief ("dominant app headers") and reads as branded, but it is a stronger look than Material's default light bars; the status bar is forced light-on-dark to
  match. Reverting to a light header later is a one-line change to the header colors.
- **The dark header is nearly the dark background** (1.07:1); the divider is what separates them. If that looks weak on the emulator the header can use Oil Slick Blue in dark mode (1.5:1) instead.
- **Derived colors are not in the palette document.** The light-mode distance shade, the dark-mode tints and the error colors are derived; the document stays as the source of the named colors, and the design's tables record the derivations so
  they are not rediscovered.
- **`SystemBarStyle.dark` for the status bar assumes the header is always dark.** If a screen without a header is ever added (a full-screen photo, say), it must set its own bar style.
- **Material 3 components pick their colors from roles the palette does not address** (switches, text field outlines, radio buttons, dialogs, segmented buttons). They follow the mapping automatically, and the emulator review in every screen
  is what catches an odd result; a component that looks wrong gets an explicit override, not a change to a role.
- **Emerald as a bright accent in dark mode** can be loud next to text; it is limited to the distance figure.
