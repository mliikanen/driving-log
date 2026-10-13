# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [ ] 1.1 `app-shell`: compare each of the 5 MODIFIED descriptions with `main`'s text sentence by sentence, the split
      ones against their new requirements together, with the palette's names and codes checked character for
      character. Verify: every old normative statement is found in the delta; any rewording that could be read
      differently is listed here.
- [ ] 1.2 `vehicle-color` and `event-pictures`: the same. Verify: as 1.1.
- [ ] 1.3 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-d --strict`
      passes, and an archive in a scratch copy of `openspec/` keeps every scenario and leaves 0 requirements over 500
      characters in the whole project (`openspec validate --all --strict` passes there).

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one; a device
check is recorded where only the screen can show it.

- [ ] 2.1 `app-shell`, the theme scenarios ("The light scheme's accents and dividers", "A primary button in light
      mode", "System bar icons in dark mode"). Verify: the tests are named here, existing or new, and pass in
      `./gradlew :shared:allTests`, or the device check is recorded.
- [ ] 2.2 `app-shell`, the permission and Home screen scenarios ("Opening a form that can scan asks nothing", "Tapping
      the edge of a tile", "After the first vehicle is added", "A screen reader reads a disabled action"). Verify: as
      2.1.
- [ ] 2.3 `vehicle-color` scenarios ("A saved color that is not a preset", "Saving applies the chosen color", "The same
      photo twice", "Choosing presets leaves the photo color alone", "A saved color reaching the details screen").
      Verify: as 2.1.

## 3. Strict spec validation again

- [ ] 3.1 Make `scripts/validate-specs.sh` run `openspec validate --all --strict --no-interactive` alone, and delete
      `scripts/check-requirement-length.mjs` and `scripts/check-requirement-length.test.mjs` (design Decision 2).
      Verify: `scripts/validate-specs.sh` passes on the archived specs of a scratch copy and fails on one with a
      requirement over 500 characters; `node --test 'scripts/*.test.mjs'` passes.
- [ ] 3.2 Update `.github/workflows/pr-check.yml`'s spec-validation job (no full history needed, comments say what
      runs), `docs/test-strategy.md`'s Spec validation section and `docs/change-workflow.md`'s check list. Verify: no
      file outside `openspec/changes/archive/` mentions `check-requirement-length` or the lenient main-spec step.

## 4. Regression run

- [ ] 4.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.
