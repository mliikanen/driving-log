# Tasks (stub)

## 1. Crop state and controls

- [ ] 1.1 Verify the current behavior first: on the emulator open the crop screen, zoom and move, rotate, and confirm that the frame resets (the crop state is a plain `remember`); record it in the design
- [ ] 1.2 Add step functions to `CropState` for the buttons (zoom in and out by a step, move by a step in a direction, all through `zoomBy` and `panBy`) and a `Saver` for it, and verify unit tests: clamping at the photo's edges for every direction, the zoom limits (no zoom below 1 or above `maxZoom`, on a landscape, a portrait and a square photo, and on a photo smaller than the minimum frame), a zoom step and its inverse return to the same crop (within a pixel), the mapping to a `CropRect` always stays inside the photo, reset equals `initial`, and a saved state restores to an equal one (and is refused for a photo of another size)
- [ ] 1.3 Save the crop with `rememberSaveable` in `CropScreen` (restored only for the same image size), and verify by the emulator check of task 3.3 that rotation keeps zoom and position

## 2. The screen

- [ ] 2.1 Add the labelled buttons (zoom in, zoom out, move left, right, up, down, Reset) beside or under the photo, repeating while held, and the keyboard handling (arrows, plus, minus), and verify they compile for Android and iOS and that every control has its accessibility label
- [ ] 2.2 Draw the photo dimmed outside the frame (the whole photo at the current scale, a scrim outside the frame, the outline), and verify it compiles for Android and iOS
- [ ] 2.3 Make the scaling explicit: keep Android's stepwise halving, set a high interpolation quality (or halve stepwise) in the iOS renderer, and verify a JVM-side test where possible (a stepwise-halving helper on a synthetic fine pattern gives no stripes, if the helper is made pure) and that iOS compiles

## 3. Flows and checks

- [ ] 3.1 Update the Maestro flows that use the crop screen (15, 16, 17, 18 and `picture/*.yaml`; the crop screen is another window without test tags, so use the texts "Use photo", "Cancel" and the new button labels), and verify they still pass
- [ ] 3.2 Add a flow: on the crop screen zoom in with the button, move with the buttons, Reset, confirm, and rotate with the crop screen open (the crop screen stays open); verify it passes
- [ ] 3.3 Check by hand on the emulator (light and dark, portrait and landscape, with a large photo, with a tall photo and with a photo smaller than the minimum frame) that the buttons, the dimmed context and the saved crop after rotation work and look right, and that a tight crop of a photo with a small vehicle gives the picture and (with `add-vehicle-color`) the color the user expects; fix what looks wrong

## 4. Project context and final verification

- [ ] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, and verify all pass; run the `picture` manifest (the flows that use the crop screen) once while applying, not as part of the final regression run
