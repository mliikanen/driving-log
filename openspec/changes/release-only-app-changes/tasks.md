# Tasks

Every task leaves `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh`
green. No Maestro: no app code changes. The release itself can't run outside `main`; it is checked by the tests and by
the preview on this change's own pull request.

## 1. What counts as the app

- [x] 1.1 Add `scripts/ci/app-changed.sh <from> <to>` (design Decision 1): exit 0 when the diff touches an app path, 1 when not, 2 on error; the pathspecs in one commented list. Verify: run against real history, it says #17's merge (`74d1501`) changed the app and #19's (`b9f51f7`) and #21's (`bf6a4d0`) didn't.
- [x] 1.2 Add `scripts/release-gate.test.mjs` (`node:test`, throwaway git repositories) covering `app-changed.sh`: a change in `shared/src/commonMain`, `androidApp/src/main`, `gradle/libs.versions.toml` or a root build file counts; one only in `shared/src/commonTest`, `shared/src/iosMain`, `androidApp/src/fake`, `docs/`, `openspec/`, `.github/`, `scripts/` or `maestro/` doesn't; a bad commit exits 2. CI's `spec-validation` job runs it: its test step becomes `node --test 'scripts/*.test.mjs'` already, so check it is picked up. Verify: `node --test 'scripts/*.test.mjs'` passes.

## 2. The release, the notes and the preview

- [x] 2.1 `release.yml`: after the already-tagged check, publish nothing (no tag, summary "Nothing to release: no change to the app since …") when `app-changed.sh` says the range since the last `dist-v*` tag changed no app path (design Decision 2). Verify: the YAML is valid and the step reads as described; the preview (2.3) exercises the same script.
- [x] 2.2 Notes filtering (design Decision 2): `choose-release-notes.sh` skips a merged pull request whose merge commit changed no app path; `release-notes.sh` skips an archived change whose archiving commit changed no app path. Add tests for `release-notes.sh` to the test file: an archived change whose commit also changed app code is listed, one archived alone is not. Verify: the tests pass, and `scripts/release-notes.sh` on `main` prints nothing since `dist-v207-bf6a4d0` (the one change archived since, `ratchet-spec-validation`, changed no app path).
- [x] 2.3 `release-notes-preview.sh` (design Decision 3): "Nothing to release: this merge changes no app code" when `app-changed.sh HEAD^1 HEAD` says so. Verify: this change's own pull request's `release-notes` check shows that message (reproduced locally: the script on GitHub's merge of #23 into `main` prints "Nothing to release: no change to the app since dist-v208-b9f51f7, this pull request included"; the check-run API doesn't expose job summaries).

## 3. Docs

- [x] 3.1 `docs/app-distribution.md` ("Publishing from CI": the app-change rule and where the path list lives; "Release notes": what is left out) and `docs/distribution.md` ("Cutting a release": a merge that changes no app code publishes nothing). Verify: both describe the rule as implemented, and name `scripts/ci/app-changed.sh`.

## 4. Final regression run

- [x] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh`, without Maestro. Verify: both succeed.

## Workflow follow-up

- After merging: the Release run for the merge says "Nothing to release: no change to the app since …" and creates no tag. Close issue #22.
