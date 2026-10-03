# Driving Log

Kotlin Multiplatform (Android + iOS) mileage tracking app, built with spec-driven
development using [OpenSpec](https://github.com/Fission-AI/OpenSpec).

## Spec-driven workflow

Specs in `openspec/specs/` are the source of truth for current behavior. Do not
implement a feature or behavior change without a change in `openspec/changes/`.

**Every change lives on its own branch, `change/<name>`, never on `main`** (`docs/change-workflow.md`). Create it
from `main` in a worktree (`git worktree add ../driving-log-<name> -b change/<name> main`) and work there:

1. `/opsx:propose <name>` (or `openspec new change <name>`): proposal, spec deltas, design, tasks; commit on the
   branch, push, open a draft PR
2. Review and refine the artifacts before writing code
3. `/opsx:apply`: implement `tasks.md` item by item, ticking them off
4. `/opsx:archive`: merge the deltas into `openspec/specs/` when done, then merge the branch into `main`

`main`'s `openspec/changes/` holds only `archive/`. Changes in flight are the `change/*` branches
(`git branch -a --list '*change/*'`): check them before proposing, and record a needed one in `.openspec.yaml`
as `depends_on: [<change>]`.

Useful CLI: `openspec list` (on a change's branch), `openspec show <change>`, `openspec validate --all`.
Project context and artifact rules live in `openspec/config.yaml`.

## Layout

- `shared/`: KMP library (commonMain UI + logic, androidMain, iosMain)
- `androidApp/`: Android application shell
- `iosApp/`: iOS shell (Xcode project is created on a Mac)
- `openspec/`: specs and changes
- `docs/`: operational documentation for how the project is built, tested and released — not app behavior (that's
  `openspec/specs/`). Read the relevant file here before touching an area it covers, e.g. `docs/test-strategy.md`
  before changing tests, `docs/test-fixtures.md` before adding a Maestro fixture or a `.sqm` migration (a migration
  makes every checked-in fixture stale; that file says exactly what to run), `docs/color-palette.md` before a color
  change, `docs/change-workflow.md` before proposing or merging a change, `docs/code-quality.md` before changing lint configuration or adding a suppression, `docs/app-distribution.md` before touching signing, versioning or the Firebase App Distribution wiring
  (`docs/distribution.md` is the day-to-day "cut a release" runbook for the same area).

## Commands

- `./gradlew :shared:allTests`: run shared tests
- `./gradlew :androidApp:assembleDebug`: build the Android app (needs Android SDK; builds both the `production` and `fake` flavors' debug variants)
- `maestro/run.sh <area>...`: run Maestro manifests on a running Android emulator or device with the `fake` flavor's debug app installed
  (`./gradlew :androidApp:assembleFakeDebug`, then `adb install -r androidApp/build/outputs/apk/fake/debug/androidApp-fake-debug.apk`) — never the `production`
  flavor, which needs real Firebase/Google Sign-In setup Maestro can't drive (`add-firebase-auth`). A manifest (`maestro/manifests/<area>.yaml`) is the flows of `maestro/<area>/` in order; areas:
  `vehicles`, `distance`, `resilience`, `appearance`, `auth`. `maestro/run.sh vehicles edit` runs the manifest's setup and then one flow; `maestro/run.sh --all` runs all five.
  The device-state groups `picture` (checks the files the app stores over `adb`), `theme` (samples screenshot pixels) and `clock` (12-hour/24-hour) change or inspect device state and are run by name
  (`maestro/run.sh picture theme clock`). Each flow clears the app's data and expects the device locale English (United States). `maestro/subflows/` holds shared steps and is not run on its own;
  `maestro/check-permissions.sh` fails when the installed app requests a system permission.
  The flows are self-contained: the photos they use are in `maestro/assets/` (see its README) and the `setup.yaml` of each manifest uploads them once, so a fresh
  emulator with the debug app installed is all a manifest needs. `run.sh` first removes the copies earlier runs left (`maestro/reset-media.sh`; each run adds another copy, and after a few dozen
  the emulator's `addMedia` fails or the photo picker shows nothing).
- Test strategy: see `docs/test-strategy.md`. A check goes in the lowest kind of test that can check it; Maestro flows are only the happy path of a feature end to end.
  While working on a change, run only the Maestro manifests of the functionality the change touches (only flows that changed or that exercise the screens changed).
  The final regression run of a change is `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all --strict`, **without Maestro**.
  `codeQuality` is ktlint, detekt and Android lint (`docs/code-quality.md`); `./gradlew ktlintFormat` fixes formatting. There are no baselines: fix a finding or suppress it with a reason.
  CI (`.github/workflows/pr-check.yml`) runs the same final regression run on every PR and push to `main`, as two checks
  (`tests-and-build`, `code-quality`); once the ruleset on `main` (`.github/rulesets/main.json`) exists, a PR can't merge
  until both pass. Merging to `main` publishes to the testers (`docs/distribution.md`).
  The whole Maestro suite is for major refactorings, which the developer names.
