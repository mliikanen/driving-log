# Tasks

## 1. Frames into the recognizers

- [x] 1.1 Give the recognizers a bitmap entry point (design.md, "Frames go to the same recognizers"): ML Kit from a
      `Bitmap`, PP-OCR from its `RgbImage`, the combined recognizer passing a frame to both; the bytes entry point
      decodes and calls it. A frame is turned upright by its rotation first. Verify: the existing
      `PpOcrRealPhotosJvmTest` and `CombinedTextRecognizerTest` pass unchanged, plus a unit test that a rotated frame's
      boxes come back in upright pixels.
- [x] 1.2 The `LiveReadings` tracker in common code: merge a frame's detections into the shown readings (same value at
      about the same place keeps its identity and takes the new box; new ones appear; one unseen for 1.5 s goes), each
      reading keeping its latest frame and that frame's detections. Verify: unit tests for each rule, with a fake clock.
- [x] 1.3 The frame-to-screen mapping for `FILL_CENTER` (scale by the larger ratio, centered, overflow cropped), pure.
      Verify: unit tests for a frame wider than the view, taller than the view, and the same aspect.
- [x] 1.4 A JVM test feeding the real photos in `maestro/assets/ocr/` as frames (PP-OCR on ONNX Runtime's JVM library)
      through recognition, classification and the tracker. Verify: the LCD readings appear as tracked readings with the
      right kind, and stay when one frame of a sequence misses them.

## 2. The scanner screen

- [x] 2.1 Add CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) to the Android source set,
      and the `CAMERA` permission and `android.hardware.camera` feature (not required) to the manifest; update
      `maestro/check-permissions.sh` to allow exactly `android.permission.CAMERA`. Verify: `./gradlew
      :androidApp:assembleDebug` builds, and `maestro/check-permissions.sh` passes with only the camera listed.
- [x] 2.2 The camera area (an `expect` composable, Android `actual`): `PreviewView` with `FILL_CENTER`, `ImageAnalysis`
      (latest frame only, RGBA, about 1280 × 720) in one `UseCaseGroup` with the view's `ViewPort`, analysis on one
      background thread feeding the combined recognizer, `detectReadings` and the tracker; bound to the lifecycle and
      stopped when the scanner closes. Verify: on the emulator, the scanner shows the camera's preview and closing it
      stops the camera (no camera in use afterwards, per `adb shell dumpsys media.camera`).
- [x] 2.3 The scanner content: tracked readings drawn and tappable (neutral outline, "ODO 71140"-style labels, 48 dp
      targets), the close action and back, the photo action (a floating button opening the system chooser), "Getting
      ready…" until the first frame is read, and the no-permission state (explanation and a button to the app's
      settings). Colors from the theme only. Verify: the Maestro flow in 4.1, and the mapping tests of 1.3.

## 3. The log event form

- [x] 3.1 `ScanDraft.scannerOpen` and the intents: open the scanner (after the permission request, whatever the
      answer), close it, a live reading tapped (encode the frame, keep it as the pending scan with its frame's detections
      and the tapped one accepted, apply it with `withScannedReading`, close the scanner). The photo flow runs from the
      scanner: leaving its review returns to the scanner; confirming closes both. Verify: processor unit tests for
      opening and closing (the form unchanged, nothing kept), a tapped odometer and trip reading (the way and field,
      the kept capture), a second accepted scan replacing the first, leaving the photo review returning to the scanner,
      and a saved-state round trip with the scanner open.
- [x] 3.2 "Scan a reading" opens the scanner through the permission request (Activity Result `RequestPermission`), the
      answer read each time. Verify: on the emulator, refusing shows the explanation with the photo action working, and
      granting shows the preview.

## 4. Verification

- [x] 4.1 Update the `distance` manifest's `scan-reading` flow: "Scan a reading" opens the scanner (grant the camera
      permission), close it and see the form unchanged; open it again and use the photo action for the existing car and
      LCD cases (unchanged from there). Verify: `maestro/run.sh distance` passes and `maestro/check-permissions.sh`
      lists only the camera.
- [ ] 4.2 The developer points the scanner at a real dashboard on a phone and taps a reading (the emulator's camera
      cannot show one; design.md, Risks). Verify: the developer confirms the reading was boxed and applied.
- [x] 4.3 Final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all
      --strict`, without Maestro. Verify: both pass.
