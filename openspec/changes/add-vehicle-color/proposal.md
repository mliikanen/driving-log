# Proposal

> **Builds on `add-vehicle-picture` and `add-vehicle-type`, both archived.** It reads the picture's confirmed crop, and its deltas of `vehicles`
> are written against the text those changes left behind (the edit screen offers the type too). The type took migration `4.sqm` and schema
> version 5, so this change is migration `5.sqm` and schema version 6. It replaces the earlier `add-vehicle-palette` proposal (a stored
> four-color palette with two libraries, a backfill and no visible result); its follow-up is `add-vehicle-color-theme`.

## Why

Every vehicle looks the same apart from its icon. The app is going to take on each vehicle's look, so that the vehicle screens feel like
that vehicle. That needs one thing to start from: a color that belongs to the vehicle. This change gives every vehicle **one color**,
lets the user choose it (or take it from the vehicle's photo), and shows it in the first place that is easy to verify, the vehicle
icons. The rest (a whole screen theme derived from the color) is `add-vehicle-color-theme`, built on the color and the animation this
change introduces. One color is enough because everything else (tints, containers, a full palette) is *derived* from it when it is drawn,
so nothing derived is stored.

## What Changes

- Every vehicle has a **color**, and it is **mandatory in the data model**: one `RRGGBB` text in a `NOT NULL` column with a default, and the
  repository cannot save a vehicle without one. The default is **the application's main theme color** (Oil Slick Blue, `#203A43`, the
  primary of the light scheme). The migration gives every existing vehicle that default. There is **no backfill**: vehicles that already
  have a picture keep the default until the user picks a color or crops a picture again.
- The add and edit screens offer a **color picker: a set of twelve preset colors** chosen as good bases for derived palettes, the first
  being the default. The current color is marked. On the add screen the default is selected at first. (No sliders or hex entry yet.)
- **A picture sets the color.** When the user confirms the crop of a picture, the app extracts **one representative color** from the cropped
  photo and makes it the current color. The picker then shows it as an extra swatch labelled "Picture color", selected, so the user can see what
  happened and can pick a preset instead (and come back to the picture color). A color that is not a preset (the picture's, or one saved earlier)
  is shown as an extra swatch, so the picker always shows the current color. Cancelling a crop or removing the picture does not change the color.
- **Icons follow the color.** The SVG-based vehicle icons (list rows, the details header, the form's picture preview and the type tiles) are
  drawn in a tint derived from the vehicle's color, on a container derived from it too, legible (contrast of at least 3:1) in both light and dark for any color.
- **Color changes are animated, as one animation.** Wherever a vehicle's color changes while it is on screen, one animated color drives
  everything that is derived from it, so all of it moves together and nothing jumps or runs out of step. The animation moves through the HCT color
  space (shortest way around the hue), respects the system's animation setting and does not animate the first display of a screen. The icons are the
  first users of it; `add-vehicle-color-theme` moves the whole screen with the same animation.
- One new dependency, **Material Color Utilities for Kotlin Multiplatform** (`com.materialkolor:material-color-utilities`: HCT and the
  quantizer), verified first (the first task is a go/no-go), with a small own implementation as the fallback behind an interface.

Out of scope: theming screens from the color (`add-vehicle-color-theme`, then the other vehicle-specific screens after it has been
proven), sliders or hex entry in the picker, storing a palette, backfilling existing pictures, picking a color from a spot of the photo,
a per-vehicle dark theme, syncing colors to a backend, and verifying iOS on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities
- `vehicle-color`: the color of a vehicle (mandatory, default from the theme), the preset picker, the color taken from a picture, icons drawn from
  the color, animated color changes, and storage.

### Modified Capabilities
- `vehicles`: adding a vehicle takes a color (preselected), and the edit screen lets the user change it (`Add a vehicle`, `Edit a vehicle`).

## Impact

- `shared/` commonMain: `Rgb` and the twelve presets with `DefaultVehicleColor`, `Vehicle.color` (non-null), `VehicleTones` (icon tint and container by HCT
  tone), `ColorExtractor` (behind an interface, library-backed and a pure fallback), the HCT interpolation and `rememberAnimatedColor`, the
  `VehicleColorChoice` composable, `color` and `pictureColor` in the add and edit states and processors, the color in the list and details view states,
  `VehiclePicture` and the type tiles drawing from the color, `SqlDelightVehicleRepository` writing and reading `vehicle_color`, migration `5.sqm`.
- Platform code: `ImageCodec.sample` (decode a stored image to a bounded array of ARGB pixels; Android `ImageDecoder`, iOS ImageIO).
- Dependency: `com.materialkolor:material-color-utilities` (Apache 2.0), to be verified against Kotlin 2.4.20 and the Android and iOS targets as the first task.
- `maestro/`: flows for choosing a color, a solid-color picture giving its color, saving, restarting, rotating; `openspec/config.yaml` notes the mandatory
  vehicle color, the icons and the rule that live color changes are animated as one.
