# Design (stub)

## Context

`CropScreen` shows the decoded photo (bounded to 3072 px on its longer side) under a fixed square frame; `detectTransformGestures` pans and zooms, `CropState` (pure, in photo pixels) keeps the zoom between 1 (the shorter side fills the frame)
and `maxZoom` (a frame of at least 128 px) and the frame inside the photo, and `rect()` gives the `CropRect` the codec crops. `ImageCodec.encodeSquare` crops that square and scales it to 256 px and 1024 px (Android: halve while at least twice the size wanted
remains, then one smooth step; iOS: one `drawInRect`). The crop screen's `crop` is a plain `remember(image)`.

## Decisions

1. **Interaction: the photo moves and zooms behind a fixed square (kept).** One model, one on-screen frame size, so the maths stays pixel-exact and testable; touch targets are the whole screen (no small handles on a phone); it is the pattern users know from avatar croppers, and it
   already exists. **Alternative: a movable, resizable square over a fixed photo.** It shows the whole photo at once and suits landscape photos where the vehicle is at one side, but needs four corner handles and a body drag (small hit targets, harder for accessibility and one-handed use) and its on-screen size changes, so
   the pixel mapping changes with it. *Open question: is seeing the whole photo the real need?* The dimmed context (below) gives most of that benefit without changing the model.
2. **Non-gesture controls.** Buttons: zoom in, zoom out (steps of about 25% of the frame's side, clamped by `zoomBy`), move left/right/up/down (about 10% of the frame's side, clamped by `panBy`), Reset (`CropState.initial`). They are `IconButton`s with `contentDescription`s in a row under the photo
   (portrait) or beside it (landscape); they repeat while held. Keyboard: arrow keys move, plus and minus zoom (`onKeyEvent` on the focusable frame). The maths is the existing `panBy` and `zoomBy`, so the limits and "the frame stays inside" hold with no new rule.
3. **Dimmed context.** Draw the whole photo at the frame's scale, clipped to the canvas, then a scrim (black at about 60%) over everything outside the frame, and the frame's outline. The photo is drawn at the current scale (`frame / crop.side` screen pixels per photo pixel), so this costs one more `drawImage`, with `FilterQuality.Medium` as now.
4. **State survives rotation and process death.** Save the crop with a `Saver` (`zoom`, `centerX`, `centerY` as floats plus the image size) via `rememberSaveable(image)`, restoring it only when the image is the same size (the decoded photo is rebuilt from the pending source after a restore, `PictureRefresh`). Zoom and center are in photo pixels, so they mean the same on any screen size.
5. **Output unchanged.** `onConfirm(crop.rect())` gives the same `CropRect`; `pictureSides` still limits the versions to 256 and 1024 and never enlarges. No storage, schema, format or dependency change.

## The scaling question, answered plainly

*"Use a high quality scaling function to make scaling at render time cheaper"*: the premise does not hold as stated. The app stores **pre-scaled small and large files** and draws each from the nearest stored size, so **render time already involves little or no scaling** (the list decodes a 256 px file for a
56 dp tile, the details screen a 1024 px file for at most 280 dp). A better scaler at save time does not make rendering cheaper. What it does buy is **quality**: the downscale runs once, from up to 3072 px to 256 px, and a single big step aliases. So the point is doing that one scale well:

- **Android** already halves stepwise (`createScaledBitmap(filter = true)`) while at least twice the size wanted remains and then makes one smooth step: an area-averaging-like result. Nothing more is needed; a test on a fine pattern (a grille) guards it.
- **iOS** draws once with `drawInRect` and no explicit interpolation quality, which can alias on a large reduction. Set `CGContextSetInterpolationQuality(kCGInterpolationHigh)` (or halve stepwise as Android does) in the renderer. Verifiable only when the Xcode project exists (compile-checked now).
- Lanczos is an option where a library offers it, but nothing here needs a dependency for it; area averaging by stepwise halving is enough at these sizes.
- The screen's own drawing during the crop uses `FilterQuality.Medium` (mipmapped), which is fine for a live preview.

## Interplay with `add-vehicle-color`

The color is taken from the confirmed crop's **small version**, so the crop is now also the color's input: in the street-photo tests, cars centered in a square gave the right color, and photos where the vehicle is a small part of the frame gave the asphalt or sky; a tight crop, made easy by this change, fixes that.
The two changes touch different requirements and no code in common except that both read the confirmed crop; either can be applied first.

## Open questions

1. Fixed square with a moving photo (recommended), or a movable, resizable square (alternative)? Is the need to see the whole photo, which the dimmed context mostly gives?
2. Step sizes for the buttons (25% zoom, 10% move proposed) and whether they repeat while held.
3. Scrim strength, and whether the crop screen should show a small "how it will look" preview at the list size.
4. Is a "rotate photo" control wanted (a sideways photo whose EXIF is wrong)? Proposed out of scope.
5. On very large zoom-ins of small photos, warn that the picture will be soft (never enlarged, so a 128 px crop gives a 128 px picture)?

## Risks

- **Two definitions of the limits** if the buttons re-implement clamping: they do not, they call `panBy` and `zoomBy`.
- **Landscape layout** of the controls on small screens (the crop screen is already scroll-free): the buttons go beside the photo and are tested on the emulator.
- **iOS scaling** is unverifiable until the Xcode project exists.
