# Design

## Context

- Builds on the archived `add-vehicle-picture` (the picture draft, `PictureDraftEditor`, `ImageCodec`, the small and large stored versions, the repository transaction that writes `picture_id`)
  and `add-vehicle-type` (`VehicleType`, the type choice, `VehiclePicture(uri, type, …)` and `VehicleIcons`, migration `4.sqm`, schema version 5).
- The app theme is Petroleum (light and dark schemes, dark header; see `ui/theme`). Its main color is Oil Slick Blue `#203A43`, the light scheme's primary.
- Rules that shape this: stored data is locale-agnostic; business logic and tests live in commonMain; every behavior is specified first; Android is primary; text contrast at least 4.5:1 and controls
  and icons at least 3:1; the theme is where colors come from, and this change adds the one place where a color is data (the vehicle's).
- Replaces the `add-vehicle-palette` proposal. What changed, and why: **one color instead of four** (Material derives the rest from a seed, so derived colors are never stored and there is no palette
  format, no selection or fill rules and no second library), a **mandatory color with a theme default** instead of an optional one (no "no palette" state, no backfill), a **preset picker** instead of a
  slider picker, and a **consumer in the same change** (the icons, and the animation) so the result is visible and the design is tested against use.

## Goals / Non-Goals

**Goals:** a mandatory vehicle color; a preset picker; a color taken from the photo at crop confirmation; icons drawn from the color with guaranteed contrast; one orchestrated animation for live color changes,
ready for `add-vehicle-color-theme` to reuse; the libraries verified before they are relied on.

**Non-Goals:** theming screens (next change), sliders or hex entry, a stored palette, backfilling pictures, an eyedropper, a per-vehicle dark theme, sync, iOS on a device.

## Decisions

### 1. Library: Material Color Utilities (HCT), verified first

`com.materialkolor:material-color-utilities` is the Compose-free Kotlin Multiplatform port of Google's Material Color Utilities (HCT color space, `Hct`, `TonalPalette`, and later `DynamicScheme`; its quantizer is not used, see decision 5). The Android Palette
API (`androidx.palette`) is not multiplatform (it works on `android.graphics.Bitmap`), and the multiplatform ports of it are not needed any more, because we extract **one** color, not a swatch set. *Verify* the coordinates and version on
Maven Central. Task 1.1 is a **go/no-go before anything is built on it**: it resolves and compiles for `androidTarget`, `iosArm64` and `iosSimulatorArm64` with Kotlin 2.4.20 and Compose Multiplatform 1.12.0; a smoke test builds an `Hct`, reads it and
rebuilds it; the APK growth is recorded. The library appears in **one file** (`HctColors`, which wraps `Hct` for reading and building colors, tones and hue arithmetic) so a failure means a
replacement of that file by a small own implementation (sRGB to CAM16 or, cheaper, OKLCH: tone as OKLab lightness, hue and chroma as polar coordinates), with the same tests and no spec change; the spec states behavior, not library.

**Verified (task 1.1):** version **5.0.1** (the latest stable on Maven Central; the search index still lists 3.0.0-beta01, so read `maven-metadata.xml`). It resolves and compiles for `androidTarget`, `iosArm64` and `iosSimulatorArm64`
with Kotlin 2.4.20 and Compose Multiplatform 1.12.0, `Hct.fromInt`/`Hct.from` round-trip a color (`ColorLibrarySmokeTest`), and the debug APK grows by about 384 KB (unminified; a release build
shrinks it). No fallback needed. The package used is `com.materialkolor.hct`.

### 2. The value, the presets and the default

`Rgb(argb: Int)` (opaque, alpha ignored) with `hex` (`RRGGBB`, upper case) and `Rgb.parse(text): Rgb?` (strict: exactly six hex digits, either case). `VehicleColors.default = Rgb(0x203A43)` is **pinned to the theme**: a test asserts it equals the light scheme's `primary`, so
the two cannot drift; the migration default literal is asserted equal to it in the JVM migration test. Changing the theme's main color later does not change stored colors (a stored color is the vehicle's, not the theme's), which is intended.

`VehicleColors.presets`: twelve `(name, Rgb)`, the first the default. The set is a starting point, tuned in review (the spec fixes only "twelve, named, the first is the default"): Petroleum `203A43`, Teal `00796B`, Green `43A047`, Blue `1E88E5`,
Indigo `3949AB`, Purple `8E24AA`, Magenta `D81B60`, Red `E53935`, Orange `FB8C00`, Brown `6D4C41`, Graphite `455A64`, Silver `9E9E9E`. Fuel Gauge Gold and Road Trip Emerald are **left out** on purpose: they mean fuel and distance in the
domain colors, and a vehicle color that looks the same would blur that. A test checks every preset has a distinct hex and name, and that every one passes the icon contrast test of decision 4.

### 3. Data

Migration `5.sqm`: `ALTER TABLE vehicle ADD COLUMN vehicle_color TEXT NOT NULL DEFAULT '203A43';`, schema version 6, the same declaration in the fresh schema (the same pattern as `vehicle_type`). `Vehicle.color: Rgb` is non-null; the repository
maps `Rgb.parse(vehicle_color) ?: VehicleColors.default`, so an invalid value never fails a read. `addVehicle(…, type, color, …)` and `updateVehicle(…, type, color, picture)` take a **non-null** `Rgb`; the color is written in the same statements and
transaction as the rest of the vehicle. Existing vehicles get the default from the column default: **no backfill and no extraction for existing pictures**.

### 4. Icon tones (contrast by construction)

`VehicleTones.of(color: Rgb, dark: Boolean)` returns `icon` and `container` colors, computed in HCT with the color's hue and (capped) chroma at fixed tones: light scheme icon tone 40 on container tone 90, dark scheme icon tone 80 on container tone
30, which are Material's tones for `primary` on `primaryContainer`, so `add-vehicle-color-theme` can take the same colors from a full scheme without the icons changing. A difference of 50 tone steps gives a contrast of about 4.5:1 by construction (Material's tone rule), so the 3:1 requirement holds for every hue and chroma; a test **sweeps** all presets, white, black, greys and a grid of hues and chroma
in light and dark and computes the actual contrast with the same `contrastRatio` function `ThemeColorsTest` uses. Chroma is capped (about 48) so a very saturated color does not produce a garish container; greys and white keep chroma near zero
and give neutral tones (a white vehicle is a neutral icon, not a colorful one). `VehiclePicture` and the type tiles take the tones instead of the theme's `surfaceVariant` and `onSurfaceVariant`.

