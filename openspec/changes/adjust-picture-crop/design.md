# Design

## Context

`CropScreen` shows the decoded photo (bounded to 3072 px on its longer side) under a fixed square frame; `detectTransformGestures` pans and zooms, `CropState` (pure, in photo pixels) keeps the zoom between 1 (the shorter side fills the frame)
and `maxZoom` (a frame of at least 128 px) and the frame inside the photo, and `rect()` gives the `CropRect` the codec crops. `ImageCodec.encodeSquare` crops that square and scales it to 256 px and 1024 px (Android: halve while at least twice the size wanted
remains, then one smooth step; iOS: one `drawInRect`). The crop screen's `crop` is a plain `remember(image)`.

**Verified on the emulator (task 1.1):** with the crop screen open, panning the photo changes the frame area (mean pixel difference 22.9), and after rotating the device to landscape and back the frame area is identical to the start (difference 0.0): the crop is reset by a rotation.

## Decisions

1. **Interaction: the photo moves and zooms behind a fixed square (kept; decided by the developer).** One model, one on-screen frame size, so the maths stays pixel-exact and testable; touch targets are the whole screen (no small handles on a phone); it is the pattern users know from avatar croppers, and it
   already exists. **Alternative: a movable, resizable square over a fixed photo.** It shows the whole photo at once and suits landscape photos where the vehicle is at one side, but needs four corner handles and a body drag (small hit targets, harder for accessibility and one-handed use) and its on-screen size changes, so
   the pixel mapping changes with it. The dimmed context (below) gives most of the benefit of seeing the whole photo without changing the model. Gestures (drag to move, pinch to zoom) stay next to the buttons.
2. **Non-gesture controls.** Buttons: zoom in, zoom out (steps of about 25% of the frame's side, clamped by `zoomBy`), move left/right/up/down (about 10% of the frame's side, clamped by `panBy`), Rotate photo (one quarter turn clockwise, decision 6), Reset (`CropState.initial`). They are `IconButton`s with `contentDescription`s in a row under the photo
   (portrait) or beside it (landscape); **each acts once per tap** (decided: no repeat while held). Keyboard: arrow keys move, plus and minus zoom (`onKeyEvent` on the focusable frame). The maths is the existing `panBy` and `zoomBy`, so the limits and "the frame stays inside" hold with no new rule.
3. **Dimmed context.** Draw the whole photo at the frame's scale, clipped to the canvas, then a scrim (black at 60%, decided; no list-size preview) over everything outside the frame, and the frame's outline. The photo is drawn at the current scale (`frame / crop.side` screen pixels per photo pixel), so this costs one more `drawImage`, with `FilterQuality.Medium` as now.
4. **State survives rotation and process death.** Save the crop with a `Saver` (`zoom`, `centerX`, `centerY` as floats plus the image size, and the `quarterTurns`) via `rememberSaveable(image)`, restoring it only when the image is the same size (the decoded photo is rebuilt from the pending source after a restore, `PictureRefresh`). Zoom and center are in photo pixels, so they mean the same on any screen size.
5. **Output unchanged in form** (a square scaled to the stored sizes; a rotation only changes which pixels the square holds). `onConfirm(crop.rect(), quarterTurns)` gives the `CropRect` (in the pixels of the photo as turned) and the turns; `pictureSides` still limits the versions to 256 and 1024 and never enlarges. No storage, schema, format or dependency change.

6. **Rotate photo: a quarter turn clockwise, one button (decided: a rotate control is included).** The photo's own orientation is already applied on decode; this control is for a photo that is sideways anyway. The turn is part of the crop screen's state (`quarterTurns` 0 to 3, saved with the crop):
   - `CropState.rotatedClockwise()` gives the state for the photo turned, with the frame over the same content: the image is `height x width`, the zoom is the same (the shorter side is the same), and the centre ( `cx`, `cy` ) becomes ( `height - cy`, `cx` ). It is pure and tested (four turns give the state back; the frame stays inside).
   - The screen draws the **turned** bitmap: `DecodedImage.turnedClockwise(quarterTurns)` (Android: `Bitmap.createBitmap` with a rotation `Matrix`; iOS: a `CGContext` draw), made when the turn changes, at most one more bitmap of the bounded decode (about 28 MB) alive while the crop screen is open.
   - The confirmed crop is in the pixels of the turned photo, so `CropConfirmed` carries `quarterTurns`, and `ImageCodec.encodeSquare(bytes, crop, sides, quarterTurns)` decodes, turns and then crops exactly as the screen showed. The color comes from the small version, so it follows the turn with no other change.
   - Why not store the turn in the picture: the stored files are the result; nothing about a turn is kept, so there is no schema or storage change.
   - Alternatives: two buttons (left and right; costs a control for a rare need, at most three taps the other way) and rotating by the codec only at confirm (the screen would not show the result: rejected).

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

## Questions settled before apply

1. Fixed square with a moving photo: **chosen** (the movable, resizable square is not built).
2. Buttons: the proposed steps (25% zoom, 10% move), **one action per tap, no repeat**; drag and pinch stay.
3. Scrim at about 60%, **no** list-size preview.
4. A **rotate-photo control is included** (decision 6).
5. **No** soft-picture warning.

## Risks

- **Two definitions of the limits** if the buttons re-implement clamping: they do not, they call `panBy` and `zoomBy`.
- **Landscape layout** of the controls on small screens (the crop screen is already scroll-free): eight buttons (zoom in and out, four moves, rotate, reset) go beside the photo and are checked on the emulator.
- **The turned bitmap costs memory** (one more bounded decode while the crop screen is open): the previous one is released when the turn changes.
- **iOS scaling** is unverifiable until the Xcode project exists.
