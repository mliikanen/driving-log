# Design

The spec part follows batches A to C (`openspec/changes/archive/2026-10-13-split-long-requirements-a/design.md`):
shorten first, split only what can't fit, keep every requirement's name and every scenario word for word, state every
old SHALL, SHALL NOT and MAY again, and give each new requirement one scenario and the test that checks it.

## Context

- Three of the requirements are also modified by unapplied proposals from 2026-10-03: "Material Design 3 theming"
  (`add-vehicle-color-theme`), "Home screen offers the main actions" (`add-vehicle-hiding`) and "Photos are stored in
  the application's local file system" (`sweep-event-pictures`). As in batch C, they are shortened anyway, and the
  rebase is recorded on #33 and a new issue.
- `scripts/validate-specs.sh` runs three steps since `ratchet-spec-validation`: changes strictly, main specs leniently,
  and the requirement-length ratchet (`scripts/check-requirement-length.mjs`). Its own header and
  `docs/test-strategy.md` say #13's last change replaces them with `openspec validate --all --strict`.
- The project has no Compose UI tests, and the color animation (`ui/color/AnimatedColor.kt`) is a private class
  inside a composable.

## Decisions

### 1. What is split out

`app-shell`: the schemes' palette roles, the contrast minimums and the launch window and system bars out of the
theming requirement; asking at the action out of the permissions requirement; the tile size, the first action and the
disabled actions out of the Home screen requirement. `vehicle-color`: the row of segments and the edit screen's saved
color out of the presets; the extraction rules and the segment's lifetime out of "A picture sets the color"; the
interrupted and skipped animations out of the animation requirement.

### 2. `scripts/validate-specs.sh` stays, with one step

The final regression run, CI, `CLAUDE.md`, `openspec/config.yaml` and `.herd/project.yaml` all call
`scripts/validate-specs.sh`, so it stays the one name for "validate the specs" and its body becomes
`openspec validate --all --strict --no-interactive`. *Alternative:* call OpenSpec directly everywhere. Rejected: five
places to change now, and again whenever the command changes.

The ratchet and its tests are deleted. What it caught is now caught by strict validation of the main specs: a change's
archive is the last commit of its pull request, so CI validates the archived specs before the merge, and any
requirement over 500 characters fails. The one thing strict validation doesn't do is length-check a MODIFIED
requirement in a change that is not archived yet; it then fails at the archive commit instead of earlier.
`docs/test-strategy.md` says so. The spec-validation job keeps running `node --test 'scripts/*.test.mjs'` for the
remaining script tests (`release-gate.test.mjs`), and no longer needs full history.

### 3. The animation scenario is checked on screen

The new scenario of "A color animation can be interrupted or skipped" can't be unit-tested without making
`AnimatedColorState` visible to tests, an app code change this batch avoids. It is checked on the emulator and
recorded, and the gap is reported in the scenario-test audit that follows #13 (#27 is about reporting such gaps).

## Risks / Trade-offs

- **A reworded sentence quietly changes meaning.** → One task per spec compares the old and the new text.
- **A pull request merged without its archive** would skip the length check that the archive commit gives. → The
  workflow already requires the archive as the last commit, and the archived specs are validated on `main` after
  every merge too.
