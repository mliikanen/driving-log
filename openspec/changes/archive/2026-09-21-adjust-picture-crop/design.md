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
2. **Non-gesture controls.** Buttons: zoom in, zoom out (steps of 25% of the frame's zoom, clamped by `zoomBy`), Rotate photo (one quarter turn clockwise, decision 6) and Reset (`CropState.initial`), as `IconButton`s with `contentDescription`s in a row under the photo (portrait) or a column beside it (landscape); **each acts once per tap** (decided: no repeat while held).
   **No move buttons** (decided): the photo is moved by dragging. The arrow keys (photo moves in the arrow's direction) and plus and minus on a hardware keyboard remain, through `onKeyEvent` on the focusable frame, using `CropState.movePhoto` and the zoom steps, so keyboard users can still move the photo. The maths is the existing `panBy` and `zoomBy`, so the limits and "the frame stays inside" hold with no new rule.
3. **Dimmed context.** Draw the whole photo at the frame's scale, clipped to the canvas, then a scrim (black at 60%, decided; no list-size preview) over everything outside the frame, and the frame's outline. The photo is drawn at the current scale (`frame / crop.side` screen pixels per photo pixel), so this costs one more `drawImage`, with `FilterQuality.Medium` as now.
4. **State survives rotation and process death.** Save the crop with a `Saver` (`zoom`, `centerX`, `centerY` as floats plus the image size, and the `quarterTurns`) via `rememberSaveable(image)`, restoring it only when the image is the same size (the decoded photo is rebuilt from the pending source after a restore, `PictureRefresh`). Zoom and center are in photo pixels, so they mean the same on any screen size.
5. **Output unchanged in form** (a square scaled to the stored sizes; a rotation only changes which pixels the square holds). `onConfirm(crop.rect(), quarterTurns)` gives the `CropRect` (in the pixels of the photo as turned) and the turns; `pictureSides` still limits the versions to 256 and 1024 and never enlarges. No storage, schema, format or dependency change.

6. **Rotate photo: a quarter turn clockwise, one button (decided: a rotate control is included).** The photo's own orientation is already applied on decode; this control is for a photo that is sideways anyway. The turn is part of the crop screen's state (`quarterTurns` 0 to 3, saved with the crop):
   - `CropState.rotatedClockwise()` gives the state for the photo turned, with the frame over the same content: the image is `height x width`, the zoom is the same (the shorter side is the same), and the centre ( `cx`, `cy` ) becomes ( `height - cy`, `cx` ). It is pure and tested (four turns give the state back; the frame stays inside).
   - The screen draws the **turned** bitmap: `DecodedImage.turnedClockwise(quarterTurns)` (Android: `Bitmap.createBitmap` with a rotation `Matrix`; iOS: a `CGContext` draw), made when the turn changes, at most one more bitmap of the bounded decode (about 28 MB) alive while the crop screen is open.
   - The confirmed crop is in the pixels of the turned photo, so `CropConfirmed` carries `quarterTurns`, and `ImageCodec.encodeSquare(bytes, crop, sides, quarterTurns)` decodes, turns and then crops exactly as the screen showed. The color comes from the small version, so it follows the turn with no other change.
   - Why not store the turn in the picture: the stored files are the result; nothing about a turn is kept, so there is no schema or storage change.
   - Alternatives: two buttons (left and right; costs a control for a rare need, at most three taps the other way) and rotating by the codec only at confirm (the screen would not show the result: rejected).

8. **The app bar follows the app's theme and Material Design** (decided): the crop screen's chrome is a `Scaffold` like every screen's, with `TopAppBar` in `drivingLogTopAppBarColors()` (the dark header), the shared `BackButton` as its navigation icon, the title "Crop the photo" and **"Use photo" as a text action** (`headerTextButtonColors()`), as the add screen has "Save". **There is no "Cancel" button**: back navigation (the arrow, and the system's back through the dialog's dismiss request) cancels. The buttons under (portrait) or beside (landscape) the photo are laid out by orientation: "Zoom out" left of "Zoom in" in a row; in landscape "Zoom in" above "Zoom out" in a column, then Rotate photo and Reset.

7. **Edge to edge.** The crop screen is in a `Dialog`, whose window is laid out inside the system bars unless it opts out: an `expect fun cropDialogProperties()` (Android: `usePlatformDefaultWidth = false` and `decorFitsSystemWindows = false`, iOS: `usePlatformDefaultWidth = false`) makes the window fill the screen. The screen is two layers: a **full-window canvas** (black background, the photo, the scrim and the frame, and the gestures) and, above it, the controls in a column with `safeDrawingPadding()` (title, buttons, Cancel and Use photo), so the interactables keep clear of the bars and cutouts while the photo reaches every edge. The frame is centred in the free space the controls leave (a spacer in the padded column reports its bounds to the canvas).

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
- **Landscape layout** of the controls on small screens (the crop screen is already scroll-free): four buttons (zoom in and out, rotate, reset) go beside the photo and are checked on the emulator.
- **The turned bitmap costs memory** (one more bounded decode while the crop screen is open): the previous one is released when the turn changes.
- **iOS scaling** is unverifiable until the Xcode project exists.

## Verified on the emulator (tasks 3.1 to 3.3)

- **The `picture` group** (`maestro/run.sh picture`, now with a sixth step, `crop-controls`) passes: add, replace, remove and cancel with their file checks, the camera, and the crop screen's controls (all nine named, the buttons, the crop screen open in portrait and landscape after a device rotation, a turned crop confirmed).
- **The crop survives a device rotation:** with the photo turned and zoomed, rotating the device to landscape and back gives an identical frame area (mean pixel difference 0.0; it was 22.9 before panning and 0.0 back at the start before this change, i.e. reset).
- **The turn:** "Rotate photo" turns the photo clockwise with the frame over the same content (a photo of a car, front to the left, has its roof to the right after one turn), and the stored picture and the photo color come from the turned photo.
- **Layouts:** portrait (two rows of buttons under the photo) and landscape (two columns beside it) both fit with every control reachable; the parts of the photo outside the frame are dimmed (a 60% scrim), the crop screen is a fixed black surface so light and dark mode look the same.
- **Photos of other shapes:** a tall photo (500 x 1200) zooms and moves inside the photo with the dimmed context above and below; a photo smaller than the minimum frame (100 x 100) shows whole and "Zoom in" has no effect, without an error.
- **Not verified:** iOS (the klib compiles, `:shared:compileKotlinIosSimulatorArm64`, but there is no Xcode project to run it), and an aliasing check of the scaled versions on a fine pattern (the stepwise halving is a pure, tested function, `downscaleSteps`, used by Android; the visual check needs Roborazzi or a bitmap test, neither adopted yet).
- **Edge to edge and the app bar (tasks 2.4 and 2.5), checked in portrait and landscape:** the black background and the photo reach every edge behind the status bar, the navigation bar and the cutout side; the top app bar is the app's dark header with the back arrow, the title and "Use photo" as its action, its insets like the other screens'; the four buttons are clear of the bars (a row under the photo in portrait, "-" left of "+"; a column beside it in landscape, "+" above "-"). The `picture` group passes with `crop-controls` updated (no move buttons, no "Cancel": back navigation cancels and leaves the form as it was).
- **Gestures (found by the developer, tasks 3.4 and 3.5):** after the Scaffold was laid over the canvas (task 2.5), drag and pinch did nothing: a Material `Scaffold` is a `Surface`, which takes the pointer events that reach it, so the detector on the canvas below never saw them. Reproduced with `adb shell input swipe` on the crop screen (frame area difference 0.0), fixed by putting `detectTransformGestures` on the parent layer (a parent still sees what no child consumed, and buttons consume their own taps), and now guarded twice: the maths by unit tests (`transformedBy`, `cropFrame`) and the touch path by `picture/run.sh` step 7 (a swipe moves the photo, 80.3; the dragged frame survives a device rotation, 0.0).
- **Not automated:** a two-finger **pinch** on the emulator (`adb shell input` has no multi-touch, and Maestro no pinch command); its maths is unit tested, its wiring is the same `detectTransformGestures` call that the swipe check exercises. A Compose integration test (not adopted yet) is where a pinch would be tested end to end.
