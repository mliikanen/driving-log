# Proposal

## Why

Issue #22. Every merge to `main` that archives an OpenSpec change publishes a new Firebase App Distribution build,
whether or not the app changed: archiving gives `scripts/ci/choose-release-notes.sh` a change name to list, and a
non-empty note is all `release.yml` needs to publish. Docs, spec, CI and tooling changes therefore send testers a
build of an unchanged app with a note that means nothing to them (`dist-v202-33ec90c` for `upgrade-openspec-cli`,
`dist-v207-bf6a4d0` for #21's archive of `retire-tooling-specs`, and #19's merge).

## What Changes

- **A release needs a change to the app.** CI publishes only when something that ends up in the APK changed since the
  last `dist-v*` tag: `shared/` and `androidApp/` (their test source sets, `shared`'s iOS sources and the `fake` flavor
  excepted), `gradle/` (dependency versions, the wrapper) and the root build files. Otherwise it publishes nothing,
  creates no tag, and the run's summary says there was no change to the app since the last distribution.
- **Skips are explicit and recorded.** The release run judges every merge since the last release that has no decision
  yet, and tags each one that changed nothing in the app `dist-skip-v<count>-<sha>` (pushed). It publishes nothing
  when every merge since the last release is such a skip.
- **Release notes leave out exactly the recorded skips.** A merged pull request or an archived change whose commit
  carries a skip tag contributes nothing; everything else is collected, so an app merge that should have been released
  but wasn't (a failed upload, a replaced run) has its notes in the next release that is made.
- **The pull request preview shows the same decision.** The `release-notes` check says "Nothing to release: this merge
  changes no app code" for such a pull request, and otherwise shows the notes as today.
- **One definition of "the app".** A single script, `scripts/ci/app-changed.sh`, holds the list of app paths;
  `scripts/ci/release-skips.sh` asks it once per merge and records the answer, and the notes read the record.

Out of scope:

- The local manual fallback (`scripts/distribute.sh`): a person running it has decided to publish. Its draft notes do
  get the same filtering, since they come from the same `scripts/release-notes.sh`.
- Choosing per pull request whether to publish (an explicit opt-in or opt-out). If the path rule proves wrong, that
  can be added later.

## Capabilities

None: `skip_specs: true`. Releasing is tooling, documented in `docs/app-distribution.md` and `docs/distribution.md`
(the `app-distribution` capability was retired into them by `retire-tooling-specs`).

## Impact

- New `scripts/ci/app-changed.sh`; changed `.github/workflows/release.yml` (the "Decide what to release" step),
  `scripts/ci/choose-release-notes.sh`, `scripts/release-notes.sh`, `scripts/ci/release-notes-preview.sh`.
- Tests: a `node:test` file for the path rule and the notes filtering in throwaway git repositories, run by CI's
  `spec-validation` job with the existing script tests.
- Docs: `docs/app-distribution.md` ("Publishing from CI", "Release notes"), `docs/distribution.md` ("Cutting a
  release").
- No app code, so no release notes. Merging this change is itself the first merge the new rule skips.
