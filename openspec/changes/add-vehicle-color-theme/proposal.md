# Proposal

> **Builds on `add-vehicle-color`** (the vehicle's color, the presets, the icons drawn from it and the single animated color). Apply and archive that change first: this
> one adds requirements to its `vehicle-color` capability and reuses `rememberAnimatedColor`.

## Why

After `add-vehicle-color` the vehicle's color shows only in its icons. The point of the color is that the vehicle's screens feel like that vehicle, and the way to
find out whether that works, with real contrast rules, real animation cost and the app's own identity intact, is to do it on **one pair of screens first**: the add and edit
vehicle screens, where the color is chosen and every change is visible at once. If it works there, the other vehicle-specific screens follow (see the end of this proposal); if it does
not, it has cost two screens.

## What Changes

- The **add and edit vehicle screens are themed by the vehicle's color**: a full Material 3 color scheme (light or dark, following the device) is **derived from the form's current
  color** and replaces the Petroleum scheme on these two screens: background and surfaces, containers, text fields, buttons, the radio buttons and selected tiles, and the icons.
- **The screen changes color live, in one animation.** Choosing a swatch, or a picture setting the color, moves *every* color of the screen together: one animated color (the one of
  `add-vehicle-color`) drives the derived scheme, so nothing jumps or runs out of step.
- **The app's identity stays where it is not vehicle-specific.** The app bar keeps Petroleum Deep with light content on every screen, error colors stay the app's error colors, and every
  other screen keeps the Petroleum scheme. Leaving the form restores the app's theme.
- **Legibility is guaranteed by construction and by test:** every text pair reaches 4.5:1 and every outline, icon and control 3:1, in both schemes, for every preset, for white, black and
  greys and for a sweep of hues (the same rule and the same contrast function as the app's own theme test).
- The theme change is **measured**: the derivation cost per frame and the animation's smoothness on the emulator are recorded, because deriving a scheme per frame is the one
  new cost.

Out of scope: theming any other screen (see below), a colored app bar, per-vehicle dark or light override, sliders or hex entry, changing how colors are stored.

**What this proves, and what comes next.** This change is the test of the approach. Its last task records findings (does the derived scheme look and feel right, does the default color's
scheme sit well next to the Petroleum screens, what the animation costs, what happened to the accents and the header) and proposes the next change: doing the same for **the other
vehicle-specific screens** (the vehicle details screen, the log and the log-distance screens, and the vehicle list rows), including how the list (app theme) and a vehicle's screens (vehicle
theme) meet when navigating. That is deliberately not decided here.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-color`: gains the requirements "The add and edit screens are themed by the vehicle's color" and "The screen's colors change in one animation".
- `app-shell`: the requirement "Material Design 3 theming" no longer says the theme is the same on every screen without exception.

## Impact

- `shared/` commonMain: `VehicleScheme.of(seed, dark)` (a full `ColorScheme` from HCT tonal palettes, error roles from the app's schemes), `VehicleTheme(color)` (animated
  color, derived scheme, `MaterialTheme`), the add and edit screens wrapped in it.
- No storage, migration or dependency change (the HCT library comes with `add-vehicle-color`).
- Tests: a scheme contrast sweep (reusing the theme's contrast function), role completeness; Maestro flows for the two screens; a recorded measurement.
- `openspec/config.yaml`: notes that the add and edit vehicle screens are themed by the vehicle's color while the app bar and other screens are not.
