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
- `maestro test maestro/`: run the UI flows on a running Android emulator or device with the debug app installed
  (`adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`). Each flow clears the app's data and
  expects the device locale English (United States). Run a single flow with `maestro test maestro/05-odometer-field.yaml`.
  `maestro/subflows/` holds shared steps and is not run on its own. `maestro/clock/run.sh` runs the 12-hour/24-hour flow and
  `maestro/picture/run.sh` the picture flows with a check of the files the app stores (both change device state over `adb`, so
  they are not part of the plain suite); `maestro/check-permissions.sh` fails when the installed app requests a system permission.
- Checking a change while working on it: run only the Maestro flows that were changed themselves or that exercise the screens the
  change touches, not the whole suite. Run the full suite only when getting ready to archive the change.
