# Design

## Context

- This change is applied after `add-vehicle-picture`. From it we build on: `Vehicle.pictureId`, the picture draft (`None | Unchanged | Removed | Pending`) held in the add and
  edit states and its `PictureDraftEditor`, `ImageCodec` (platform decode and encode; the crop is confirmed into a small (at most 256 px) and a large version), the
  `VehiclePictureStore` (files) and the repository transaction that writes `picture_id`, schema version 4.
- The project rules that shape this: stored data is locale-agnostic and platform-independent; business logic and tests live in commonMain; every state-changing behavior is
  specified first; Android is primary, iOS opportunistic. The palette is *data for a later theming change*: it must be stable, deterministic and role-neutral, and
  nothing visible is themed by it now.
- The user asked whether Android's Palette API is multiplatform-compliant and, if not, for a library that can do the extraction (naming material-color-utilities as an example).

## Goals / Non-Goals

**Goals:**
- Extract four good, distinct, deterministic colors from the confirmed crop; derive four similar colors from a chosen main color; store them with the vehicle; show them on the forms.
- Keep the library choice swappable and verified before it is relied on; keep the colors and the rules for them in plain common code that tests can run.

**Non-Goals:**
- Theming any screen, dark themes per vehicle, editing the four colors one by one, an eyedropper on the photo, several palettes, sync, iOS verification on a device.

## Decisions

### 1. Which library extracts the colors (the question asked)

**Android's own Palette API (`androidx.palette:palette`) is not multiplatform.** It is an Android library that works on `android.graphics.Bitmap`; it cannot be used from `commonMain` or on iOS.
There are, however, Kotlin Multiplatform options. What the research found (the searches were done while planning; every item marked *verify* is re-checked in task 1.1 before any code relies on it):

| Candidate | What it is | Multiplatform | Fit |
|---|---|---|---|
| **kmpalette `androidx-palette` module** (`jordond/kmpalette`, version 4.0, published as `com.materialkolor.palette:core`; *verify* coordinates) | A Kotlin Multiplatform **port of the Android Palette algorithm** (median cut, swatch targets Vibrant, Muted, Light/Dark variants, Dominant). Since 4.0 it is dependency-free: no Compose, no Skiko; `Palette.Builder` takes a plain `IntArray` of ARGB pixels plus width and height and downsamples itself. | Android, iOS (arm64, simulator arm64), desktop, browser (*verify* the iOS targets) | Exactly the API the user knows from Android, on pixel arrays we already produce. Deterministic (median cut has no randomness). |
| **Material Color Utilities, Kotlin Multiplatform port** (`com.materialkolor:material-color-utilities`, same family as MaterialKolor, 5.0.1 found; Apache 2.0; *verify*) | Google's HCT color space, `TonalPalette`, `DynamicScheme`, **`QuantizerCelebi` (Wu + WSMeans)** and **`Score`** (ranks colors by suitability as a theme seed). Compose-free artifact. Also reported: an official-repo KMP pull request (#76) and a separate KMM port (`msasikanth/material-color-utilities-kmm`), not used. | Android, iOS, JVM, JS/wasm | Best for *deriving* colors (HCT tones and hues are how Material builds related colors) and later for contrast and text colors. Its quantizer + `Score` is built to pick a **seed color**, not a four-color palette: `Score` drops low-chroma colors and returns a fixed fallback blue for a grey or white photo, which is wrong for a white car. |
| **MaterialKolor** (`com.materialkolor:material-kolor`) | Compose Multiplatform theme generation from a seed color, built on the artifact above. | Yes | It adds Compose and theme APIs. It is the natural tool for the *later theming* change, not needed to store a palette. |
| Own implementation (OKLab, center-weighted k-means) | About 200 lines of common Kotlin. | Trivially | No dependency and full control, but it re-invents what the libraries do and needs its own tuning; kept as the **fallback**. |

