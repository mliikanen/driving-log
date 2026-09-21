# Design

## Context

After `test-strategy-cleanup` (implemented, not yet archived) the Maestro suite is: manifests as config files `maestro/manifests/<area>.yaml` run with `maestro test --config=<file> .` from the one `maestro/` workspace
(media may only come from inside the workspace), each with a `setup.yaml` first where photos are needed; a single flow is run through a config generated for the run; `maestro/run.sh` is the entry point (it resets the emulator's photos
with `reset-media.sh` when `adb` exists, then runs the manifests, or the device-state groups `picture`, `theme` and `clock` through their own `run.sh`, which use `adb` and Python). The APK is installed by hand.

The build is Gradle 9.7.1 (wrapper), AGP 9.4.1, Kotlin 2.4.20, Kotlin DSL with a version catalog, **configuration cache and build cache on** (`gradle.properties`), modules `:shared` (KMP library) and `:androidApp`. There is no `build-logic` or `buildSrc`.
Maestro CLI 2.10.0 is installed outside the build. The policy of `docs/test-strategy.md` (Maestro is not part of the final regression run) must survive.

## Goals / Non-Goals

**Goals:** one Gradle task per manifest and per device-state group, aggregates, one-flow runs, an install-then-run dependency with clear failures, reports in `build/reports`, nothing wired into `check`.

**Non-Goals:** CI or cloud runs, iOS, changing flows or manifests, managing the Maestro CLI's version or installation, replacing the CLI.

## The evaluation

Requirements the candidates were held against (R1 to R8 of the request): **R1** a task per manifest plus an aggregate, running Maestro with `--config`, the workspace `maestro/` and the environment (`ADB`); **R2** one flow of a manifest; **R3** the adb-driven groups as tasks with the same naming;
**R4** depending on the debug install, with a clear failure without a device; **R5** not in `check`, in a separate group; **R6** Kotlin DSL, catalog, AGP 9.4.1 / Kotlin 2.4.20, configuration cache; **R7** JUnit report and output into `build/reports`; **R8** the CLI stays external, flows untouched.

### `craigatk/maestro-gradle-plugin` (plugin id `com.atkinsondev.maestro`)

