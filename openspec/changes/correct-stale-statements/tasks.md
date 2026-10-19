# Tasks

## 1. Specs

- [ ] 1.1 Edit the Purpose of `openspec/specs/distance-logging/spec.md` (refuelings, the Home screen route, notes and
  photos) and of `openspec/specs/vehicles/spec.md` (every editable field) directly. Verify `scripts/validate-specs.sh`
  passes.
- [ ] 1.2 Check every delta against the app it describes: "Add a vehicle" against `LandingNavKey`/`AddVehicleNavKey`
  (returns with `onBack`), the field order against `AddVehicleScreen`, "Log event" availability against
  `LandingScreen`, refueling notes and photos against the log event form and `EventDetailsScreen`. Verify each
  statement by reading the code, and `openspec validate correct-stale-statements --strict` passes.

## 2. Code and tests

- [ ] 2.1 Make `KindSelector` (`LogEventChoices.kt`) always enabled: drop `enabled = LogKind.entries.size > 1` and its
  uses, and rewrite its KDoc for the two kinds. Verify the existing `LogEventProcessor` tests still pass and
  `maestro/distance/log-distance.yaml` still taps `log_kind_selector` (no flow change needed).
- [ ] 2.2 Rename `CropStateTest`'s `aMoveButtonMovesThePhotoAStepUnderTheFrame`,
  `theMoveButtonsStopAtEveryEdgeOnEveryShapeOfPhoto` and `aLandscapePhotoAtTheSmallestZoomCannotBeMovedUpOrDownByTheButtons`
  for the arrow keys, and fix the "move button(s)" comments in `CropState.kt` (`CropMove` and `MOVE_STEP`). Verify
  `grep -rni 'move button' shared/src` finds nothing and the tests pass.

## 3. Root cause

- [ ] 3.1 Add the sweep to `openspec/config.yaml`'s rules (proposal: search the main specs, tests and comments for
  statements the change makes false and list them; specs: fix each, replacing a requirement under a new name when a
  scenario must be renamed or dropped; tasks: rename tests and fix comments the sweep found) and one line in
  `docs/change-workflow.md`'s Proposing. Verify `openspec instructions proposal --change correct-stale-statements
  --json` shows the new rule.
- [ ] 3.2 Comment on `add-vehicle-hiding`'s PR that its "Vehicle details screen" delta should rename "Open the log
  distance form" with design Decision 1. Verify the comment is posted.

## 4. Regression

- [ ] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh`, and
  verify both pass. No Maestro manifest needs to run: no behavior changes (the `distance` manifest covers the Kind
  selector if a reviewer wants it).

## Workflow follow-up

- Archive as the PR's last commit after review.
