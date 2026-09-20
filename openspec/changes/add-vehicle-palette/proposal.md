# Proposal

> **Depends on `add-vehicle-picture`.** This change is applied after `add-vehicle-picture` has been applied and archived: it
> reads the picture's confirmed crop, saves in the same transaction as the picture, and its delta of `vehicles` is written
> against the text that change leaves behind. Do not apply it earlier.

## Why

The app is going to take on each vehicle's look: an accent color, header and screen backgrounds and matching text colors, so that
the vehicle screens feel like that vehicle. That later theming needs a small set of colors that belong to the vehicle. This change
gets and stores that set, the vehicle's **palette**, without changing how any screen looks yet, so that the theming can be its
own small change on top of data that already exists.

## What Changes

- Every vehicle can have a **palette of four colors**, stored with the vehicle (two nullable columns, migration `4.sqm`, schema version 5).
- **With a picture:** when the user confirms the crop of the picture (`add-vehicle-picture`), the app **extracts the four colors from
  the cropped photo**. The colors are picked deterministically (the same photo always gives the same palette), are visibly
  different from each other, and keep whites, blacks and greys (a white car gets a white color). A photo with fewer than four
  distinct colors is filled up with lightness variations of its main color. The palette is saved with the vehicle and the picture in one step.
- **Without a picture:** the add and edit screens offer a **main color** chosen with a **simple color picker** (a row of preset
  swatches and hue, saturation and brightness sliders, with a preview; no third-party picker). The app **derives the four-color
  palette from it**: the chosen color first, then three similar colors of the same family. The main color is optional and can be cleared.
- The palette **follows the source**: a picture wins; when the picture is removed the palette is derived from the chosen main color, or
  the vehicle has no palette when it has neither. Editing a vehicle without touching the picture or the color never re-computes the
  palette. The invariant is: a vehicle has a palette if and only if it has a picture or a main color.
- The add and edit screens **show the four colors** as swatches (a live preview while choosing a color or after cropping a picture).
  That is the only visible change: **no screen is themed yet** (see out of scope).
- Vehicles that already have a picture but no palette (saved before this change) **get their palette when the app starts**.
- The palette is **role-neutral**: four opaque colors in a fixed order, the most representative first. Which color becomes the
  accent, a background or a text color is decided by the later theming change, which can use the colors' tones.
- The extraction and the derivation use **Kotlin Multiplatform libraries working on plain pixel arrays and colors** (details, the alternatives
  and the check that has to pass before they are used are in the design): the multiplatform port of the Android palette
  algorithm for photos, and the multiplatform port of Material Color Utilities (HCT color space) for deriving colors. Both sit behind two
  small interfaces so the choice can be changed, and the app falls back to its own implementation if a library fails the check.
- Out of scope: theming any screen with the palette (accent, header and screen backgrounds, text on backgrounds), a dark theme
  per vehicle, an editable palette (choosing the four colors one by one), picking a color from the photo, several palettes per vehicle,
  syncing palettes to a backend, and verifying the iOS side on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities
- `vehicle-palette`: the four-color palette of a vehicle, how it is extracted from the picture and derived from a chosen main
  color, the simple color picker, the swatches on the forms, storage, and backfilling existing pictures.

### Modified Capabilities
- `vehicles`: the edit screen also offers the main color (for a vehicle without a picture), so the requirement listing what the edit
  screen offers changes (`Edit a vehicle`).

## Impact

- `shared/` commonMain: `Rgb` and `Palette` domain types, `Vehicle.mainColor` and `Vehicle.palette`, the `PaletteExtractor` and `PaletteDeriver`
  interfaces with their library-backed implementations (and a pure fallback), the rule for which palette a vehicle has, HSV/RGB
  conversions for the picker, the `ColorPicker` and `PaletteSwatches` composables, palette and main color in the add and edit states and
  processors (the palette computed at crop confirmation is part of the picture draft), repository writes in the picture transaction,
  the startup backfill, migration `4.sqm`.
- Platform code: `ImageCodec.sample` (decode a stored image to a bounded array of ARGB pixels; Android `ImageDecoder`, iOS ImageIO), added
  to the interface from `add-vehicle-picture`.
- Dependencies: `com.materialkolor.palette:core` (multiplatform Android palette port, no Compose) and `com.materialkolor:material-color-utilities`
  (HCT, no Compose), both Apache 2.0, to be verified against Kotlin 2.4.20 and the Android and iOS targets as the first task.
- `maestro/` gets a flow that adds a solid-color picture and reads the palette swatches, and one that chooses a color; `openspec/config.yaml`
  notes that a vehicle's palette is stored as data and is what theming uses.
