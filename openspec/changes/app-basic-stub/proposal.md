# Proposal

## Why

The repository has build scaffolding and a placeholder `App()` that renders a single `Text`, but no
application structure. Every upcoming feature (login, vehicles, refueling, camera capture, sync) needs
somewhere to live and a proven pattern to follow. A minimal, running stub establishes that skeleton now,
so later changes add behavior instead of arguing about wiring, and so the build, test and launch paths
are verified end to end early.

## What Changes

- Replace the placeholder `App()` with an app shell: a Material Design 3 theme (light and dark, following
  the system setting) hosting a single Home screen.
- The Home screen shows the app name in a top app bar and an empty-state message. It has no data and no actions
  yet; it exists to be replaced and extended by feature changes.
- Introduce the first MVI feature slice using Kide: `HomeViewState`, `HomeIntent`, `HomeSideEffect` and
  `HomeProcessor`, with the screen rendering state from the processor. This is the reference pattern for later features.
- Introduce the dependency injection foundation using Metro: an `AppGraph` in common code that provides the
  processor, created once at app start on each platform.
- Wire the entry points: `MainActivity` hosts the app on Android; `MainViewController` in `iosMain` exposes
  it to the future Xcode project.
- Replace the placeholder smoke test with tests for `HomeProcessor` using `kide-test`.

Out of scope (each is its own later change): login and accounts, Firebase, local storage and sync, vehicles,
camera and OCR, unit settings, multi-screen navigation (`kide-navigation`), Firebase App Distribution setup,
and the Xcode project itself. The app launcher icon (Android and iOS) is also left for a later change.

## Capabilities

### New Capabilities
- `app-shell`: the application launches, applies the Material 3 theme, and shows the Home screen through the
  MVI and dependency injection structure that all features build on.

### Modified Capabilities
<!-- None: no specs exist yet. -->

## Impact

- Code: `shared/src/commonMain` (theme, `App`, `home` package, DI graph), `shared/src/iosMain`
  (`MainViewController`), `androidApp` (`MainActivity`),
  `shared/src/commonTest` (replaces `SmokeTest`).
- Dependencies: no new libraries expected; Compose, Kide and Metro are already in the version catalog.
  `kotlinx-serialization` is not needed because state persistence across process death is not part of the stub.
- Verification: this is the first change to compile the Kotlin since the package rename and the Koin to
  Metro/Kide swap, so it may surface fixes in the existing build files.
- Platforms: Android is verified by build and unit tests; iOS code is written but cannot be built on this
  Linux machine and is verified later on a Mac.
