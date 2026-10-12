# Tasks

Every task leaves `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`
green. No Maestro: no app code changes, so no manifest covers it.

## 1. Move what the docs don't say yet

- [x] 1.1 `test-strategy`: check each of its nine requirements and their scenarios against the doc in design Decision 2, and add to that doc any rule or case it does not state. At least: in `docs/change-workflow.md`'s "Merging", that CI does not build or test iOS targets, that `spec-validation` reports without waiting for the build, and that a failing `tests-and-build` names the failing step. Verify: for every requirement, the doc section named in its REMOVED delta's Migration states it (list any additions in the PR description).
- [x] 1.2 `code-quality`: the same check of its five requirements against `docs/code-quality.md`. Verify: as in 1.1.
- [x] 1.3 `app-distribution`: the same check of its six requirements against `docs/app-distribution.md` and `docs/distribution.md`. Verify: as in 1.1.

## 2. Keep tooling out of specs

- [x] 2.1 Add the sentence of design Decision 3 to `openspec/config.yaml`'s `context`. Verify: `openspec instructions proposal --change retire-tooling-specs --json` shows it in `context`.

## 3. Final regression run

- [x] 3.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all`, without Maestro. Verify: both succeed.

## Workflow follow-up

- `/opsx:archive` deletes `openspec/specs/{test-strategy,code-quality,app-distribution}/` (`retire_capabilities: true`); check it did.
