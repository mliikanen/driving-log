## Why

OpenSpec warns about a requirement over 500 characters, so the main specs can't be validated with `--strict` (issue
#13). Batches A to C brought the count from 76 to 12. This is the last batch: `app-shell` (5 long), `vehicle-color` (5)
and `event-pictures` (2). Once they are short, spec validation can go back to plain `openspec validate --all --strict`,
which is what #13 asks for.

## What Changes

- Every requirement over 500 characters in the three specs is rewritten to 500 characters or fewer, **with no change in
  behavior**: every SHALL, SHALL NOT and MAY in the old text is still stated, by the same requirement or by a new one.
  As in the earlier batches, each keeps its name and every scenario word for word.
- 6 are only shortened; 6 are also split into 12 new requirements, each with its own scenario (the design lists them).
- **Spec validation is strict again.** `scripts/validate-specs.sh` stays the one command the final regression run and
  CI's `spec-validation` job call, and becomes `openspec validate --all --strict --no-interactive`. The
  requirement-length ratchet that bridged the gap (`scripts/check-requirement-length.mjs` and its tests) is deleted,
  and `docs/test-strategy.md`, `docs/change-workflow.md` and the CI job's comments say what is checked now.
- Tests: each new scenario gets the test that checks it, existing or new.

Out of scope: any change in behavior, and fixing wording that is stale but not too long (filed as issues).

**In-flight branches:** `add-vehicle-color-theme` modifies "Material Design 3 theming", `add-vehicle-hiding` "Home
screen offers the main actions" and `sweep-event-pictures` "Photos are stored in the application's local file system",
all three shortened here. Their MODIFIED blocks need rebuilding on the new text before they are applied (#33 for
`add-vehicle-hiding`; an issue for the other two).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `app-shell`: 5 requirements shortened, 3 of them split into 7 new requirements.
- `vehicle-color`: 5 requirements shortened, 3 of them split into 5 new requirements.
- `event-pictures`: 2 requirements shortened.

## Impact

- The three main specs once archived: no requirement over 500 characters is left anywhere, and `openspec validate --all
  --strict` passes.
- `scripts/validate-specs.sh`, `scripts/check-requirement-length.mjs` and `.test.mjs` (deleted),
  `.github/workflows/pr-check.yml`, `docs/test-strategy.md`, `docs/change-workflow.md`.
- No app change: merging this publishes no build and needs no release notes.