### 5. Extraction: one color from the cropped photo

`ImageCodec.sample(bytes, maxSide = 128): PixelSamples?` (Android: `ImageDecoder` with a target size then `getPixels`; iOS: ImageIO thumbnail drawn into an RGBA buffer, converted to ARGB; null for bytes that are not an image) reads the **stored small version**
(at most 256 px) of the confirmed crop, so the color is tied to what the user confirmed and the work is small (at most 16 K pixels). `ColorExtractor.extract(samples): Rgb?` (an interface, the library behind it):
1. Only the central 80% of the square is used (a 10% margin each side), a deterministic stand-in for "the middle counts more".
2. Pixels with alpha below 128 are dropped; fewer than 1% opaque gives `null` (the color stays as it was).
3. The opaque pixels are counted in a **histogram of 16 levels per channel** (4096 bins), each pixel with a **weight**: a Gaussian around the middle of the counted region (`exp(-2 (dx^2 + dy^2))`, about 0.135 at the middle of an edge) times `1 + 6 x chroma` (chroma is `(max - min) / 255` of the pixel's channels, 0 for
   a grey), so the middle counts more and a vivid pixel weighs up to seven times a grey one; the **heaviest bin** wins (ties go to the lower `RRGGBB` bin, so the result is deterministic) and the color is the **average of the pixels in that bin**, which is exactly the color for a flat one. The weights are summed as whole numbers, so equal weights are exactly equal and a tie is a tie.