Facts and how they were established (the checked-out source and a trial on this build's Gradle are **verified**; the rest is read from the README and the plugin portal):

- Latest release **2.0.0, 30 November 2022** (portal page and git tags; the earlier tags date from 26 October to 25 November 2022). The last commit in the repository is 2 March 2024 (dependency updates by a bot); the GitHub API shows a push on 7 September 2026 (bot branches, not a release). 11 stars, 11 open issues, Apache-2.0, unofficial (not from mobile.dev). Built with Gradle 7.6.4 and Kotlin 1.9.22.
- It is one task type, `com.atkinsondev.maestro.MaestroTest` (the plugin class itself does nothing), with these inputs: `flowsDir` (a directory), `flowParameters` (a map), `generateJunitReport`, `junitReportFile`, `maestroExecutable`. It runs the CLI, not Maestro Cloud. It does not install an APK (its README suggests `dependsOn("installDebugAndroidTest")`).
- **Verified on Gradle 9.7.1** (a scratch project outside the repository, `maestroExecutable` set to `echo`): the plugin resolves from the portal, the task runs, the configuration cache is stored and reused. The task is **up to date on the second run** (its inputs are the flow files), so it would not run tests again; that can be turned off with `outputs.upToDateWhen { false }`.
- **Verified against Maestro 2.10.0: `flowParameters` produces a command the CLI rejects.** The plugin builds `maestro -e KEY VALUE test ...` (the option before the subcommand, key and value as separate words); `maestro -e ADB x test --help` answers `Unknown options: '-e', 'ADB', 'x'`. Its 2.0.0 was written for Maestro 1.15.
- **Verified from the source: no `--config`, no way to pass other arguments, no `--udid`, no tags, no `--test-output-dir`**; `flowsDir` is passed as the workspace, so the manifest files (which need `--config`) cannot be used, and a single flow of a manifest cannot be run.
  The command line is assembled in private functions, so a subclass cannot add options.

| Requirement | Plugin |
|---|---|
| R1 task per manifest, `--config`, env | Partly: a task per manifest can be registered, but no `--config`; the flows-directory mode would run `setup.yaml` and the flows in file order, not the manifest's; env broken on 2.10.0 |
| R2 one flow | No |
| R3 device-state groups | No (they are scripts; a plain `Exec` is needed alongside) |
| R4 install dependency, no-device message | Not built in; `dependsOn` can be added by hand; no device check |
| R5 not in `check`, group | Yes (nothing is wired unless the build does it) |
| R6 Gradle 9, configuration cache | Works (verified above); Kotlin 1.9 built |
| R7 reports | JUnit file only; no test-output directory |
| R8 external CLI | Yes |

**Conclusion: it cannot satisfy R1, R2, R4 and R7, and has not been released for over three years.** Adopting it would still need a custom task next to it for everything the project actually does.

### Alternatives considered

| Candidate | What it is | Verdict |
|---|---|---|
| **A. A typed task class in a small convention plugin (`build-logic`, an included build)** | Our own `MaestroManifest` task (an abstract task using `ExecOperations`, `@Option --flow`, inputs and outputs declared) and `MaestroScript` task, registered by a convention plugin that discovers manifests and groups from the `maestro/` directory | **Chosen.** Satisfies R1 to R8; about 200 lines; no external plugin to track; testable with Gradle TestKit if wanted |
| B. Plain `tasks.register<Exec>` blocks in `androidApp/build.gradle.kts` | Same commands, no class | Works (Exec is configuration-cache compatible), but the discovery loop, the preflight, the lock and `--flow` (an `@Option` needs a task class) do not fit a build script well; would grow into A inside the script |
| C. Keep `maestro/run.sh`, Gradle tasks only `Exec` it | A thin wrapper | The install dependency, the report directory and per-manifest tasks would be Gradle, the rest bash: two places that know the manifests and the reset; `--flow` and the no-device message would be bash again. Rejected: the point is one implementation |
| D. `maestro-gradle-plugin` plus custom `Exec` tasks for what it lacks | Mix | The plugin contributes nothing that the custom task does not do better (see the table); an extra dependency to keep alive |
| E. Maestro Cloud (`maestro cloud`, the GitHub action, Bitrise steps) | Runs flows on hosted devices | No Gradle plugin exists (searched September 2026); not local; a cloud account is out of scope |
| F. `devicelab-dev/maestro-runner` | An independent open-source runner that runs Maestro flows without the JVM CLI | A replacement of the CLI, not a Gradle integration, and a new tool to trust with our flows; out of scope |
| G. Batching flows into parent flows (the approach described by Doist for cloud runs) | A flow layout technique | Not about Gradle; the manifests already do this |

Searches made (September 2026): the plugin portal, GitHub and general web searches for "Maestro gradle plugin", "Maestro Cloud gradle" and Maestro-runner alternatives; nothing else turned up that runs Maestro from Gradle.

Links: [craigatk/maestro-gradle-plugin](https://github.com/craigatk/maestro-gradle-plugin) · [portal page](https://plugins.gradle.org/plugin/com.atkinsondev.maestro) · [the author's write-up](https://www.atkinsondev.com/post/android-maestro-testing/) ·
[Maestro Cloud docs](https://docs.maestro.dev/maestro-cloud/run-tests-on-maestro-cloud) · [Doist on orchestrating Maestro](https://www.doist.dev/orchestrating-ui-tests-with-maestro/) · [maestro-runner](https://github.com/devicelab-dev/maestro-runner).

## Decisions

**A custom typed task in a convention plugin, in a `build-logic` included build.** `pluginManagement { includeBuild("build-logic") }`; the plugin `driving-log.maestro` is applied only to `:androidApp` (see the next decision). Gradle's recommendation over `buildSrc` (a change in `buildSrc` invalidates the whole build's caches); the version catalog is not
needed, since the plugin uses only the Gradle API and the Android Gradle Plugin API that is already on the classpath of the app's build.

**The tasks live in `:androidApp`.** It is the project that has `installDebug` and, through `androidComponents.sdkComponents.adb`, the path of the `adb` of the SDK the build already uses (no `PATH` guesswork, no reading of `local.properties`). A task in the root project would have to reach into another project's model, which the configuration cache
dislikes. `./gradlew maestroVehicles` from the root finds the task in `:androidApp` by name (Gradle selects tasks by name across projects), so the commands stay short. *To be verified in the spike: that `sdkComponents.adb` exists on AGP 9.4.1 in the form assumed.*

**Naming and discovery.** `maestro<Name>` for every `maestro/manifests/<name>.yaml` (manifest tasks) and every `maestro/<name>/run.sh` (device-state tasks); the aggregates `maestroAll` and `maestroDeviceState`; the support tasks `maestroPreflight` and `maestroResetMedia` in the same group. The convention plugin lists the two directories at configuration time (with the directory as a declared
input of the configuration, so the configuration cache notices a new manifest), so adding a manifest adds its task.

**What a manifest task does.** It runs `maestro [--udid <serial>] test --config=<manifest> --format junit --output build/reports/maestro/<name>/report.xml --test-output-dir build/reports/maestro/<name> <maestro dir>` with the maestro directory as the working directory. `--flow=<name>` writes a config to the task's
temporary directory listing `<area>/setup.yaml` (if the area has one) and `<area>/<name>.yaml` with `flowsOrder` (the same config `run.sh` generates today), where the area is the manifest's name. It has `outputs.upToDateWhen { false }` (a device test is never up to date), declares the report directory as an output, and logs where it is on success and on failure. *To be verified in the
spike: that Maestro 2.10.0 accepts `--config` together with `--format junit --output` and `--test-output-dir`, and what `flowsOrder` does with them.*

**What a device-state task does.** `Exec`-like: runs `maestro/<name>/run.sh` with `ADB` set to the SDK's adb, the working directory as the script expects. Their output stays as it is (the scripts print their own checks).

**Preflight.** `maestroPreflight` checks `maestro --version` (missing: fail with "Maestro is required: install it from https://docs.maestro.dev" and the way to override the executable with `-Pmaestro.executable=<path>`), then `adb devices` (no device, or several without `ANDROID_SERIAL`: fail with a message saying so). It is
a dependency of every manifest and device-state task and `mustRunAfter`-orders before `installDebug` so that nothing is installed when it fails. The message is the whole point: `installDebug` alone fails with AGP's "No connected devices!".

**Install and reset.** Every manifest and device-state task `dependsOn` `:androidApp:installDebug` (so the app under test is always the current code) and `maestroResetMedia` (runs `reset-media.sh` with the SDK's adb, once per Gradle invocation, before the first task that needs it). Skipping the install is Gradle's own `-x installDebug`; no extra property is invented. The device-state
groups that are launched against the installed app need the install as much as the manifests do.

**Ordering, no parallel device use.** The aggregates depend on their members, which are chained with `mustRunAfter` in a fixed order (vehicles, distance, resilience, appearance; picture, theme, clock). A `BuildService` with `maxParallelUsages = 1` is declared as used by every device task, so `--parallel` cannot run two on one device.

**Lifecycle.** No task of the group is a dependency of anything, and the plugin never touches `check`, `test`, `build` or `assemble`; the group exists only in `:androidApp`. A task in the spike and the final task run `./gradlew check --dry-run` and `./gradlew :shared:allTests :androidApp:assembleDebug --dry-run` and fail the check if a `maestro` task is listed.

**`maestro/run.sh` is removed; the device-state scripts stay.** One implementation of "reset, run the manifest, or one flow" is better than a bash and a Kotlin copy; the tasks replace `run.sh` command for command (`./gradlew maestroVehicles maestroDistance`, `./gradlew maestroVehicles --flow=edit`, `./gradlew maestroAll`, `./gradlew maestroDeviceState`). `picture/run.sh`, `theme/run.sh` and
`clock/run.sh` are real programs (adb checks, pixel sampling, device settings) and stay bash and Python; they lose only their "run it through `../run.sh`" comment. `reset-media.sh` and `check-permissions.sh` stay (the first is called by a task). A developer without Gradle can still run a manifest by hand with
`maestro test --config=maestro/manifests/<area>.yaml maestro/`; `docs/test-strategy.md` keeps that one line as the underlying command.

**Documentation.** `CLAUDE.md`, `docs/test-strategy.md` and the comments in `maestro/` are updated with the Gradle commands; `openspec/config.yaml` rules mention manifests but not the script, so they need only a check. The policy (what is run when) does not change.

## Risks / Trade-offs

- [The tasks are a small piece of build code to maintain] → about 200 lines in one place, with a TestKit test for the pure parts (task naming, the generated one-flow config, the dry-run check); against a plugin that is three years stale and lacks what we need.
- [`androidComponents.sdkComponents.adb` may not be usable from a convention plugin under AGP 9.4.1] → the spike checks it first; the fallback is the `ANDROID_HOME` / `local.properties` lookup, still inside the plugin.
- [The device is a shared, stateful resource: `installDebug` and the reset run before each Gradle invocation, which costs some seconds] → accepted; `-x installDebug` skips the install.
- [`maestro` on the `PATH` of the Gradle daemon differs from the shell's, when the daemon was started from another environment] → `-Pmaestro.executable` and the preflight message; `gradle.properties` is not changed.
- [Discovering tasks by directory listing at configuration time interacts with the configuration cache] → the directories are registered as configuration inputs (checked in the spike by adding a manifest and re-running).
- [Removing `run.sh` breaks habits and the words in `CLAUDE.md` and the docs] → they are changed in the same commit as the removal, as `test-strategy-cleanup` did for the numbered flows.

## Migration Plan

Archive `test-strategy-cleanup` first. Spike, then build the plugin and the tasks next to `run.sh` (both work), move the documentation to the Gradle commands, then delete `run.sh` in the last step. Rollback: revert the commits; nothing else depends on the tasks.

## Open Questions

- None that blocks the work. (Whether the aggregate `maestroAll` should also include the device-state groups is decided as *no*, in keeping with `run.sh --all` today; `maestroDeviceState` is separate.)
