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
- [x] 2.3 `release-notes-preview.sh` (design Decision 3): on GitHub's merge of the pull request into `main` (HEAD), make the release's decisions for the whole range from the last `dist-v*` tag through HEAD (`release-skips.sh`, as local tags removed on exit) and say "Nothing to release: no change to the app since …, this pull request included" when every merge in it is a skip; otherwise show the notes the release will publish. Verify: this change's own pull request's `release-notes` check shows that message (reproduced locally on GitHub's merge of #23 into `main`; the check-run API doesn't expose job summaries), and no skip tag is left behind.

## 3. Docs

- [x] 3.1 `docs/app-distribution.md` ("Publishing from CI": the app-change rule and where the path list lives; "Release notes": what is left out) and `docs/distribution.md` ("Cutting a release": a merge that changes no app code publishes nothing). Verify: both describe the rule as implemented, and name `scripts/ci/app-changed.sh`.

## 4. Final regression run

- [x] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `scripts/validate-specs.sh`, without Maestro. Verify: both succeed.

## 5. Explicit skips (review)

- [x] 5.1 Record skips instead of re-judging paths (design Decision 2): `scripts/ci/release-skips.sh` tags each unjudged merge since the last release that changed no app path `dist-skip-v<count>-<sha>`; `release.yml` pushes them and publishes nothing when every merge since the last release is a skip; `choose-release-notes.sh` and `release-notes.sh` leave out exactly the skip-tagged merges; the preview makes the same decisions with local tags it removes. Verify: the tests for recorded skips (an app merge is not skipped, a decision is not made again, only merges since the last release are judged) and for the notes (a skip is left out, a never-judged merge is collected, an unreleased app merge is collected into the next release) pass; the preview on GitHub's merge of #23 still says "Nothing to release" and leaves no tag behind.
- [x] 5.2 Docs: `docs/app-distribution.md` and `docs/distribution.md` describe the recorded skips and that only they leave notes out. Verify: both name `dist-skip-v*` and `scripts/ci/release-skips.sh`.

## Workflow follow-up

- After merging: the Release run for the merge says "Nothing to release: no change to the app since …" and creates no tag. Close issue #22.