**Recommendation.** Use **two small, Compose-free libraries, each for what it is best at**, both from the same maintained family and both behind our own interfaces:
1. **Photos: the kmpalette `androidx-palette` port** (`PaletteExtractor` implementation). It is the multiplatform version of the Android Palette API, deterministic, and its swatch targets (vibrant, muted, light and dark
   variants, dominant) are the vocabulary the later theming needs (accent, header, background, text). Pixels come from our `ImageCodec.sample`.
2. **A chosen color: the `material-color-utilities` HCT** (`PaletteDeriver` implementation). HCT gives perceptually uniform tone and chroma, so "the same color family, lighter or darker, a neighbouring hue" is a
   few well-defined operations, and it will also compute readable text colors in the theming change.
Kept out on purpose: `Score` for the photo palette (seed-oriented, mishandles greys and whites) and MaterialKolor's Compose layer (belongs to the theming change).

**Guard rails, because the API details are unverified:** the libraries appear in exactly two files (`KmPalettePaletteExtractor`, `HctPaletteDeriver`); everything else uses our `Rgb` and `Palette` types and two interfaces:

```
interface PaletteExtractor { fun extract(samples: PixelSamples): Palette? }   // PixelSamples(width, height, argb: IntArray)
interface PaletteDeriver   { fun derive(main: Rgb): Palette }
```

Task 1.1 adds the dependencies to the version catalog and shows, before anything else is built on them, that (a) they resolve at the versions chosen and compile for `androidTarget` and `iosSimulatorArm64` (and `iosArm64`)
with this project's Kotlin 2.4.20 and Compose Multiplatform 1.12.0 (the libraries' recent releases track Kotlin 2.4.x; *verify* that the artifacts' metadata versions are readable by 2.4.20), (b) a smoke test
extracts the expected colors from a synthetic image and gives the **same result twice** (determinism), and (c) the app's dependency size and method count grow by an acceptable amount. **If either library fails the check,
its implementation is replaced by the own implementation behind the same interface** (decision 2 describes the algorithm), and the spec does not change, because it states behavior, not library.

### 2. Extraction: sampling, the central region, the selection, the fill

Input is the **stored small version** of the confirmed crop (or, for a backfill, the stored small file), read by `ImageCodec.sample(bytes, maxSide = 128): PixelSamples?` (Android: `ImageDecoder` with a target size, then pixels; iOS: ImageIO
thumbnail drawn into an RGBA buffer). Using the small version keeps the palette tied to the confirmed crop and makes the extraction cheap (at most 16 K pixels).

