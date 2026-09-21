# Tasks

## 1. Spike: Maestro manifests

- [x] 1.1 On Maestro 2.10.0 and the emulator, prove with a scratch workspace under the scratchpad (not committed) that a manifest can be a config file chosen with `--config`, that `executionOrder.flowsOrder` runs a `setup.yaml` (an `addMedia` of one photo) first, that `../subflows/…` and `../assets/…` resolve from a flow inside an area directory, and that `--include-tags` filters the flows; record the result (and the fallbacks) in the design's Decisions
- [x] 1.2 Prove that a flow that finds the setup's photos in the picker works with the pick-photo retry reduced to one attempt when the photos were uploaded in an earlier flow (run the picture add flow after a setup flow); record whether the retry can shrink

## 2. Strategy and policy

- [x] 2.1 Write `docs/test-strategy.md`: the four kinds of test and what each is for (with one example from this project each), "put a check in the lowest kind that can check it", the rule that Maestro flows are happy paths (and what to do with a case idea: a unit test), what exists today and what is not yet adopted (Compose integration tests via Robolectric, Roborazzi screenshot tests; the follow-up changes named), the `kotlin.test` decision (shared tests stay `kotlin.test`, no Jupiter), the platform split (unit tests shared and run everywhere; Robolectric, Compose UI test and Roborazzi tests are Android/JVM-only and live in `shared/src/androidHostTest`, with `androidApp` for shell tests only; iOS has no counterpart yet; stated even though no such tests exist), the manifests and their setup, and the run policy (while applying: the touched features' manifests; final regression: gradle tests, build, validate; whole suite: major refactorings only). Verify: the document exists and every statement about "today" matches the build (`grep` for robolectric, roborazzi and jupiter in `gradle/` and `*.kts` finds none)
- [x] 2.2 Replace the Maestro-checking bullet in `CLAUDE.md` (no Maestro in the final regression run, the touched manifests while applying, whole suite for major refactorings, link to the document; the Commands bullet is rewritten in 6.1, when `maestro/run.sh` exists); add to `openspec/config.yaml` a `rules.tasks` entry (name the manifests to run for the touched functionality; no whole-suite task unless the change is a major refactoring; the final regression run is the gradle tests, build and validation) and a line in the project context pointing to `docs/test-strategy.md`. Verify: `openspec validate --all --strict` passes and the two texts do not contradict each other
- [x] 2.3 In `add-vehicle-color-theme` task 3.4 and `adjust-picture-crop` task 4.1, replace "the whole Maestro suite" with the manifests those changes touch (appearance and vehicles; picture) and drop the whole-suite clause; verify with `grep -n "whole" openspec/changes/*/tasks.md` (no whole-suite task remains) and `openspec validate --all --strict`

## 3. Owners of what the flows check

- [ ] 3.1 For every row of the design's ledger, confirm that the owner named for each dropped assertion exists, and where the owner is not a plain test of the rule, list it: read the flow, list its assertions, and map each to a test; record the resulting table in the design (replacing the ledger's "Owner" wording where it differs)
- [ ] 3.2 Add the unit tests the ledger marks "add first" and any the audit of 3.1 finds missing (at least: the edit form keeps the odometer and unit out of reach, `EditVehicleProcessor` and `LogDistanceProcessor` restore their state before intents like the add processor does); run `./gradlew :shared:allTests` and verify each new test fails when the behavior it owns is broken (temporarily break it, see it fail, restore)

## 4. Manifest scaffolding

- [ ] 4.1 Create `maestro/run.sh`: runs `reset-media.sh` when `adb` is available (says so and goes on when not), then `maestro test maestro/<area>` for each area named, `--all` for all plain areas (`vehicles distance resilience appearance`), and `<area> <flow>` for setup plus one flow (through a generated config file); documents the device-state areas (`picture theme clock`) and runs them through their own `run.sh`. Verify: `maestro/run.sh nosuch` fails with the list of areas; running a manifest on the emulator works
- [ ] 4.2 Add `maestro/manifests/<area>.yaml` for `vehicles`, `distance`, `resilience` and `appearance`, each listing the area's flows and `flowsOrder` (setup first when it has one), and a `setup.yaml` in the areas whose flows choose photos (`appearance`, `resilience`); no tags (they conflict with `flowsOrder`); verify that `maestro/run.sh <area>` reaches the area's first flow

## 5. Move the flows, one manifest at a time

- [ ] 5.1 `vehicles/`: write `add-and-browse.yaml` (four units, all fields, list order, details, full log, back, a typed odometer with a backspace) and `edit.yaml`; run `maestro/run.sh vehicles`; then delete `01`, `02`, `03`, `04`, `05`, `06`, `07`, `08` and commit
- [ ] 5.2 `distance/`: write `log-distance.yaml` (trip distance, new odometer count, unit choice, an earlier day and another time zone, the log entries, the app in airplane mode is not part of it); run `maestro/run.sh distance`; then delete `11`, `12`, `13` and commit
- [ ] 5.3 `resilience/`: write `offline-and-restart.yaml` (airplane mode, add with a picture from the setup's photo, log a distance, edit, stop and start the app, everything present) and `rotation.yaml` (add screen with typed name, type and color, crop screen, edit screen, log form); run `maestro/run.sh resilience`; then delete `09`, `10`, `14`, `17` and commit
- [ ] 5.4 `appearance/`: write `type-and-color.yaml` and `color-from-photo.yaml` (setup uploads the solid purple photo and one real car photo); run `maestro/run.sh appearance`; then delete `19`, `20`, `21`, `22`, `23` and commit
- [ ] 5.5 `picture/`: move `picture/*.yaml` and `run.sh` into the manifest with its `setup.yaml` (uploaded once per `run.sh`, not per step), fold `15`'s list and details assertions into `add` and `16`'s leaving-drops-the-picture check into `cancel`, move `18` in as `camera.yaml`; run `maestro/picture/run.sh`; then delete `15`, `16`, `17`, `18` (17's persistence is covered by `resilience/offline-and-restart`) and commit
- [ ] 5.6 `theme/` and `clock/`: keep their scripts and flows, use the shared `run.sh` entry for them, and make sure no flow other than a setup flow uploads a photo (`grep -rn addMedia maestro` lists only `setup.yaml` files); run both scripts and commit

## 6. Finish

- [ ] 6.1 Update `maestro/assets/README.md` (which manifest's setup uploads which photo) and the layout description in `CLAUDE.md`; verify the documented commands by running each once
- [ ] 6.2 Run `maestro/run.sh --all` and `picture theme clock` once (this change restructures the whole suite, so it is a major refactoring by the document's own definition), record the run time against the earlier 42 minutes, and run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; verify all pass
