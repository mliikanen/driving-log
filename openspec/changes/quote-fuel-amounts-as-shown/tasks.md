# Tasks

## 1. Confirm the tests already match the corrected specs

- [ ] 1.1 Check that each corrected scenario has a test expecting "42.30 L": `EventRowContentTest` for "A refueling's row",
  "A refueling with mileage shows two rows" and the figure example, and `maestro/distance/log-distance.yaml` for the
  rows on screen; the details screen shows the event row's own figure (`EventDetailsScreen`'s `content.trailing`), so
  "A refueling's details" follows from the row tests. Verify by grepping the tests for "42.30 L" and finding no test
  or flow expecting "42.3 L". No Maestro manifest needs to run: no code or flow changes.

## 2. Regression

- [ ] 2.1 Run the final regression run, `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
  `scripts/validate-specs.sh`, and verify both pass.

## Workflow follow-up

- Archive the change after review, as the PR's last commit.
