# Tasks

Each group ends with `./gradlew codeQuality` passing, after regenerating the baselines it touched
(`docs/code-quality.md`, "Baselines"), and `git diff` on the baseline files showing only removed entries.

## 1. Prerequisites

- [x] 1.1 Check that `add-lint-quality-gates` is archived (`openspec/specs/code-quality/spec.md` exists). Verify: it
      does; if not, stop and archive that change first.

## 2. Android lint (design.md decision 6)

- [x] 2.1 Fix the five UseKtx findings (`SharedPreferencesClaimedAccountStore`, `AndroidImageCodec` ×3,
      `MlKitTextRecognizer`), keeping the same scaling filter flag. Verify: `./gradlew :shared:allTests` passes, and
      both lint baselines lose those entries.
- [x] 2.2 Fix `RememberReturnType` in `App.kt`, keeping navigation key registration synchronous during composition.
      Verify: `:shared:lintAndroidMain` no longer reports it; Maestro `maestro/run.sh vehicles` passes (back stack
      restore after process death is in that manifest's flows).
- [x] 2.3 Add the explicit `NoCredentialException` catch in `FirebaseAuthRepository.signIn`, mapped to the same
      failure as today. Verify: `:androidApp:lintProductionDebug` no longer reports CredentialManagerMisuse, and the
      `SignInProcessor` tests pass unchanged.
- [x] 2.4 Disable `OldTargetApi` and `ChromeOsAbiSupport` in `androidApp`'s `lint {}` with comments, and suppress
      `MissingApplicationIcon` on `<application>` with a comment naming `update-app-icon` (unless that change is
      already applied). Add a task to `update-app-icon`'s tasks.md to remove that suppression. Verify: both lint
      baselines are empty (`<issues>` with no `<issue>`), and `update-app-icon/tasks.md` has the new task.

## 3. detekt configuration (design.md decisions 2 and 4)

- [x] 3.1 In `config/detekt/detekt.yml`: `ReturnCount` (`excludeGuardClauses: true`, `max: 3`),
      `CyclomaticComplexMethod` (`ignoreSimpleWhenEntries: true`), and test source sets excluded from `LargeClass` and
      `DestructuringDeclarationWithTooManyEntries`, each with a comment naming the convention. Verify: `./gradlew
      detektBaseline` removes entries only, and the remaining ReturnCount/CyclomaticComplexMethod entries are listed
      in this task's notes for groups 5 and 6.
      Notes: 157 → 129 entries, removals only. Remaining ReturnCount: `validateLogDistance` (LogDistanceRules.kt),
      `LogEventState.withScannedReading` (LogEventProcessor.kt). Remaining CyclomaticComplexMethod: `EventDetailsContent`,
      `LogEventContent` (screens, 6.2), `PpOcr.boxes`, `PpOcr.regions` (ported, 6.4).

## 4. Mechanical detekt fixes (design.md decisions 5 and 7)

- [x] 4.1 Replace every baselined MagicNumber with a named constant next to its use (OCR, color math, units, time),
      with the formula cited for published ones. Verify: `:shared:allTests` passes (the color, contrast, OCR and unit
      tests pin these values), and the baseline has no MagicNumber entry.
- [x] 4.2 Rename `LiveCamera.kt` and `TimeZoneChoices.kt` after their declarations; comment the empty blocks and
      suppress the unused parameters in `AuthRepositoryFactory`/`CurrentActivityHolder` with the flavor reason; fix
      `Geometry.kt`'s five-way destructuring. Verify: `:shared:allTests :androidApp:assembleDebug` passes and those
      entries are gone.

## 5. Exceptions (design.md decision 3)

- [ ] 5.1 Add `catchingFailures` to commonMain (catches `Exception`, rethrows `CancellationException`, returns
      `Result`) with unit tests: a failure becomes `Result.failure`, a cancellation propagates. Verify: the tests
      pass.
- [ ] 5.2 Use it in `AddVehicleProcessor`, `EditVehicleProcessor`, `EventDetailsProcessor`, `LogEventProcessor` and
      `SqlDelightVehicleRepository` in place of `catch (throwable: Throwable)`. Verify: every processor test passes
      unchanged, and those TooGenericExceptionCaught entries are gone.
- [ ] 5.3 Suppress, with the fallback named, the deliberate catch-all fallbacks in `AndroidImageCodec`,
      `PhotoDecoding` and `CombinedTextRecognizer`. In `PhotoPicker.android`, keep or report the caught cause, or
      suppress where the platform gives nothing to keep. Verify: no TooGenericExceptionCaught or SwallowedException
      entry remains, and `maestro/run.sh picture` passes.

## 6. Size and complexity (design.md decision 4)

- [ ] 6.1 Before refactoring, check each remaining complex or long function has a test that would catch a behavior
      change (processor tests for processors; Maestro manifests for screens). Add a processor test where one is
      missing. Verify: the list of functions and their covering tests is in this task's notes.
- [ ] 6.2 Split the long screen composables (`AddVehicleContent`, `EditVehicleContent`, `CropScreen`,
      `EventDetails` ×2, `LogEventContent`, `VehicleDetails`) into sub-composables, keeping every test tag and
      semantics property. Verify: `:shared:allTests` passes, the LongMethod/CyclomaticComplexMethod entries for screens
      are gone, and Maestro `vehicles`, `distance` and `appearance` pass.
- [ ] 6.3 Restructure the processors' and helpers' remaining ReturnCount/ComplexCondition findings (e.g.
      `LogEventProcessor`'s form-state condition as a named property). Verify: processor tests pass, and the entries are
      gone.
- [ ] 6.4 Split `LogEventScreen.kt` and `ReadingDetection.kt` into files by concern. Suppress TooManyFunctions on
      `VehicleRepository`, its implementations and `PictureStore`, and suppress the complexity rules on `PpOcr`'s and
      `Geometry`'s ported functions, each with the reason from design.md decision 4. Verify: `:shared:allTests` passes
      (OCR tests including `PpOcrRealPhotosJvmTest` give identical results), and the detekt baseline is empty.

## 7. Remove the baselines

- [ ] 7.1 Delete `config/detekt/baseline.xml`, `androidApp/lint-baseline.xml`, `shared/lint-baseline.xml` and
      `scripts/check-baselines.sh`, and remove the `baseline` settings from root `build.gradle.kts` and both `lint {}`
      blocks. Verify: `./gradlew codeQuality` passes with no baseline anywhere (`git ls-files | grep -i baseline`
      prints nothing).
- [ ] 7.2 CI: if `add-ci-workflows` is applied, remove the baseline-check step from `pr-check.yml`'s `code-quality`
      job. If not, update its design.md decision 8, tasks 2.3 and 2.5, and its `test-strategy` delta (the
      requirement text's "no baseline grew" and the "A baseline grows" scenario) to drop it. Verify: `openspec validate
      --all --strict` passes and nothing references `check-baselines.sh` (`grep -rn check-baselines` over the repo,
      archive excepted).
- [ ] 7.3 Update `docs/code-quality.md`: replace the baselines section with "there are none; fix or suppress",
      including toolchain updates. Update `CLAUDE.md`'s "Baselines only shrink" line to "There are no baselines". Verify:
      neither file mentions a baseline file or the script.

## 8. Final regression run

- [ ] 8.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all
      --strict`, without Maestro (the manifests of the touched screens ran in groups 2, 5 and 6). Verify: all pass.
