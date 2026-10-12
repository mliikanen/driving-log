# Tasks

Every task leaves `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and the spec validation green
(`openspec validate --all` until task 2.1 adds `scripts/validate-specs.sh`, the script after it). No Maestro: this
change touches no app code, so no manifest covers it.

## 1. The ratchet

- [x] 1.1 Add `scripts/check-requirement-length.mjs` (design Decision 2): Node, no dependencies, reads `openspec list --json`, `openspec show <change> --json --deltas-only` and `openspec show <spec> --type spec --json`, and fails (exit 1) naming change, spec, requirement and both lengths when a MODIFIED requirement's text is over 500 characters and longer than the main one; fails loudly when an expected JSON field is missing. Verify: it passes on `main` (no active change) and on this change's branch.
- [x] 1.2 Add `scripts/check-requirement-length.test.mjs` (`node:test`), each case in a throwaway OpenSpec tree (a temp dir with `openspec/specs/` and one change), covering the ratchet's cases (design Decision 2): 600 → 650 fails and names both lengths; 450 → 520 fails; 900 → 700 passes; 900 unchanged with an added scenario passes; no active change passes. Verify: `node --test 'scripts/*.test.mjs'` passes.

## 2. One entry point

- [x] 2.1 Add `scripts/validate-specs.sh` (design Decision 3): `openspec validate --changes --strict --no-interactive`, `openspec validate --specs --no-interactive` and the ratchet, every step run, failing if any failed. Add cases to the test file for the strict change validation (design Decision 1): a change adding a 520-character requirement fails; a change whose `.openspec.yaml` has an unknown key fails; a main spec with a 600-character requirement and no change passes. Verify: `node --test 'scripts/*.test.mjs'` passes and `scripts/validate-specs.sh` passes on this branch.
- [x] 2.2 CI: the `spec-validation` job in `.github/workflows/pr-check.yml` runs `node --test 'scripts/*.test.mjs'` and then `scripts/validate-specs.sh` instead of `openspec validate --all`. Verify: the job passes on this change's PR, and its log shows both steps.
- [x] 2.3 Point everything else at the script: `CLAUDE.md` (the useful-CLI line and the final regression run), the tasks rule in `openspec/config.yaml`, `docs/test-strategy.md` (the "final regression run" row, and a short section on what the script checks: strict for changes, lenient main specs, the length ratchet, and that #13's last batch replaces it with `openspec validate --all --strict`), `docs/change-workflow.md` (the herd gate and the required-checks paragraph), and `.herd/project.yaml`'s gate. Verify: `grep -rn 'openspec validate --all' --exclude-dir=archive --exclude-dir=.claude .` finds only the notes that #13 brings `openspec validate --all --strict` back (`scripts/validate-specs.sh`, `scripts/check-requirement-length.mjs`, `docs/test-strategy.md`) and the `test-strategy` scenario the proposal names as out of date.

## 3. Final regression run

- [x] 3.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh`, without Maestro. Verify: both succeed.

## Workflow follow-up

- After merging: `add-vehicle-hiding` splits its two new requirements over 500 characters before its next push passes.
- Issue #13: its batches follow; the last one replaces the body of `scripts/validate-specs.sh` with
  `openspec validate --all --strict` and deletes the ratchet.
