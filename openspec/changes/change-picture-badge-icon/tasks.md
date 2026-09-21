# Tasks

Apply after `add-vehicle-type` is applied and archived (same requirement text, same SVG conversion).

## 1. Icon

- [ ] 1.1 Add `docs/icons/phosphor/camera-fill.svg` (from the Phosphor set already in that folder, same `0 0 256 256` single-path form) and list it in `THIRD_PARTY_NOTICES.md`; verify the file is there and the notice names it
- [ ] 1.2 Add the camera `ImageVector` (`ui/PhotoIcons.kt`) from that SVG the way `VehicleIcons` is built, and a test in `commonTest` that it has a non-empty path and a 256 viewport scaled to the requested size; verify `./gradlew :shared:allTests` passes

## 2. Badge

- [ ] 2.1 In `PictureField`, use the camera icon (18 dp, `onPrimary`) in the existing circular badge instead of `Icons.Filled.Edit`, remove the unused import, keep `contentDescription = null`, and add `testTag("picture_badge")`; verify the module compiles for Android and iOS (`./gradlew :shared:compileKotlinIosSimulatorArm64 :androidApp:assembleDebug`)
- [ ] 2.2 Verify the preview's semantics are unchanged: it is still announced as "Add picture" / "Change picture" with one click action, and `picture_preview` still opens the chooser (Maestro flows 15 and 18 keep passing)

## 3. Verification

- [ ] 3.1 Check the badge by hand on the emulator in light and dark mode, on the add screen (placeholder for a chosen type and before a type is chosen) and on the edit screen (with a picture), also in landscape; fix the glyph size or fall back to `camera` (regular) if it does not read at 18 dp
- [ ] 3.2 Add `assertVisible: id: picture_badge` to the add screen steps of `maestro/15-vehicle-picture.yaml` (no picture) and to the edit screen steps (with a picture), and to `maestro/picture/add.yaml`; verify `maestro test maestro/15-vehicle-picture.yaml` and `maestro/18-vehicle-picture-camera.yaml` pass
- [ ] 3.3 Run the whole Maestro suite, `maestro/picture/run.sh`, `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; verify all pass
