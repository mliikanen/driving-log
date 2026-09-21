# Design

## Context

Observed today (the tiers the request names, and what the project actually has):

| Tier | In the build today? |
|---|---|
| Pure unit tests | **Yes.** `kotlin.test` in `shared/src/commonTest` (about 50 files: `OdometerEntryTest` 39 tests, `AddVehicleProcessorTest` 82, `EditVehicleProcessorTest` 48, `LogDistanceRulesTest`, `LogDistanceProcessorTest`, `VehicleListProcessorTest`, formatting, color, picture tests) plus `androidHostTest` (SQLDelight JVM driver) and `iosTest`. Kide processors are tested with `kide-test`. |
| Compose integration tests (Robolectric + Compose UI test) | **No.** No Robolectric, no `compose.uiTest`, no composable is tested anywhere except through Maestro. (The project context names Robolectric as intended.) |
| Roborazzi screenshot tests | **No.** Not a dependency. |
| Maestro | 23 numbered flows in `maestro/`, run by `maestro test maestro/` (about 42 minutes), plus `picture/`, `theme/` and `clock/` groups driven by `run.sh` scripts that use `adb`. |
| JUnit Jupiter | **No.** The shared tests are `kotlin.test` in `commonTest`, which also runs on iOS, where Jupiter does not exist. |

Maestro 2.10.0 is installed. Its workspace `config.yaml` can set `flows` (globs), `includeTags`/`excludeTags` and `executionOrder` (`flowsOrder`); it has no hook that runs once per
workspace (`onFlowStart` runs before every flow), and a workspace directory has one `config.yaml`.

## Goals / Non-Goals

**Goals:**
- Write down the strategy and make the suite follow it: one owner per check, Maestro for happy paths only.
- Cut the Maestro suite to about a dozen flows and to a run of well under half of today's, without losing a check that has no other owner.
- Upload test photos once per manifest.
- Take Maestro out of the final regression run of a change.

**Non-Goals:**
- Adding Robolectric, Compose UI test or Roborazzi. That is a build change with its own decisions (source set, Robolectric SDK level, Roborazzi record/verify tasks, where reference images live) and is
  two follow-up changes, `add-compose-integration-tests` and `add-screenshot-tests`. The document describes both tiers as the target and marks them "not yet adopted". Some checks that only a
  flow makes today (a screen surviving a rotation, a layout that keeps its controls reachable) stay in a flow until the follow-ups exist; this change does not pretend otherwise.
- Changing any app behavior.

## Decisions

**A manifest is a Maestro workspace directory.** `maestro/<area>/config.yaml` with `flows` and `executionOrder.flowsOrder` (setup first), and `tags` on flows for `--include-tags`. Alternatives: one root
`config.yaml` with tags (no per-area setup, and one setup would upload every photo for every area: rejected), or a shell script per area that lists flows (the existing `run.sh` pattern; kept only for the areas that need
`adb`). The first task is a spike that proves on Maestro 2.10.0 that `flowsOrder` runs a setup flow first, that `../subflows` and `../assets` paths resolve from a flow in a manifest directory, and that `maestro test maestro/<area>`
reads `maestro/<area>/config.yaml`; if a piece does not work, the fallback is `run.sh` running `maestro test <area>/setup.yaml <area>/…` as one command (`maestro test` takes several flow files).

**Setup is a flow, not a script.** `setup.yaml` in a manifest is only `addMedia` for the photos the manifest's flows use. The earlier requirement, that the flows (not a script) are responsible for uploading, still holds; it is
now once per manifest. Uploading first also gives the media store time to index a JPEG before a flow opens the picker, which is what the retry in `subflows/pick-photo.yaml` works around (the retry stays as a safety net).
`maestro/run.sh` calls `reset-media.sh` first (it needs `adb`; if `adb` is missing it says so and continues, since a fresh emulator needs nothing), then `maestro test` on each manifest, and takes `--all`. To run one flow: `maestro/run.sh <area> <flow>` (setup, then that flow).

**Device-state groups become manifests with their own `run.sh`.** `picture/` (adb file checks between steps), `theme/` (pixel sampling in light and dark) and `clock/` (12-hour and 24-hour) keep the scripts, since they change or inspect device state. `run.sh --all` runs the plain manifests; the three are named on the
command line (`run.sh picture theme clock`) or by `--all --device-state`.

**Where each current flow goes** (the ledger; every deleted assertion is checked against the owner named, and the "add first" cases are tasks):

