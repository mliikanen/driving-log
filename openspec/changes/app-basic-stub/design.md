# Design

## Context

Current state (see proposal.md for motivation):

- `shared/` is a KMP library (Android library target plus `iosArm64` and `iosSimulatorArm64` static framework),
  with Compose runtime, foundation, material3 and ui in `commonMain`.
- `App()` is a `MaterialTheme { Text("Driving Log") }`. `MainActivity` calls it via `setContent`.
- Kide 2.2.0 (`kide`, `kide-test`) and the Metro 1.4.4 Gradle plugin are on the classpath but nothing uses them yet.
- The build now compiles with `compileSdk` 37 and `minSdk` 33 (Kide 2.2.0 requires 33) and Android host tests enabled.
- Project rules: business logic in `commonMain`, MVI, tests in `commonTest` without devices, Android first.

## Goals / Non-Goals

**Goals:**
- One end-to-end vertical slice (theme, screen, processor, DI graph, entry points, tests) that later features copy.
- A green `./gradlew :shared:allTests` and `:androidApp:assembleDebug`.

**Non-Goals:**
- Navigation between screens, persistence, networking or state saving across process death.
- Any decision about the data layer, the Firebase integration or the module split; those wait for real features.

## Decisions

### 1. Package layout by feature, with shared app-level code at the root

```
com.mikonoma.drivinglog
  App.kt                 // App(): theme + Home screen
  di/AppGraph.kt         // Metro graph
  ui/theme/Theme.kt      // DrivingLogTheme (M3, system light/dark)
  home/                  // HomeScreen, HomeProcessor, HomeViewState, HomeIntent, HomeSideEffect
```

Feature packages own their contract types and screen, so a new feature is a new package. Alternative: layer
packages (`ui/`, `presentation/`). Rejected because it scatters one feature across the tree.

### 2. Kide core only; no `kide-navigation` yet

With a single screen there is nothing to navigate. `kide-navigation` pulls in Navigation 3 and expects
`ScreenNavKey` per screen plus a state serializer (which needs kotlinx-serialization). Adopt it in the first
change that adds a second screen. The stub renders `HomeScreen` directly from a `HomeProcessor` instance.
Alternative: add navigation now for a "proper" skeleton. Rejected as speculative weight that would be reworked
when real routes are known.

### 3. Processor lifetime and rotation

The processor must survive rotation (spec: state survives configuration changes). On Android it is held in a
`ViewModel` (`androidx.lifecycle:lifecycle-viewmodel-compose`) or, if Kide provides a host, that host; the graph
supplies the processor factory and the platform layer keeps the instance. Kide 2.2.0 processors are `AutoCloseable`, own a coroutine scope (defaulting to `defaultProcessorScope()`) and
expose `states` (StateFlow), `sideEffects` (Flow) and `dispatch`, so the holder only needs to keep the instance alive
and close it at the end of its life. The exact holder is confirmed while
implementing, against Kide 2.2.0 (the guide lists Decompose and Voyager hosts, and `kide-navigation` handles
this itself). If a `ViewModel` is needed, it is added to the version catalog and documented in the tasks.
Alternative: recreate the processor on rotation. Rejected as it breaks the requirement as soon as the state
is non-trivial.

### 4. Metro graph in common code, created per platform

`AppGraph` is a Metro `@DependencyGraph` in `commonMain` exposing the Home processor. Each platform entry
point creates it once (`createGraph<AppGraph>()` in a shared `App` entry) and passes what it needs down. Platform
services arrive later as graph inputs (`@Provides` from `expect`/`actual` or a graph factory parameter).
Verified against the Metro 1.4.4 runtime: `@DependencyGraph(scope)`, `@Inject`, `@Provides`, `@SingleIn`,
`AppScope` and `createGraph<T>()` all exist.
Alternative: manual constructor wiring until there are real dependencies. Rejected because the stub is
meant to prove the DI setup compiles on all targets.

### 5. `HomeProcessor` carries a minimal real state

Kide's `PresentationProcessor(initialState, scope, interceptors)` has defaults for the last two and an abstract
`map(intent)`; its tests use `processor.test { expectState(...) }` from `kide-test` (backed by Turbine).

`HomeViewState` holds a single derived flag (`isEmpty`, true) so the empty-state message is driven by state
rather than hard-coded in the composable, and the processor tests have something real to assert. The
`HomeIntent` and `HomeSideEffect` types exist as sealed interfaces with one placeholder member each only if
Kide requires non-empty hierarchies; otherwise they are empty sealed interfaces. No fake actions are added to the UI.

### 6. Theme

`DrivingLogTheme` wraps `MaterialTheme` and picks `lightColorScheme()` or `darkColorScheme()` via
`isSystemInDarkTheme()`. Default Material colors and typography for now; no dynamic color (Android-only,
would make Android and iOS differ). A brand palette is a later design change.

On Android the activity uses a `DayNight` window theme with the framework action bar disabled, and calls
`enableEdgeToEdge()`. Without this the default theme shows a second native title bar above the Compose top bar,
and status bar text is hard to read. The Compose `Scaffold` handles the insets.

### 7. Tests

`HomeProcessorTest` in `commonTest` uses `kide-test` to assert the initial state. Compose UI tests need a
device or Robolectric and are deferred; the launch path is checked by `assembleDebug` and a manual run on an emulator.
The old `SmokeTest` is removed.

## Risks / Trade-offs

- [First compile since the rename and the DI swap may reveal build breakage, for example AGP 9.4.1 with the
  `android.kotlin.multiplatform.library` plugin, or Metro/Kotlin 2.4.20 compatibility] → Fix build files as part of
  the first task and keep those fixes in their own commit.
- [Local build currently lacks a JDK with `javac`, so nothing compiles yet] → Install a JDK before applying.
- [Kide and Metro API details in this document come from README-level reading] → The first implementation
  task is a compile check against the real APIs, and the design is updated if they differ.
- [iOS code cannot be built or run on this machine] → Keep it to a few lines, and verify on a Mac when the Xcode
  project is created.
- [A stub with no actions can look like busywork] → It is deliberately the smallest thing that proves the whole toolchain.
