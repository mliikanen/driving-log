# Tasks

## 1. Build baseline

- [x] 1.1 Install a JDK that includes `javac` and verify `./gradlew :androidApp:assembleDebug` gets past Java compiler resolution
- [x] 1.2 Fix any build-file breakage from the rename and the Metro/Kide swap (own commit) and verify `./gradlew :shared:allTests :androidApp:assembleDebug` succeeds with the existing placeholder `App()`
- [x] 1.3 Compile-check the Kide and Metro APIs used in the design (processor base class, action builders, `@DependencyGraph`, `createGraph`) and update `design.md` if they differ

## 2. Theme and Home screen

- [x] 2.1 Add `DrivingLogTheme` (Material 3, light/dark from the system setting) and verify by a composable preview or a commonTest that the scheme follows the dark-mode flag
- [x] 2.2 Add `HomeViewState`, `HomeIntent`, `HomeSideEffect` and `HomeProcessor` and verify the initial state reports the empty state
- [x] 2.3 Add `HomeScreen` (top app bar "Driving Log", empty-state message, no actions) rendering from the processor state and verify `:shared:compileKotlinAndroid` succeeds
- [x] 2.4 Replace the placeholder `App()` with the themed Home screen and verify the shared module still compiles

## 3. Dependency injection and lifetime

- [x] 3.1 Add `AppGraph` (Metro) providing `HomeProcessor` and verify it compiles for Android and iOS targets (`:shared:compileKotlinIosSimulatorArm64` if the host supports it, otherwise Android only, noting the limit)
- [ ] 3.2 Hold the processor so it survives rotation, adding any needed dependency to `libs.versions.toml`, and verify on an emulator or device that rotation keeps the Home screen content

## 4. Entry points

- [x] 4.1 Update `MainActivity` to create the graph once and host `App`, and verify `./gradlew :androidApp:assembleDebug` produces an APK
- [x] 4.2 Add `MainViewController` in `shared/src/iosMain` and update `iosApp/README.md` to match, verifying by review since iOS cannot be built here

## 5. Tests and verification

- [x] 5.1 Replace `SmokeTest` with `HomeProcessorTest` using `kide-test` covering the initial empty state, and verify `./gradlew :shared:allTests` passes
- [ ] 5.2 Run the app on an emulator or device in light and dark mode, offline, and rotate it, checking each scenario in `specs/app-shell/spec.md`
- [x] 5.3 Run `openspec validate --all` and verify it passes