| Current | Fate | Owner of what is dropped |
|---|---|---|
| `01`, `03`, `06`, `07` (add with all fields, one vehicle per unit, list order, details and log) | Merge into `vehicles/add-and-browse`: add a vehicle in each of the four units, see the list (alphabetical), open details and the full log, back | List order: `VehicleListProcessorTest.vehiclesAreOrderedByNameWithoutRegardToCase`. Unit display: `OdometerFormatTest`. |
| `02`, `04`, `05` (validation, defaults, the odometer field) | **Delete.** One typed odometer with a backspace stays inside `add-and-browse` | Empty name, trimmed name, required odometer, typed zero, preselected unit, leading zero and every key sequence: `AddVehicleProcessorTest`, `VehicleInputTest`, `OdometerEntryTest`, `OdometerEntryZeroPrefixTest` |
| `08` (edit) | Trim to `vehicles/edit`: change the name and plate, save, see them; leave without saving keeps the vehicle | Trimming, clearing the plate, name required, cancel: `EditVehicleProcessorTest`. That the odometer and unit cannot be edited: **add** a state test |
| `09`, `14` offline part, `17` | Merge into `resilience/offline-and-restart`: airplane mode on, add a vehicle with a picture, log a distance, edit it, stop and start the app, all still there | The offline behavior is that no code path needs a network: nothing to unit test; the flow is the check |
| `10`, `14` rotation, `16` rotation, `19` and `21` rotation steps | Merge into `resilience/rotation`: rotate on the add screen with a typed name, a chosen type and color, on the crop screen, on the edit screen and on the log form | State kept across recreation: `restoreState` tests of the processors (present for add, **add** for edit and log distance if missing) |
| `11`, `12`, `13` (log distance) | Merge into `distance/log-distance`: a trip distance, then a new odometer count, the unit choice, another day and time zone chosen, the entries in the log | Validation messages, future moment, before the initial odometer: `LogDistanceRulesTest`, `LogDistanceProcessorTest`. Time zone search: `TimeZoneChoicesTest` |
| `15`, `16` leave part, `picture/*` | The `picture/` manifest is the one owner (its steps and adb checks); `15`'s list and details assertions are folded into `picture/add`, `16`'s "leaving drops the picture" into `picture/cancel`; `15`, `16` and `17` are deleted | Draft handling: `PictureDraftEditorTest`, `FileVehiclePictureStoreTest` |
| `18` (camera) | Keep as `picture/camera` (emulator's camera app; happy path) | — |
| `19`, `20`, `21` (type and color choices) | Merge into `appearance/type-and-color`: Car preselected, choose Motorcycle and a preset color, save, list and details show them, edit changes them, restart keeps them | All eight icons: `VehicleIconsTest`; selection rules: `ColorChoiceTest`, `AddVehicleProcessorTest`; persistence: repository tests |
| `22`, `23` (color from a picture) | Merge into `appearance/color-from-photo`: a solid-color photo and one real car photo give a selected "Photo color"; old color and photo color on the edit screen | The four real photos' colors: `RealPhotoColorJvmTest` |
| `theme/`, `clock/` | Kept as manifests with their `run.sh` | — |

That leaves twelve journey flows in place of 23 (`vehicles` 2, `distance` 1, `resilience` 2, `appearance` 2, `picture` 5 with the camera one) and the `theme`/`clock` helpers. The last column is checked for each dropped assertion: break the rule in the code, see the owner test fail.

**The regression policy is written in three places, and they agree.** `docs/test-strategy.md` (the reasoning), `CLAUDE.md` (the commands and the instruction), and `openspec/config.yaml` under `rules.tasks` (so future `/opsx:propose` tasks do not ask for the whole suite). The existing bullet in
`CLAUDE.md` ("run the full suite only when getting ready to archive") is replaced. A major refactoring is one that the developer names as such; the agent does not decide alone that a change is major.

**Jupiter.** The request names "Jupiter Unit". The shared logic is tested with `kotlin.test` in `commonTest` so it also runs on iOS, and Jupiter is JVM-only. The document therefore says: pure unit tests are `kotlin.test` (running on JUnit under Android host tests); JUnit Jupiter is allowed
in JVM-only source sets where a test needs its features, and nothing is migrated. See Open Questions.

## Risks / Trade-offs

- [A flow that is deleted was the only check of something] → the ledger names an owner for each dropped assertion, and a task per "add first" case; the deletion of each flow is its own step, after its owner is confirmed (break the rule, see the owner test fail).
- [Without Compose integration tests, layout and rotation regressions are caught only by the flows that remain] → the two resilience flows and the happy paths keep those journeys; the document lists it as a known gap that `add-compose-integration-tests` closes.
- [Order dependence inside a manifest: a setup flow that must come first] → `flowsOrder` fixes it, every other flow still starts with `clearState`, and no flow depends on another's data; only the emulator's photos are shared.
- [Photo copies accumulate across runs] → `run.sh` clears them first; the flows still work without it until the emulator's `addMedia` starts to fail (documented).
- [Maestro is run less, so a UI regression is found later] → it is what the strategy accepts: the feature's manifests run when the feature changes, and the full suite runs before a major refactoring.

## Migration Plan

Do the spike first. Write the document and the policy before deleting anything. Then move one manifest at a time: create its flows, run the manifest, delete the flows it replaces, and commit. The old `maestro test maestro/` command stops meaning "everything" when the flows move, so
`CLAUDE.md` changes in the same step as the last move. Rollback is a revert of the moving commits.

## Open Questions

- Are the manifests as Maestro workspace directories what was meant by "manifests"? Assumed so (the closest thing Maestro has); a named list in one file would be the alternative and changes only the layout tasks.
- Jupiter: is `kotlin.test` for shared logic acceptable, with Jupiter allowed only in JVM-only source sets? Assumed so because the shared tests must also run on iOS.
