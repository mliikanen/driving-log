# Proposal

## Why

The build passes, but it warns, and nothing stops a change from adding more warnings. A full build of `main`
(`./gradlew :shared:allTests :androidApp:assembleDebug codeQuality --warning-mode all --rerun-tasks`) prints 20
Kotlin compiler warnings, in 16 places. Some of them point at real risk, not style:
- deprecated APIs the next library update can remove (Compose's `BackHandler` ×3, Material 3's `MenuAnchorType` ×3,
  `Activity.onActivityResult`);
- a condition that is always false (`EventDetailsScreen.kt:100`);
- a coroutines API used without opting in to it (`flatMapLatest`);
- Java platform types assumed non-null in tests.

Warnings that are allowed to accumulate stop being read. The static-analysis gate (`codeQuality`) already fails on
lint and detekt findings. The compiler and Gradle are the remaining tools whose warnings pass silently.

## What Changes

- **Every current compiler warning is resolved** in code. Each is fixed where a fix doesn't change behavior. A warning
  that marks a deliberate choice is suppressed at the narrowest scope with a reason, as the code-quality spec
  already requires for linters:
  - deprecated APIs replaced by their successors: `NavigationEventHandler` for `BackHandler`,
    `ExposedDropdownMenuAnchorType` for `MenuAnchorType`;
  - Metro's "only one `@Inject` constructor" suggestion: the annotation moved to the class (5 processors);
  - an opt-in added where an experimental or beta API is used on purpose (`flatMapLatest`; `NSData.create` in iOS);
  - the always-false condition and the unnecessary `!!` removed;
  - the Java platform types in the host tests (`System.getProperty`) handled as the nullable values they are;
  - `MainActivity.onActivityResult` keeps calling the deprecated `super`. `ActivityResultBridge` exists because the
    Activity Result API must be registered before the activity starts, too early for the auth flow. That call is
    suppressed with that reason.
- **The build fails on any new warning**:
  - Kotlin compiler warnings become errors in every compilation of `shared` and `androidApp`;
  - Gradle runs with `org.gradle.warning.mode=fail`, so a Gradle or plugin deprecation fails the build;
  - the build scripts' own compiler warnings fail it too (`org.gradle.kotlin.dsl.allWarningsAsErrors`). The one
    script warning there was, KMP's deprecated `androidLibrary { }` block in `shared`, becomes `android { }`.
  - It's part of the build itself, so the final regression run, the CI checks and herd workers all apply it with no
    command change.
- **The JVM warning from the host tests** (SQLite loading its native library) is fixed by enabling native access for
  the test JVM.
- `docs/code-quality.md` covers the compiler and Gradle gate, how to suppress a compiler warning, and the tool warnings
  that are knowingly left (below).

**Out of scope:**
- **JVM warnings printed by tools, not by our code.** ktlint 1.8's embedded Kotlin compiler calls `sun.misc.Unsafe`,
  which JDK 25 warns about. Fixing that needs a ktlint release built on a newer compiler. It's listed in
  `docs/code-quality.md` and not gated.
- iOS native compilation warnings (Kotlin/Native link and compile tasks). They only run on a Mac, which CI and the herd
  don't have. The iOS sources' *common* compilation is covered.
- Replacing `ActivityResultBridge` with the Activity Result API (a behavior change, and its own change if wanted).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `code-quality`: the build reports no compiler or Gradle warnings, and a new one fails it. A compiler warning may be
  suppressed only under the same narrowest-scope-with-a-reason rule as a linter finding.

## Impact

- Code: about 12 files in `shared` (commonMain, iosMain, commonTest, androidHostTest) and `MainActivity.kt`. No
  behavior change.
- Build: the `compilerOptions` of `shared` and `androidApp` (warnings as errors), `gradle.properties`
  (`org.gradle.warning.mode=fail`), the host test JVM arguments.
- Docs: `docs/code-quality.md`, `CLAUDE.md` (one line next to `codeQuality`).
- After this, a toolchain update that brings new deprecation warnings fails the build until its change handles them,
  as a linter update already does.
- Maestro: `distance` (the log event form's dropdowns, the note editor, and the scanner and review screens that
  use the back handler) and `resilience` (its rotation flow uses the note editor).
