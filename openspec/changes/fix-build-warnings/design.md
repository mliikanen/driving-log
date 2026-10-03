# Design

## Context

A full build of `main` at `bfc3281` (`./gradlew :shared:allTests :androidApp:assembleDebug codeQuality
--warning-mode all --rerun-tasks`) prints:

| Kind | Where | Count |
|---|---|---|
| Deprecated `BackHandler` (use `NavigationEventHandler`) | `EventNoteAndPhotos.kt:240`, `LiveScannerContent.kt:64`, `ScanReviewContent.kt:75` | 3 |
| Deprecated `MenuAnchorType` (renamed `ExposedDropdownMenuAnchorType`) | `LogEventChoices.kt:55, 89, 203` | 3 |
| Deprecated `super.onActivityResult` | `MainActivity.kt:44` (both flavors' compilation) | 2 |
| Metro `SUGGEST_CLASS_INJECTION` (move `@Inject` to the class) | `VehicleDetailsProcessor`, `LogEventProcessor`, `EditVehicleProcessor`, `EventDetailsProcessor`, `VehicleLogProcessor` | 5 |
| Opt-in needed: `ExperimentalCoroutinesApi` (`flatMapLatest`) | `LogEventProcessor.kt:122` | 1 |
| Opt-in needed: `BetaInteropApi` (`NSData.create`) | `IosImageCodec.kt:169` (iOS common compilation) | 1 |
| Condition is always false (`event == null`) | `EventDetailsScreen.kt:100` | 1 |
| Unnecessary `!!` | `PictureDraftEditorTest.kt:114` | 1 |
| Java type mismatch: `String?` for `String` (`System.getProperty`) | `PpOcrRealPhotosJvmTest.kt:18, 19`, `LiveFramesJvmTest.kt:19, 20` | 4 |

That's 21 warnings: the problems report counts 20 Kotlin compiler warnings, plus the always-false condition. Gradle
itself reports no deprecations. Two JVM runtime warnings also appear:
- `sun.misc.Unsafe::objectFieldOffset` (×12), printed by the ktlint tasks. It comes from ktlint 1.8's embedded Kotlin
  compiler 2.2.21 under JDK 25.
- `java.lang.System::load` by `org.sqlite.SQLiteJDBCLoader`, from the host tests' SQLite driver (native access isn't
  enabled for the test JVM).

The project doesn't configure compiler warnings anywhere today. Android lint and detekt already fail on findings
(`docs/code-quality.md`).

## Goals / Non-Goals

**Goals:**
- Zero compiler and Gradle warnings, and a build that keeps it that way without anyone watching the log.
- No behavior change.

**Non-Goals:**
- Silencing a warning without resolving it: a suppression is for deliberate code only, with its reason.
- Kotlin/Native compile tasks (iOS link and compile), which only run on a Mac (decision 3).

## Decisions

### 1. Fix, or suppress with a reason, each warning

