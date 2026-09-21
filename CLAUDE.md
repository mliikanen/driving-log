# Driving Log

Kotlin Multiplatform (Android + iOS) mileage tracking app, built with spec-driven
development using [OpenSpec](https://github.com/Fission-AI/OpenSpec).

## Spec-driven workflow

Specs in `openspec/specs/` are the source of truth for current behavior. Do not
implement a feature or behavior change without a change in `openspec/changes/`.

1. `/opsx:propose <name>` (or `openspec new change <name>`): proposal, spec deltas, design, tasks
2. Review and refine the artifacts before writing code
3. `/opsx:apply`: implement `tasks.md` item by item, ticking them off
4. `/opsx:archive`: merge the deltas into `openspec/specs/` when done

Useful CLI: `openspec list`, `openspec show <change>`, `openspec validate --all`.
Project context and artifact rules live in `openspec/config.yaml`.

## Layout

- `shared/`: KMP library (commonMain UI + logic, androidMain, iosMain)
- `androidApp/`: Android application shell
- `iosApp/`: iOS shell (Xcode project is created on a Mac)
- `openspec/`: specs and changes

## Commands

- `./gradlew :shared:allTests`: run shared tests
- `./gradlew :androidApp:assembleDebug`: build the Android app (needs Android SDK)
- `maestro/run.sh <area>...`: run Maestro manifests on a running Android emulator or device with the debug app installed
  (`adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`). A manifest (`maestro/manifests/<area>.yaml`) is the flows of `maestro/<area>/` in order; areas:
  `vehicles`, `distance`, `resilience`, `appearance`. `maestro/run.sh vehicles edit` runs the manifest's setup and then one flow; `maestro/run.sh --all` runs all four.
  The device-state groups `picture` (checks the files the app stores over `adb`), `theme` (samples screenshot pixels) and `clock` (12-hour/24-hour) change or inspect device state and are run by name
  (`maestro/run.sh picture theme clock`). Each flow clears the app's data and expects the device locale English (United States). `maestro/subflows/` holds shared steps and is not run on its own;
  `maestro/check-permissions.sh` fails when the installed app requests a system permission.
  The flows are self-contained: the photos they use are in `maestro/assets/` (see its README) and the `setup.yaml` of each manifest uploads them once, so a fresh
  emulator with the debug app installed is all a manifest needs. `run.sh` first removes the copies earlier runs left (`maestro/reset-media.sh`; each run adds another copy, and after a few dozen
  the emulator's `addMedia` fails or the photo picker shows nothing).
- Test strategy: see `docs/test-strategy.md`. A check goes in the lowest kind of test that can check it; Maestro flows are only the happy path of a feature end to end.
  While working on a change, run only the Maestro manifests of the functionality the change touches (only flows that changed or that exercise the screens changed).
  The final regression run of a change is `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`, **without Maestro**.
  The whole Maestro suite is for major refactorings, which the developer names.
