# Tasks

## 1. Spike (nothing is committed except the recorded result)

- [ ] 1.1 In a scratch copy of the build (or a branch that is dropped), register one task that runs `maestro test --config=maestro/manifests/vehicles.yaml --format junit --output build/reports/maestro/vehicles/report.xml --test-output-dir build/reports/maestro/vehicles maestro` on the emulator with Maestro 2.10.0, through `ExecOperations`; verify that the flows run in the manifest's order (setup first where the manifest has one), the JUnit file and the output directory are written, and the exit code is non-zero when a flow is broken on purpose; record the result (and any option that must change) in the design's Decisions
- [ ] 1.2 In the same scratch build check that `androidComponents.sdkComponents.adb` is usable from a plugin applied to `:androidApp` under AGP 9.4.1 (print the path, then run `adb devices` through it); if not, record the fallback (`ANDROID_HOME`, `local.properties`) in the design
- [ ] 1.3 Verify with the configuration cache and the build cache on (as in `gradle.properties`) that the task runs both times when nothing changed (with `outputs.upToDateWhen { false }`), that the second run reuses the configuration cache, and that adding a file to `maestro/manifests/` (registered as a configuration input) is noticed by the next run; verify `./gradlew check --dry-run` lists no such task; record the results

## 2. The convention plugin

- [ ] 2.1 Create `build-logic/` (settings, `build.gradle.kts` with `kotlin-dsl` on the Kotlin version the build uses, one plugin `driving-log.maestro`) and add `includeBuild("build-logic")` to `pluginManagement` in `settings.gradle.kts`; verify `./gradlew help` and `./gradlew :androidApp:assembleDebug` still work with the configuration cache on
- [ ] 2.2 Add the task class `MaestroManifest` (inputs: manifest file, maestro directory, executable, optional device serial, report directory as an output, `@Option --flow`, never up to date; builds the command of the design; writes the one-flow config to its temporary directory) and a TestKit or unit test of the command it builds and of the one-flow config (setup listed when `setup.yaml` exists, `flowsOrder`, unknown flow fails naming the area's flows); verify with `./gradlew :build-logic:test`
- [ ] 2.3 Add the task class `MaestroScript` (runs `maestro/<name>/run.sh` with `ADB` set and the working directory the script expects) and a test of its command line; verify with `./gradlew :build-logic:test`
- [ ] 2.4 Add `maestroPreflight` (the Maestro executable answers `--version`, exactly one device from `adb devices` or the one named by `ANDROID_SERIAL`; each failure a message that says what to do; `-Pmaestro.executable` override) and test the messages for a missing executable, no device and several devices with a fake `adb` script; verify with `./gradlew :build-logic:test`
- [ ] 2.5 Add `maestroResetMedia` (runs `maestro/reset-media.sh` with the SDK's adb)

## 3. Registering the tasks

- [ ] 3.1 In the plugin register `maestro<Name>` for every `maestro/manifests/*.yaml` and `maestro<Name>` for every `maestro/<name>/run.sh` (group `maestro`, descriptions), the aggregates `maestroAll` and `maestroDeviceState`, the `mustRunAfter` chain in a fixed order, the dependencies on `maestroPreflight`, `:androidApp:installDebug` (ordered after the preflight) and `maestroResetMedia`, and a `BuildService` with `maxParallelUsages = 1` used by every device task; apply the plugin in `androidApp/build.gradle.kts`; verify with `./gradlew :androidApp:tasks --group maestro` (all seven tasks and the aggregates listed) and `./gradlew maestroAll --dry-run` (the order is vehicles, distance, resilience, appearance, after the preflight, install and reset)
- [ ] 3.2 Verify that no task of the group is reachable from a lifecycle task: `./gradlew check --dry-run` and `./gradlew :shared:allTests :androidApp:assembleDebug --dry-run` list none (add this as a check in the plugin's tests, so it cannot regress silently)

## 4. Verification on the emulator

- [ ] 4.1 Run `./gradlew maestroVehicles` (install, reset, setup-less manifest) and `./gradlew maestroResilience` (a manifest with `setup.yaml`); verify both pass and write `build/reports/maestro/<area>/report.xml` and the output directory
- [ ] 4.2 Run `./gradlew maestroVehicles --flow=edit` and verify only that flow runs; run it with `--flow=nosuch` and verify the failure lists the flows and starts no Maestro
- [ ] 4.3 Stop the emulator (or `adb kill-server` with none attached) and run `./gradlew maestroVehicles`; verify it fails before installing, with the no-device message; run it with the `maestro` executable overridden to a missing path and verify the missing-CLI message
- [ ] 4.4 Break a flow on purpose and run its manifest; verify the build fails, the report directory has the failing flow's output and the message names it; restore the flow
- [ ] 4.5 Run `./gradlew maestroDeviceState` and `./gradlew maestroAll`; verify they pass in order and never at the same time, and that `./gradlew maestroTheme` alone works
- [ ] 4.6 Change a line of app code, run a manifest task with `--dry-run` and then for real, and verify the app is rebuilt and installed first; run it with `-x :androidApp:installDebug` and verify it is skipped

## 5. Replace `run.sh` and document

- [ ] 5.1 Update `CLAUDE.md` (Commands bullet), `docs/test-strategy.md` (the command block, the reset paragraph, the "what is run when" table, one line on how to run a manifest by hand without Gradle), the header comments of `maestro/manifests/*.yaml`, `maestro/picture/*.yaml`, `maestro/theme/state.yaml`, `maestro/picture/setup.yaml` and `maestro/picture/run.sh`, and check `openspec/config.yaml` and the open changes' tasks for the words `run.sh`; verify with `git grep -n "maestro/run.sh\|run\.sh" -- ':!openspec/changes/archive'` (only the device-state scripts' own names remain)
- [ ] 5.2 Delete `maestro/run.sh`; verify `./gradlew maestroAll` and `./gradlew maestroDeviceState` still pass, `./gradlew :shared:allTests :androidApp:assembleDebug` passes, and `openspec validate --all --strict` passes (Maestro is not part of this final run, per the strategy; the manifests were run in section 4)
