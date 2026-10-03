# Tasks

## 1. Fix the warnings (design.md decision 1)

- [x] 1.1 Replace the three deprecated `BackHandler` uses (`EventNoteAndPhotos.kt`, `LiveScannerContent.kt`,
      `ScanReviewContent.kt`) with `NavigationEventHandler`, declaring `navigationevent-compose` in the version catalog
      if it's only a transitive dependency today. Where the new API can't express the handler as simply, suppress that
      use with the reason instead. Verify: those three warnings are gone; `maestro/run.sh distance` passes (back
      closes the scanner, the photo review and the note editor).
      Done: `NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = …)`, the
      handler Compose's deprecated `BackHandler` registers on the same dispatcher; `navigationevent-compose` 1.1.0 (the
      version Compose Multiplatform brings) declared directly. `maestro/run.sh distance` passed 4/4, but its flows press no
      system back on these screens, so a throwaway flow (not committed) pressed back on each: the scanner closed to the
      form, the photo review returned to the scanner, and the note editor attached the typed note and closed.
- [x] 1.2 Replace `MenuAnchorType` with `ExposedDropdownMenuAnchorType` (`LogEventChoices.kt` ×3). Verify: the warnings
      are gone; the `distance` run in 1.1 covers the dropdowns (`log-from-home`).
      Done: the three warnings are gone; `log-from-home` (vehicle and kind dropdowns) passed in the `distance` run.
- [x] 1.3 Move `@Inject` from the constructor to the class in the five processors Metro names. Verify: the warnings
      are gone and `:shared:allTests :androidApp:assembleDebug` passes (the graph still builds).
- [x] 1.4 Add the opt-ins: `ExperimentalCoroutinesApi` for `flatMapLatest` (`LogEventProcessor.kt`) and
      `BetaInteropApi` for `NSData.create` (`IosImageCodec.kt`). Verify: both warnings are gone.
- [x] 1.5 Remove the always-false `event == null` (`EventDetailsScreen.kt:100`) and the unnecessary `!!`
      (`PictureDraftEditorTest.kt:114`). Verify: both warnings are gone and the event details tests pass.
- [x] 1.6 In `PpOcrRealPhotosJvmTest` and `LiveFramesJvmTest`, read the `System.getProperty` paths with
      `requireNotNull(…) { "<property> is set by the Gradle test task" }`. Verify: the four type-mismatch warnings are
      gone and both tests pass.
- [x] 1.7 Suppress `DEPRECATION` on `MainActivity.onActivityResult` with the `ActivityResultBridge` reason. Verify: both
      flavors' compilations no longer warn there.
- [x] 1.8 Enable native access for `shared`'s host test JVMs (`--enable-native-access=ALL-UNNAMED`). Verify: the
      `System::load` warning from SQLite no longer appears in `:shared:allTests`' output.

## 2. The gate (design.md decisions 2 to 4)

- [x] 2.1 In the root `build.gradle.kts`, set `compilerOptions.allWarningsAsErrors = true` on every Kotlin compilation
      task except Kotlin/Native's, with a comment on why native is excluded. Verify: with a scratch deprecated call (e.g.
      a `MenuAnchorType` use) in commonMain, `./gradlew :shared:allTests` fails naming the file and the warning, and
      with it removed the build passes; the same for a scratch warning in `androidApp`. Remove the scratch code.
      Done: a deprecated call in `shared`'s commonMain and one in `androidApp` each fail with "warnings found and -Werror
      specified", naming the file and line.
- [x] 2.2 Set `org.gradle.warning.mode=fail` in `gradle.properties`, with a comment. Verify: the full build passes;
      a scratch use of a deprecated Gradle API in a build script (e.g. `project.buildDir`) fails it, naming the
      deprecation. Remove the scratch line.
      Done, plus `org.gradle.kotlin.dsl.allWarningsAsErrors=true` (design.md decision 4, added during apply) and
      `androidLibrary { }` → `android { }` in `shared/build.gradle.kts`, the one build-script warning. The `project.buildDir`
      probe fails the build, but through the build-script compiler (`buildDir` is deprecated for Kotlin too), so
      `warning.mode=fail`'s own path wasn't exercised by a probe; a runtime-only Gradle deprecation is hard to stage. The
      deprecated `androidLibrary { }` put back also fails the build. All four scripts recompiled show no warning.
- [x] 2.3 Rerun `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality --warning-mode all --rerun-tasks` and
      compare with design.md's Context. Verify: no Kotlin compiler warning and no Gradle deprecation is printed; the
      only JVM warning left is ktlint's `sun.misc.Unsafe`.
      Done: 0 Kotlin compiler warnings, 0 Gradle deprecations; the only JVM warning left is ktlint's
      `sun.misc.Unsafe` (×12). The SQLite `System::load` warning is gone.
## 3. Documentation

- [x] 3.1 Add a "Compiler and Gradle warnings" section to `docs/code-quality.md`: warnings are errors (Kotlin, except
      Kotlin/Native) and Gradle deprecations fail the build; how to suppress a compiler warning (narrowest scope, a
      reason, as for linters); and the tool warning left on purpose (ktlint's `sun.misc.Unsafe` under JDK 25, cause and
      when it should go). Add one line to `CLAUDE.md` next to the `codeQuality` line. Verify: the doc covers each
      scenario of the spec's new requirement.

## 4. Final regression run

- [x] 4.1 Run `maestro/run.sh resilience` (the rotation flow uses the note editor). Then the regression run:
      `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all --strict`.
      Verify: all pass, and the PR's checks are green.
      Done: `maestro/run.sh resilience` passed 3/3 (and `distance` 4/4, task 1.1); the regression run and
      `openspec validate --all --strict` (17 items) pass, and PR #4's four checks passed on 39c9cc7.