The library's `QuantizerCelebi` was tried first and left out: measured while applying this change, for an image with few distinct colors and a lopsided split (75/25 or 90/10 of two colors, 1000 pixels or more) it merged both into one in-between color, which is wrong for "the
most common color". The histogram is short, exact and deterministic; real photos with gradients spread a paint color over several neighbouring bins, which is one reason the result is shown and can be changed, and a neighbourhood-weighted bin is the first refinement to try.

**Tuned on real photos.** The plain histogram (the most populated bin) was tried on nine street photos of cars and motorcycles (a centered square, as the crop starts) and gave the background for six of them (asphalt, sky, shadow; three of nine were right). Weighting the middle (Gaussian, falloff 2) and vivid
colors (boost 6) gave the right color for the red car, the blue car, the grey car, the white car and a black motorcycle, and it is what is used; a parameter sweep (falloff 0 to 3, boost 0 to 20, a dark-pixel penalty) showed a wide plateau around it, so the values are not fragile. The photos where the vehicle is a small part of the
frame (a motorcycle at the edge, a rider in front of the bike) still give the background; a tighter crop, which the user makes on the crop screen, fixes it, and in every case the result is shown as the selected "Photo color" segment and can be changed in one tap, which is why the result is shown and never silently applied. Better
selection (background-aware, a neighbourhood-weighted bin) is a later refinement that changes no storage or spec. Extraction runs in the processor right after the crop is confirmed (`PictureDraftEditor.sampleColor` reads the pending small version, samples it through the codec, which works off the main thread on Android, and extracts): it is a single pass over at most 16 K pixels, about a millisecond, so it needs no dispatcher of its own, and it stays synchronous in tests. Its result is a serializable `Rgb` in the state.

### 6. State, processors and the picker

Add and edit states gain `color: Rgb` (the add state starts with the default; the edit state starts with the saved color, and is not shown until it has loaded) and `pictureColor: Rgb?` (set only by a confirmed crop). Intents: `ColorSelected(rgb)`. When a crop is confirmed
the processor samples and extracts off the main thread and then reduces `pictureColor` and `color` together (a non-null result; `null` changes nothing); cancelling a crop or removing the picture changes neither `color`, and removing the picture sets `pictureColor = null`.
`save` passes `color` to the repository. The list and details view states carry `color: Rgb` next to `type` and `pictureUri`.

