# Tasks

## 1. Crop state, turning and the codec

- [x] 1.1 Verify the current behavior first: on the emulator open the crop screen, move the photo, rotate the device, and confirm the frame resets (the crop state is a plain `remember`); record it in the design's Context
- [x] 1.2 Add to `CropState`: step functions for the buttons (zoom in and out by a step, move by a step in a direction, all through `zoomBy` and `panBy`), `rotatedClockwise()` (the image `height x width`, the same zoom, the centre `(cx, cy)` becoming `(height - cy, cx)`) and a save and restore for it; verify by unit tests: clamping at the photo's edges for every direction, the zoom limits on a landscape, a portrait and a square photo and on a photo smaller than the minimum frame, a zoom step and its inverse return to the same crop (within a pixel), the frame's `rect()` stays inside the photo after every step and turn, four turns return the state, one turn keeps the frame over the same content (the rect of a turned state equals the turned rect of the original), reset equals `initial`, and a saved state restores to an equal one (and is refused for a photo of another size)
- [x] 1.3 Carry the turn through the picture logic: `DecodedImage.turnedClockwise(quarterTurns)` and `ImageCodec.encodeSquare(bytes, crop, sides, quarterTurns)` (default 0), `CropConfirmed(crop, quarterTurns)` in the add and edit intents, `PictureDraftEditor.cropConfirmed(state, crop, quarterTurns)` and the fakes; verify by processor and editor tests that the turns reach the codec, that a confirmed turned crop gives the pending picture and the color from it, and that a crop with no turn behaves as before
- [x] 1.4 Implement the turn in the codecs: Android turns the decoded bitmap with a rotation `Matrix` before cropping (and in `turnedClockwise`), iOS with a `CGContext`; verified on the emulator instead of a JVM test (a bitmap needs Robolectric, which is not adopted yet: see docs/test-strategy.md): a photo turned on the crop screen is stored turned, upright as the crop showed it and with the color taken from it; and `./gradlew :shared:compileKotlinIosSimulatorArm64` compiles the iOS codec

## 2. The screen

- [x] 2.1 Save the crop and the turns with `rememberSaveable` in `CropScreen` (restored only for the same image size), and add the labelled buttons ("Zoom in", "Zoom out", "Move left", "Move right", "Move up", "Move down", "Rotate photo", "Reset"), one action per tap, and the keyboard handling (arrows, plus, minus), keeping drag and pinch; verify it compiles for Android and iOS and that every control has its accessibility label
- [x] 2.2 Draw the photo dimmed outside the frame (the whole photo at the current scale, a scrim of black at 60% outside the frame, the outline), and verify it compiles for Android and iOS
- [x] 2.3 Make the scaling explicit: keep Android's stepwise halving, set a high interpolation quality (or halve stepwise) in the iOS renderer, and verify a JVM-side test where possible (a stepwise-halving helper on a synthetic fine pattern gives no stripes, if the helper is made pure) and that iOS compiles

## 3. Flows and checks

- [x] 3.1 Run the `picture` group (`maestro/run.sh picture`) and verify its flows still pass with the new crop screen (the crop screen is another window without test tags: flows use its texts "Use photo", "Cancel")
- [x] 3.2 Add `maestro/picture/crop-controls.yaml` (a step of `picture/run.sh`): on the crop screen zoom in with the button, move with the buttons, rotate the photo, Reset, confirm, and rotate the device with the crop screen open (the crop screen stays open); verify it passes
- [x] 3.3 Check by hand on the emulator (light and dark, portrait and landscape, with a large photo, with a tall photo and with a photo smaller than the minimum frame) that the buttons, the turn, the dimmed context and the saved crop after a device rotation work and look right, and that a tight crop of a photo with a small vehicle gives the picture and (with `add-vehicle-color`) the color the user expects; fix what looks wrong

## 4. Final verification

- [x] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, and verify all pass (Maestro is not part of the final regression run; the `picture` group was run in 3.1 and 3.2)