`extractPalette(samples)` (common code, using the library through the interface):
1. **Central weighting without weights.** Only the central 80% of the square (a margin of 10% each side) is passed on. Median cut has no per-pixel weights, and dropping the border is deterministic and simple. (The 80% is tuned with the test photos; the
   Palette algorithm's own downsampling is nearest-neighbour, so this is a cheap crop of the array.)
2. **Transparent pixels.** Pixels with alpha below 128 are not colors. If fewer than 1% of the pixels are opaque there is no palette (`null`). Otherwise transparent pixels are replaced by the average opaque color, so they add weight to nothing new.
3. **Keep whites, blacks and greys.** `Palette.Builder` is built with `clearFilters()`: the default filter of the Android algorithm removes near-white, near-black and near-red-I-line pixels, which would remove the main color of a white or black vehicle.
   Maximum color count 16.
4. **Select four.** Candidates in priority order: the dominant swatch, then vibrant, dark vibrant, light vibrant, muted, dark muted, light muted, then the remaining swatches by population. A candidate is taken when its perceptual distance (CAM16-UCS ΔE from `material-color-utilities`, threshold about 10, tuned in tests)
   from all colors already taken is above the threshold. **The result is ordered by selection, most representative first.**
5. **Fill.** If fewer than four were taken, the rest come from the same generator as the derived palette (decision 3) applied to the first color, starting with its lightness variations, so there are always four different colors.
   (The spec scenario "a solid color photo" gives the color itself first and three lightness variations.)

A `Palette` is `4 x Rgb` (opaque ARGB with the alpha dropped). `Rgb` has `hex` (`RRGGBB` upper case) and the conversions the picker needs; the `Palette`'s text form for storage is `RRGGBB,RRGGBB,RRGGBB,RRGGBB`.

**The pure fallback** (used if a library fails task 1.1): convert samples to OKLab, run a deterministic k-means with `k = 8` (initialization by farthest-point from the mean, a fixed number of iterations, no randomness), weight by population, then apply steps 4 and 5 above. Same interface, same tests.

### 3. Deriving the palette from a main color

`derive(main)` works in HCT (hue, chroma, tone) and is deterministic:
- slot 0: the chosen color, exactly (converted back through the original ARGB, never through HCT, so the value is not rounded);
- candidates, in this order: **analogous** (hue + 30°, same tone and chroma), **tonal** (same hue; tone moved 35 toward the far end from 50, chroma x 0.7), **soft** (same hue, chroma 8, tone moved to 92 or 20 by the same rule), **opposite-analogous** (hue - 30°),
  and then tone steps of the main color (± 20, ± 40, ± 55);
- a candidate is taken when it is farther than the distance threshold (decision 2 step 4) from those taken; the first three that qualify fill slots 1 to 3.

Whites, blacks and greys have almost no chroma, so hue candidates are near-duplicates of the main color and are skipped; the **tone steps always qualify**, which guarantees four different colors for any input, including `#FFFFFF` and `#000000`. The same generator fills gaps in the photo
palette. The candidate order and numbers are tested (the first color is the chosen one, four distinct colors for every preset and for a sweep of 360 hues x a few tones, determinism); the numbers may be tuned in review without a spec change.

### 4. Data: two nullable columns

Migration `4.sqm`: `ALTER TABLE vehicle ADD COLUMN main_color TEXT;` and `ALTER TABLE vehicle ADD COLUMN palette TEXT;` and schema version 5. `main_color` is `RRGGBB` or null; `palette` is the four `RRGGBB` values joined by commas or null. `Vehicle` gains `mainColor: Rgb?` and `palette: Palette?`. The vehicle queries carry both,
and the insert and update queries write them; the column values are text, so nothing depends on locale, platform or byte order. The invariant "a palette if and only if a picture or a main color" is kept by the single function that decides it (decision 5) and asserted in repository tests.

### 5. Which palette a vehicle has, and when it is computed

`vehiclePalette(picturePalette: Palette?, hasPicture: Boolean, mainColor: Rgb?, deriver): Palette?` is the one rule: a picture's palette wins; else derive from the main color; else null. It is pure and tested on every combination.

**When the crop is confirmed** (`PictureDraftEditor` from the picture change) the editor writes the pending small version, samples it, extracts the palette and stores it in the draft (`Pending(pendingId, palette)`, serializable as the four hex values), so a rotation restores the swatches and Save never has to decode anything. Extraction runs off the main
thread (`Dispatchers.Default`). A `null` result (fully transparent image) leaves the picture without a palette; the vehicle then falls back to the main color's palette by the same rule.

**The main color** is part of the add and edit states (`mainColor: Rgb?`, serializable) and is not offered while the draft has a picture. Its palette preview is `derive(mainColor)` computed in the processor when the color changes (cheap, pure).

**At save** the processor computes the vehicle's palette with the rule above from the draft's picture palette (or the stored palette when the picture is unchanged, which is *never re-extracted*) and the main color, and passes `mainColor` and `palette` to the repository next to the picture change. The repository writes them in the same transaction as `picture_id`
(and the vehicle and its initial event on add). An edit that changes neither the picture nor the color passes the stored values unchanged, so the stored palette is kept exactly. The repository does not compute anything: it stores what it is given, so it stays free of image and color code.

### 6. Backfill for pictures that predate palettes

At app start, after the picture sweep of the picture change, `PaletteBackfill.run()` reads the vehicles with `picture_id` set and `palette` null, reads each vehicle's stored small file, samples and extracts, and writes only the `palette` column (one small update per vehicle, `updated_at` untouched, since the user did not edit the vehicle). A vehicle whose file cannot be read or extracted
is skipped and retried at the next start; one failure never stops the others or the app. It is idempotent (it selects on `palette IS NULL`) and runs off the main thread. A vehicle whose picture yields no palette (fully transparent) stays without one and is retried each start, which costs one small decode; acceptable.

### 7. The color picker and the swatches

`ColorPicker(color: Rgb?, onColor: (Rgb) -> Unit, onClear: () -> Unit)` is a small composable: a row of 12 preset swatches (white, silver, grey, black, red, orange, yellow, green, teal, blue, purple, brown) with a check on the selected one, a preview chip with `#RRGGBB`, a "Custom" area with three `Slider`s (hue 0 to 360, saturation 0 to 100, brightness 0 to 100)
and a "Clear color" action. The conversions between HSV and RGB are pure common functions (`Rgb.toHsv()`, `hsvToRgb`) with round-trip tests; the sliders' state is derived from the chosen color, and choosing a preset moves the sliders to it. No third-party color picker: the widget is small, has no gestures beyond a slider, is easy to test with Maestro, and keeps
accessibility simple.

`PaletteSwatches(palette: Palette)` shows four rounded squares (48 dp), each with `contentDescription = "Color #RRGGBB"` and test tags `palette_swatch_0` to `palette_swatch_3`; the picker has tags `color_preset_<hex>`, `color_hue`, `color_saturation`, `color_brightness`, `color_hex`, `color_clear`. The forms place them under the picture row: with a picture, the swatches and the text "Colors from the picture"; without one, the picker and the swatches.

### 8. Migration and testing

- Pure, in commonTest: `Rgb` and HSV conversions (round trip over a grid, primaries, greys), `Palette` text form (parse and format, rejects malformed), the selection and fill rules on hand-made swatch lists (with a fake `PaletteExtractor` backend), `derive` (first is the chosen color, four distinct colors for every preset and a hue sweep, white, black and mid grey, determinism), `vehiclePalette` on all combinations, extraction on synthetic pixel arrays through the real library (solid, two halves, white with a little dark grey, red square in a blue border, transparent, all
  transparent, a determinism check that runs it twice).
- Repository on real SQL: save with a palette and a main color, the invariant, an unrelated edit keeps the stored palette bytes, picture removal falls back to the color's palette, a failed transaction leaves the old values. Migration test 4 to 5 (and 1, 2, 3 to 5, fresh version 5). Backfill with a fake codec: fills, skips existing, an unreadable picture, idempotence.
- Processors with kide-test: pick a color, clear it, rotate (restore with a color and with a pending palette), the swatches after a confirmed crop, save arguments, leaving without saving.
- Maestro on the emulator (`addMedia` with generated solid-color images kept in `maestro/assets/`): choose a preset color and read `palette_swatch_0` (description `Color #...`); add a solid-color picture and read the swatches; the swatches after saving on the edit screen; removing the picture falls back to the chosen color's swatches; rotation with a color chosen.

## Risks / Trade-offs

- **Library API and target support are unverified.** Mitigated by the interfaces, the first task's check and the pure fallback; the spec is library-independent.
- **The dominant color may be the background** (a road, a wall) even after the user cropped and after the central weighting. The palette is a set of four colors with the most prominent first, not a claim about the paint. The 80% central region and the selection order are tuned on sample photos; a wrong "first color" is
  cosmetic now and can be improved without changing the storage.
- **The median cut has no seed, `WSMeans` (if the fallback or a future use needs it) does.** Determinism is a tested requirement; a library update that changes results would change new palettes only, never stored ones (they are kept, not recomputed, except the backfill of missing ones).
- **Two dependencies from one family** move together with Kotlin and Compose updates. Kept to the two Compose-free artifacts; MaterialKolor proper is left for the theming change.
- **Colors the eye finds close are compared by CAM16 distance,** which needs the `material-color-utilities` artifact even when only the photo path is used. It is in the build anyway for the derivation.
- **iOS:** the logic is common; `ImageCodec.sample` on iOS is compile-verified only until the Xcode project exists.
