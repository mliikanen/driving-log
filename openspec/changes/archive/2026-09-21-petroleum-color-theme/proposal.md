# Proposal

## Why

The app is drawn with Material 3's stock purple scheme, which says nothing about a driving and fuel log. The project already has a considered
brand palette in `docs/color-palette.md` (the **Petroleum** theme: deep blue-greens, fuel gold, trip emerald, cool neutrals) that no screen uses.
Making it the default theme gives the app its own identity now, and gives the later per-vehicle palettes (see `add-vehicle-palette`) a
defined base to customise.

## What Changes

- The **default color theme becomes the Petroleum theme** from `docs/color-palette.md`, in a light and a dark scheme that follow the device setting
  as today. The palette document stays the source of the colors; the design records how each color maps to a Material 3 color role.
  - Light: the app background is **Cool Platinum** `#F4F7F6`, text is Asphalt `#12181B`, primary buttons and active elements are **Oil Slick Blue**
    `#203A43`, interactive accents (switches, secondary actions) are **Refinery Teal** `#2C5364`, dividers are **Exhaust Fog** `#E0E6ED`.
  - Dark: the app background is **Asphalt Dark** `#12181B`; the primary and secondary roles use lighter tints of the palette's blue-greens so buttons,
    icons and switches stand out from the dark background, with Oil Slick Blue and Refinery Teal as their containers.
- **App bars are dark headers** in both modes: **Petroleum Deep** `#0F2027` with light text and icons, as the palette document asks
  ("dark navigation bars, dominant app headers"). A thin divider separates the header from the dark background in dark mode.
- **Distances and fuel get their own accent colors:** logged distances are drawn in **Road Trip Emerald** (a deeper shade of it in light mode, where the
  bright one is too pale to read); **Fuel Gauge Gold** is defined for refueling and used as a fill with dark text where a gold surface is needed. The gold
  is not used yet, because refueling does not exist.
- **Every text and control color meets accessibility contrast** (4.5:1 for text, 3:1 for outlines and controls) in both schemes. The palette has raw pairs that
  do not (Emerald on Cool Platinum is 1.8:1, Gold on Cool Platinum 1.6:1, Oil Slick Blue on Asphalt Dark 1.5:1), so the theme uses the palette colors where they
  pass and derived tints or shades where they do not, and a test proves every pair.
- **Android system surfaces match:** the launch window background follows the theme (no white or black flash before the first frame), the status bar shows
  light icons over the dark header, and the navigation bar's icons follow the light or dark scheme.
- The crop screen's frame becomes white, so it stays visible on its black surface in both modes.
- Out of scope: the per-vehicle palette and theming from a vehicle's colors (`add-vehicle-palette`), a user-selectable or custom theme, Android's dynamic
  (wallpaper) color, a high-contrast mode, new fonts or shapes, the app icon, and verifying iOS system bars on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `app-shell`: the theming requirement now names the Petroleum theme, its schemes, the header, the contrast rule and the system bars, and a new requirement
  defines the distance and fuel accent colors.

## Impact

- `shared/` commonMain: `ui/theme` gains the palette constants, the two `ColorScheme`s, a `DomainColors` set for distance and fuel provided through the theme, a
  contrast-ratio helper, and top app bar colors used by the six screens' app bars; the distance figure in log rows uses the distance color; the crop frame color.
- `androidApp`: window background colors (light and dark) in the theme resources, and the edge-to-edge bar styles in `MainActivity`.
- Tests: pure tests of the schemes (palette colors present, the accessibility contrast of every pair, the light and dark schemes differ where they should) and a
  script that samples screenshot pixels on the emulator in both modes.
- No new dependency; no data or storage change.
- `openspec/config.yaml`: the project context notes the Petroleum theme, that the palette document is its source, and the contrast rule for any new color.
