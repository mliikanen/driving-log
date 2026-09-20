# Mileage Tracker

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