| Warning | Resolution |
|---|---|
| `BackHandler` ×3 | Replaced by `NavigationEventHandler` from `navigationevent-compose`, which Compose Multiplatform's own `BackHandler` now delegates to. The dependency is declared directly in the version catalog if it's only transitive today. If the replacement can't express "always handle back while shown" as simply in one of the three places, that place is suppressed with the reason, rather than restructured. |
| `MenuAnchorType` ×3 | `ExposedDropdownMenuAnchorType`, the same type renamed. |
| `super.onActivityResult` | Suppressed (`@Suppress("DEPRECATION")` on the override, next to its existing `@Deprecated`): `ActivityResultBridge` needs the classic result call, because `registerForActivityResult` must happen before the activity reaches `STARTED`, too early for an `AuthRepository` call (the bridge's KDoc). |
| Metro ×5 | `@Inject` moved from the constructor to the class, as Metro suggests. The graph is unchanged. |
| `flatMapLatest` | `@OptIn(ExperimentalCoroutinesApi::class)` on the processor's declaration that uses it. Switching the selected vehicle's flow is what `flatMapLatest` is for. |
| `NSData.create` | `@OptIn(BetaInteropApi::class)` next to the existing `ExperimentalForeignApi` opt-in. It's the standard way to wrap bytes. |
| Always-false condition | `event == null` removed from `EventDetailsScreen.kt:100`. `content != null` already implies it, and the compiler smart-casts `event` from that, which is why it warns. It was added in `clean-up-lint-baselines`. |
| Unnecessary `!!` | Removed. |
| `System.getProperty` ×4 | `requireNotNull(System.getProperty("ocrModelsDir")) { ... }` (and the others), with a message naming the property the Gradle task sets. A missing property fails with that message instead of an NPE later. |

### 2. Kotlin warnings as errors, set once, for every Kotlin compile task

The root `build.gradle.kts` sets `compilerOptions.allWarningsAsErrors = true` on every Kotlin compilation task of
every project (`KotlinCompilationTask`), except Kotlin/Native's (decision 3). That covers `shared`'s common,
Android, iOS-common and test compilations and the app's (AGP built-in Kotlin) in one place. A module can't quietly
opt out, and a new module gets it for free. The detekt and ktlint plugins don't run the compiler, so they aren't
affected.

*Alternative:* `-Werror` in each module's `compilerOptions`. Rejected: two places to keep in sync, and the app has no
`kotlin {}` block today.

### 3. Kotlin/Native compilations are left out

`KotlinNativeCompile` tasks (iOS framework and test binaries) run only on a Mac. Nobody can check what they print
from here, and turning warnings into errors there could break the Mac build on warnings no Linux run sees. They are
excluded by type, with a comment, until a change that runs on a Mac can clean them up. The iOS sources' common
(metadata) compilation is still covered; it's where the `BetaInteropApi` warning came from.

### 4. Gradle deprecations fail the build

`org.gradle.warning.mode=fail` in `gradle.properties`, so it applies to every invocation: local, CI, herd. Gradle
reports deprecations at the end of the build and then fails it. The build has none today, so this only catches new
ones (a Gradle, AGP or plugin update).

### 5. Tool JVM warnings: fix the project's, list the rest

- **SQLite in the host tests:** `--enable-native-access=ALL-UNNAMED` on `shared`'s host test JVMs
  (`tasks.withType<Test>`). JDK 25 asks for exactly that, and the warning goes away.
- **`sun.misc.Unsafe` from ktlint:** it's in ktlint's embedded compiler and fires in ktlint's worker JVM. The project
  can only silence it (with `--sun-misc-unsafe-memory-access=allow`, if ktlint-gradle lets worker JVM arguments be set),
  not fix it, so it isn't gated. `docs/code-quality.md` lists it with its cause, and the next ktlint update that embeds
  a newer compiler should make it go away.

JVM runtime warnings aren't compiler or Gradle warnings, so `allWarningsAsErrors` and `warning.mode` don't see them
either way. Nothing gates them, which is why the project's own one is fixed rather than left.

## Risks / Trade-offs

- [A dependency update now fails the build on its first deprecation, before anyone has looked at it.] → Intended
  (spec scenario "A toolchain update brings new warnings"). The update's change fixes the uses or suppresses them with
  a reason.
- [`NavigationEventHandler`'s API differs from `BackHandler`'s (it's built for predictive back).] → The three uses are
  simple "back closes this" handlers. They're checked on device with Maestro's `distance` manifest (`scan-reading`:
  the scanner and review; `view-existing-log`: the note editor) and `resilience` (`rotation`). Decision 1 allows a reasoned suppression where the new API can't say it simply.
- [The Mac build may still have warnings (Kotlin/Native).] → Not gated (decision 3), so it can't break.
- [`warning.mode=fail` with the configuration cache: a deprecation reported only on a cache miss.] → Gradle reports
  deprecations on the run that stores the entry, and CI's runs mostly start without one, so new ones are caught there.

## Migration Plan

One change. The fixes come first, then the gate, so every commit builds. Rollback: remove
`allWarningsAsErrors`/`warning.mode`; the fixes stand on their own.
