# Proposal

> **No app behavior changes.** A follow-up to `test-strategy-cleanup`, **which must be archived first**: this change is written against the layout it leaves
> (`maestro/manifests/<area>.yaml` run with `--config` over the one `maestro/` workspace, `setup.yaml` first, the device-state groups `picture`, `theme` and `clock`
> with their own `run.sh`, `maestro/run.sh`, `maestro/reset-media.sh`) and against its `test-strategy` capability, so its own spec is a new capability, `maestro-gradle-tasks`.

## Why

Maestro is run today by `maestro/run.sh`, a bash script that has to be found, remembers nothing about the build, and is separate from how everything else in the project is run
(`./gradlew :shared:allTests`, `./gradlew :androidApp:assembleDebug`). A developer has to install the debug APK by hand (or run the previous
change's build and `adb install -r` afterwards) before a manifest, and a stale APK gives a green run that tested the wrong app. Running the manifests as Gradle tasks makes the
"build, install, run the manifests of the functionality I touched" step one command, gives the runs a place in the task graph, and puts their reports where the rest of the
reports are, without making Maestro part of `check` (the strategy says it is not part of the final regression run).

## What Changes

- **Gradle tasks in the group `maestro`**, one per manifest (`maestroVehicles`, `maestroDistance`, `maestroResilience`, `maestroAppearance`, discovered from `maestro/manifests/*.yaml`), an aggregate `maestroAll`,
  and one per device-state group (`maestroPicture`, `maestroTheme`, `maestroClock`, discovered from `maestro/<area>/run.sh`) with the aggregate `maestroDeviceState`. They run the Maestro CLI (an external install, as now) with
  `--config=<manifest>` over the `maestro/` workspace; a manifest task takes `--flow=<name>` to run its setup and that one flow.
- **The tasks are built for a device**: they depend on installing the debug APK (`:androidApp:installDebug`, skippable with Gradle's own `-x :androidApp:installDebug`), on a preflight task that fails with a plain message when `maestro` or a
  connected device is missing (never silently skipped), and on a `maestroResetMedia` task that removes the test photos earlier runs left. They never run up to date, do not run in parallel on one device,
  and are **not wired into `check`, `build`, `allTests` or any lifecycle task** (`docs/test-strategy.md`: Maestro is not part of the final regression run).
- **Reports**: each manifest run writes a JUnit report and Maestro's test output (screenshots, logs) to `build/reports/maestro/<area>/`.
- **The implementation is a small typed Gradle task in a convention plugin (an included `build-logic` build), not the third-party plugin.** The evaluation (`craigatk/maestro-gradle-plugin`
  2.0.0 and the alternatives) is in the design: the plugin passes no `--config`, has no way to run one flow, and its parameter option produces a command the installed Maestro (2.10.0) rejects.
- **`maestro/run.sh` is removed** (its logic moves into the tasks: one implementation). The three device-state `run.sh` scripts, `reset-media.sh` and `check-permissions.sh` stay, and are run by the tasks.
- **Documentation follows**: `CLAUDE.md` (Commands bullet), `docs/test-strategy.md` (the command block and the reset paragraph) and the comments under `maestro/` that say "run with `maestro/run.sh`" name `./gradlew maestro<Area>`
  instead; `openspec/config.yaml` and the two open changes' tasks speak of "the manifests" and need only a check. The policy of `test-strategy-cleanup` (what is run when) does not change.

Out of scope: running Maestro on CI or in the cloud, an iOS run, changing any flow or manifest, replacing the Maestro CLI (for example with a third-party runner), installing the Maestro CLI from Gradle, and putting Maestro into `check`.

## Capabilities

### New Capabilities
- `maestro-gradle-tasks`: how the Maestro manifests and the device-state groups are run from Gradle: which tasks exist, what they depend on, how a device or the CLI missing is reported, what they are never wired into, and where their reports go.

### Modified Capabilities
<!-- None: the `test-strategy` capability is created by test-strategy-cleanup (not yet archived); this change does not amend its requirements, only the commands its document shows. -->

## Impact

- New: `build-logic/` (an included build with one convention plugin and a task class), a `pluginManagement { includeBuild("build-logic") }` line in `settings.gradle.kts`, the plugin applied in `androidApp/build.gradle.kts`.
- Removed: `maestro/run.sh`. Edited: `CLAUDE.md`, `docs/test-strategy.md`, header comments under `maestro/`, `openspec/config.yaml` (wording).
- No new external dependency and no change to `gradle/libs.versions.toml` (the plugin is ours; only Gradle API and the AGP API already on the build's classpath).
- No app code, no data, no flow changes.