The edit state also has `savedColor: Rgb?` (the vehicle's color when the form loaded, null until then; it never changes while the form is open), which is the "Old color".

`VehicleColorChoice(color, pictureColor, savedColor, onSelect)` is one composable used by both forms, in two parts under the title "Vehicle color":
1. the **palette**: a `FlowRow` of 44 dp circular swatches, the twelve presets (`selectableGroup`, radio semantics, the selected swatch with a ring and a check, `contentDescription` = the name), test tags `vehicle_color_<HEX>` (`vehicle_color_203A43`);
2. beneath it, **one full-width row of segments** (a `Row` of equal `weight(1f)` boxes, 56 dp high, 12 dp rounded, with a 1 dp outline and, when selected, a 3 dp primary ring and a check), each filled with its color and carrying its **label as text on the color** in black or white (whichever reads better on that color, at least 4.5:1). In order: **"Old color"**
   (edit screen only: `savedColor`, tag `vehicle_color_old`), **"Photo color"** (`pictureColor`, tag `vehicle_color_picture`) and **"Current color"** (`color` when it is not a preset and equals neither of the others, tag `vehicle_color_current`). With no segment the row is absent. The number of segments is 0 to 3, all the same width.
`colorChoice(color, pictureColor, savedColor)` is the pure function that builds both parts and the **selection rule**: exactly one element is selected, a preset's swatch when `color` is that preset, otherwise the first of the segments Photo, Old, Current whose color is `color`. (When a saved or photo color equals a preset, the preset is the one shown selected.)
The segment of the photo color animates its own color through `rememberAnimatedColor` only when a newer photo replaces it, so it changes in real time only when a photo is added, and the vehicle color it sets moves in the same 300 ms as the rest of the screen. The whole choice sits after the type choice and before the odometer unit on both forms.

### 7. The single animated color

`rememberAnimatedColor(target: Rgb): Rgb` (in `ui/color`) is the one primitive. It holds one `Animatable<Float>` progress from 0 to 1 and the two endpoints (`from`, `to`); when `target` changes it sets `from` to the **currently shown** color and `to` to the target and animates the progress
(`tween(300, FastOutSlowInEasing)`); the shown color is `lerpHct(from, to, progress)`, computed in HCT (hue along the shortest arc, chroma and tone linear, endpoints returned exactly). The first composition shows `target` with no animation; the system's animator duration scale
is respected (a scale of 0 gives an immediate change) by reading `LocalMotionDurationScale`/the platform scale as Compose does for other animations. **Rule for consumers:** a screen calls it **once** with the form's color and passes the animated color down; nothing below
animates a color of its own, which is what keeps everything in step (the theme change reuses the same animated color). Screens that only show saved vehicles (list, details) call it per item or per screen the same way, so a saved change reaching them also animates. The pure part
(`lerpHct`) is unit-tested (endpoints, the shortest hue arc across 0/360, tone and chroma linear, greys); the Compose part is verified on the emulator (a frame sequence, task 4.4).

### 8. Testing

- commonTest: `Rgb` parse and hex (strict), presets (distinct, first is the default, all legible), `VehicleTones` contrast sweep, `lerpHct`, the extractor through the real library on synthetic pixels (solid, two halves, white with a little dark grey, a red square in a blue border,
  transparent, fully transparent, twice for determinism) and a fake `ColorExtractor` for the processors; repository on real SQL (each color read back, an edit changes it, an invalid stored value reads as the default, the log unchanged, a failed save changes nothing); JVM migration tests
  (1 to 4 to 6: the vehicle is intact and has the default color, the database rejects a null color, a fresh version-6 schema, the literal default equals `VehicleColors.default`); processors with kide-test (choose, the picture color appears and is selected, choosing a preset keeps the picture color offered, cancel and removal
  keep the color, the edit state's saved color, rotation restore, save arguments, leaving without saving); the extractor on synthetic images (a vivid color over a larger area of grey, the middle over the edge) and, as a recorded check rather than a test, on the street photos.
- Maestro on the emulator: choose a preset (checked swatch, the icon preview changes), a generated solid-color picture (`addMedia`) shows the "Photo color" segment selected, choosing a preset and going back to the photo color, save and reopen the edit screen ("Old color" shows the saved color and is selected when it is not a preset, "Old color" and "Photo color" side by side after a new photo), restart, rotation. Colors themselves are not readable by Maestro; the tint is checked in the unit tests and by screenshots.

## Risks / Trade-offs

- **Library API and target support are unverified.** Mitigated by task 1.1, the single wrapper file and the own fallback.
- **The most populous color may be the background.** Shown, not hidden, and one tap to change; a smarter pick is a later refinement.
- **Existing vehicles all get the default color.** Their pictures do not change it until the user acts; the alternative (a startup backfill) was rejected as a lot of code for data that is cosmetic and easy to set.
- **The default is a literal in SQL.** A migration cannot call Kotlin; a test ties the literal to the constant, and the constant to the theme.
- **Animating a full scheme per frame (next change) costs.** Icons alone are cheap; the theme change measures it and can derive the scheme in fewer steps if needed.
- **Twelve presets are a guess.** Tuned in review without a spec change.
