# Design

## Context

`add-vehicle-color` gives every vehicle one color, the twelve presets, `VehicleTones` for icons (`primary`-on-`primaryContainer` tones), the HCT wrapper `HctColors` and the single animated color
`rememberAnimatedColor`. The app theme (`ui/theme`) has two hand-set Petroleum schemes (every role set), a constant dark header (`HeaderContainer`), fixed error roles and `DrivingLogTheme.domain` (distance and fuel accents).
`ThemeColorsTest` checks every text pair at 4.5:1 and every outline, icon and control at 3:1 with `Contrast.kt`. This change applies a derived scheme on two screens to test the approach.

## Goals / Non-Goals

**Goals:** a full scheme derived from one color, legible for every color by construction and by test; a live, animated change of the whole screen from one animated color; the app bar, error roles and other screens untouched; a measurement of the cost.
**Non-Goals:** other screens, a colored app bar, storage changes, a per-vehicle mode override.

## Decisions

1. **Derive the scheme from HCT tonal palettes with Material's tone table.** `VehicleScheme.of(seed, dark)` builds tonal palettes from the seed's hue and chroma: primary (the seed hue, chroma capped to about 48 so a very saturated color is not garish), secondary (same hue, chroma 16), tertiary (hue + 60,
   chroma 24), neutral (hue, chroma 4) and neutral variant (hue, chroma 8); roles take the M3 tones: light `primary` 40, `onPrimary` 100, `primaryContainer` 90, `onPrimaryContainer` 10, `secondary` 40 …, `surface`/`background` 98, `onSurface` 10, `surfaceVariant` 90, `onSurfaceVariant` 30, `outline` 50,
   `outlineVariant` 80; dark `primary` 80, `onPrimary` 20, `primaryContainer` 30, `onPrimaryContainer` 90, `surface`/`background` 6, `onSurface` 90, `surfaceVariant` 30, `onSurfaceVariant` 80, `outline` 60 (the standard `TonalSpot` table; *verify* against the library's `SchemeTonalSpot` and prefer it if it is available and matches, since
   then no table is ours). **Every role is set** (as in `Theme.kt`, so none of Material's default purple shows), including the surface container family and the fixed roles. The **error roles are copied from the app's schemes**, not derived, so an error is always the same recognizable red. The tone
   pairs are Material's, whose 40 to 50 step differences give the contrast by construction; chroma caps and near-zero chroma for greys keep white, black and grey vehicles neutral.
   Because `VehicleTones` (icons) already uses primary 40 on primaryContainer 90 (light) and 80 on 30 (dark), the icons are the scheme's `primary`/`primaryContainer` and need no change.
2. **Contrast tested, not assumed.** A `VehicleSchemeTest` builds the scheme for all presets, white, black, greys and a grid (36 hues x 5 chroma levels) in light and dark and checks the same text and control pairs as `ThemeColorsTest` (extracted into a shared list of pairs so the two tests use one definition), with `Contrast.kt`. Any failing color is a
   design bug fixed by changing tones or the chroma cap, not by excluding the color.
3. **One animated color drives everything.** `VehicleTheme(color: Rgb, content)` calls `rememberAnimatedColor(color)` **once** and derives `VehicleScheme.of(animated, dark)` for `MaterialTheme(colorScheme = …)`; the screens below animate nothing themselves (the rule of `add-vehicle-color`). `remember(animated, dark)` avoids recomposing
   when the color is at rest. Intermediate colors are HCT interpolations of two legible endpoints, and tones are fixed by the table, so **every intermediate scheme is a legible scheme** (the tone pairs do not move, only hue and chroma) — the "legible during the animation" requirement follows from the construction, and a test samples the animation
   at several progress values across a hue jump to confirm it.
4. **Cost.** Deriving a scheme is about 5 tonal palettes, roughly 40 tone lookups (each an HCT solve): tens of microseconds each. The plan is to derive per frame without caching and to **measure** it on the emulator (`dumpsys gfxinfo` janky-frame percentage during a color change, and a Macrobenchmark-free stopwatch of `VehicleScheme.of` in a
   unit test as a guard: for example under 2 ms on the JVM). If it is too slow, cache the five `TonalPalette`s per animated color (they are the expensive part) and look tones up per role, or step the animation in fewer frames. The result of the measurement goes into the findings (task 3.2).
5. **What stays fixed.** The app bar (`drivingLogTopAppBarColors`, `HeaderContainer`) and the system bars are not touched: the status bar's icons stay light over the header, and the navigation bar's icons follow the device mode as now. The crop screen keeps its fixed black surface. `DrivingLogTheme.domain` is
   not shown on these screens and stays as is.
6. **Where it is applied.** `AddVehicleScreen` and `EditVehicleScreen` wrap their `Scaffold` in `VehicleTheme(state.color)`. The edit screen's color is null until loaded, so it shows the app theme until then and then starts in the vehicle's color **without animating** (the first-display rule). Screens shown after navigating back are outside the wrapper, so the app theme returns
   at once (the seam between the vehicle theme and the app theme on navigation is one of the things to judge in the findings).
7. **Known judgment call: the default color's scheme is not the Petroleum scheme.** The scheme derived from `#203A43` is a relative of it (blue-green, low chroma), but not identical, so there is a visible step between a Petroleum screen and the add screen. Options if it looks wrong: tune the tones for low chroma, or special-case the
   default color to the exact Petroleum scheme (which would be the least surprising, at the price of a special case). Decided after seeing it on the emulator (task 2.2), not now.

## What we learn here, for the next change (the other vehicle-specific screens)

The findings recorded in task 3.2 answer: does the derived scheme look and feel right in light and dark; how the default color sits next to Petroleum; the measured cost and smoothness; how the fixed app bar looks over a colored screen (and whether a tone-10 colored bar would be better, which would change the app-shell rule);
how accents (Road Trip Emerald for distances, Fuel Gauge Gold for fuel) look on a themed screen (needed for the details and log screens, which show distances); and how the list (app theme) and a vehicle screen (vehicle theme) meet on navigation (an animated shared transition of the seed color is a candidate). The next change is
proposed from those answers.

## Risks / Trade-offs

- **Some seeds derive an ugly scheme** (very saturated yellows, near-greys with a hue): chroma caps and the preset choice limit it; the sweep test guards legibility, not beauty, which is judged by eye in task 2.2.
- **Per-frame derivation cost** (decision 4) is measured; the fallback is cheap.
- **A themed screen next to unthemed ones can feel inconsistent.** That is the point of the experiment; the findings decide whether to go on or to change the approach (for example, theme only the container of the form).
- **Two definitions of the contrast pairs** would drift, so the pairs are extracted and shared by the two tests.
