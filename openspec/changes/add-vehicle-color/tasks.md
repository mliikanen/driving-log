# Tasks

## 1. Library and color logic

- [x] 1.1 Verify the library before anything is built on it: add `com.materialkolor:material-color-utilities` (confirm the coordinates and the latest stable version on Maven Central first) to the version catalog and commonMain, and verify (a) it resolves and compiles for `androidTarget`, `iosArm64` and `iosSimulatorArm64` with Kotlin 2.4.20 and Compose Multiplatform 1.12.0, (b) a commonTest smoke test builds an `Hct`, reads its tone and round-trips a color, and (c) the debug APK growth is recorded; if any check fails, record why in the design and use the own OKLCH implementation behind the same wrapper instead
- [x] 1.2 Add `Rgb` (opaque, `hex` in upper case, strict `parse`), `VehicleColors.default` and the twelve presets, and verify unit tests: parse and hex round trip, rejected text (length, characters, `#`), the presets are twelve, distinct and named, the first is the default, and the default equals the light scheme's primary of the theme
- [x] 1.3 Add `HctColors` (the only file that touches the library: hue, chroma and tone reads and builds) and `VehicleTones.of(color, dark)` (icon and container at the fixed tones, chroma capped), and verify unit tests: the contrast of icon on container is at least 3:1 in light and dark for every preset, white, black, greys and a grid of hues and chroma, greys stay neutral, and the result is deterministic
- [x] 1.4 Add `ColorExtractor` (the central 80%, transparent pixels dropped, fewer than 1% opaque gives null, no chroma filter, a histogram of 16 levels per channel, the most populated bin wins with ties to the lower bin, the color the average of its pixels) as `HistogramColorExtractor`, and verify unit tests on synthetic pixels: a solid color, two halves (the larger), white with a little dark grey (white), a red square in a blue border (red), fully transparent (null), partly transparent, and the same input twice
- [x] 1.5 Add `lerpHct` (hue by the shortest arc, chroma and tone linear, exact endpoints), and verify unit tests: endpoints, the shortest arc across 0 and 360, the midpoint of tone and chroma, and greys

## 2. Data and platform

- [x] 2.1 Add migration `5.sqm` (`vehicle.vehicle_color TEXT NOT NULL DEFAULT '203A43'`), schema version 6, the same declaration in the fresh schema, `Vehicle.color` (non-null, an invalid stored value reading as the default) and the queries that select and write it, and verify the JVM migration tests from every previous version (the vehicle is intact and has the default color), that a fresh database has the same column, that the database rejects a null color, and that the default literal equals `VehicleColors.default`; update the schema-version test
- [x] 2.2 Give `addVehicle` and `updateVehicle` a required non-null `color`, updating the fake repository, and verify repository tests on real SQL: each preset is written and read back, an edit changes it, an invalid stored value reads as the default, the log is unchanged by a color edit, and a failed save changes nothing
- [x] 2.3 Add `ImageCodec.sample(bytes, maxSide): PixelSamples?` (Android: `ImageDecoder` with a target size and the pixels as ARGB; iOS: ImageIO thumbnail drawn into an RGBA buffer, converted to ARGB; null for bytes that are not an image) and a fake for tests, and verify it compiles for Android and iOS and that a stored picture is sampled on the emulator (see 5.1)

## 3. Form logic

- [x] 3.1 Add `color` and `pictureColor` to the add and edit states and `ColorSelected` to the processors, extract the color when a crop is confirmed (off the main thread, `null` changes nothing, cancel and removal keep the color, removal clears `pictureColor`) and pass `color` to the repository on save, and verify processor tests with kide-test: the default at first, choosing replaces it, the picture color appears selected after a confirmed crop and stays offered after choosing a preset, another crop replaces it, cancel keeps the color, removing the picture keeps the color, an unrelated edit keeps the saved color, save arguments, rotation restores color and picture color, leaving without saving changes nothing
- [x] 3.2 Carry the vehicle's `color` in the list and details view states next to the type and the picture URI, and verify processor tests that each state has the vehicle's color and follows a change

## 4. Screens

- [x] 4.1 Add `rememberAnimatedColor` (one progress, endpoints from the currently shown color, 300 ms, the first composition not animated, the system animator scale respected), and verify it compiles for Android and iOS and that the pure interpolation is covered by 1.5
- [x] 4.2 Add the `VehicleColorChoice` composable (twelve preset swatches, the "Picture color" and "Current color" extras, selection ring and check, names as labels, the test tags from the design) and use it on the add and edit screens, and verify it compiles for Android and iOS
- [x] 4.3 Draw `VehiclePicture` and the type tiles from `VehicleTones` of the (animated) vehicle color, with one call of `rememberAnimatedColor` per screen (per item on the list) and the animated color passed down, and verify it compiles for Android and iOS and that a picture still wins over the icon
- [x] 4.4 Check the color choice, the tinted icons and the animation by hand on the emulator (add, edit, list, details) in light and dark mode, in landscape and with the keyboard open, including a frame sequence of a color change and an interrupted one, and fix what looks wrong

## 5. Maestro flows

- [ ] 5.1 Add flows for choosing a preset (the swatch is checked, another one is not), a generated solid-color picture giving the "Picture color" swatch selected, choosing a preset and returning to the picture color, saving and reopening the edit screen (the color is selected; the "Current color" extra when it is not a preset), a restart, and a rotation with a color chosen, and verify they pass together with the flows that add a vehicle (the default color is preselected, so the subflow needs no change)

## 6. Project context and final verification

- [ ] 6.1 Add to the project context in `openspec/config.yaml` that every vehicle has a mandatory color (a `NOT NULL` `RRGGBB` column, default the theme's main color), that vehicle-specific colors are derived from it when drawn and never stored, and the convention that any real-time color change is animated as one animation driven by a single animated color (`rememberAnimatedColor`), and verify `openspec validate --all --strict` passes
- [ ] 6.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug`, the whole Maestro suite and `openspec validate --all --strict`, and verify all pass
