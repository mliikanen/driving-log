# Design

## Context

See proposal.md for why. Today (`release.yml`, "Decide what to release"): find the newest checked commit on `main`
since the last `dist-v*` tag; skip if already tagged; build the notes with `scripts/ci/choose-release-notes.sh` (each
merged pull request's `## Release notes` section, else the names of the changes it archived, plus changes archived
without a pull request, via `scripts/release-notes.sh`); publish if the notes are non-empty. Pull requests are squash
merged, so a merged pull request is one commit on `main` (its `merge_commit_sha`), and its diff against its first parent
is exactly what it changed.

What reaches the APK: `shared/src/{commonMain,androidMain}` (and the SQLDelight and resource files under them),
`androidApp/src/{main,production}`, both modules' `build.gradle.kts`, `gradle/` (`libs.versions.toml`, the wrapper),
and the root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`. Not: tests (`shared/src/*Test`,
`androidApp/src/test*`), `shared/src/iosMain`, `iosApp/`, the `fake` flavor, `config/` (lint), `docs/`, `openspec/`,
`maestro/`, `scripts/`, `.github/`, `.herd/`, and the license files (not packaged).

## Goals / Non-Goals

**Goals:**
- No build for a merge that changes nothing in the app; the summary says why.
- Notes list only what changed the app.
- The preview on a pull request matches what merging it does.

**Non-Goals:**
- Changing the local manual fallback's decision to publish.
- A per-pull-request override.

## Decisions

### 1. Decide by paths, in one script

`scripts/ci/app-changed.sh <from> <to>` exits 0 when `git diff <from> <to>` touches an app path, 1 when it doesn't
(2 on error). The paths are git pathspecs, inclusions and `:(exclude)` entries, kept together in that script with a
comment per entry. `<to>^1 <to>` asks about one commit.

*Alternative: decide by the archived change's `.openspec.yaml`* (`skip_specs: true` means tooling). Rejected: not
exact either way (a `skip_specs` change can touch app code, a change with deltas can be docs only), and a merge that
archives nothing but bumps a dependency must still publish. *Alternative: an opt-out in the PR description.* Rejected
for the default (easy to forget, and wrong by default); paths need no author discipline. Erring towards "app": a
build-script or dependency change that turns out not to affect the APK publishes a build, which is today's behavior
anyway.

### 2. The release asks about the whole range; the notes ask per commit

`release.yml`: after the already-tagged check, `scripts/ci/app-changed.sh <last dist tag> HEAD` (or the whole history
when there is no tag yet). No app change → publish nothing, no tag, summary "Nothing to release: no change to the app
since <tag>". The notes check comes after, unchanged: an app change with no notes (a dependency bump with no section and
no archived change) still publishes nothing, as today.

Notes: `choose-release-notes.sh` skips a merged pull request whose `merge_commit_sha` changed no app path (neither its
section nor the changes it archived count, so they don't fall through to the name list either). `release-notes.sh`
skips an archived change whose archiving commit (the commit that added its directory under `archive/`) changed no app
path. A known edge: a change whose code and archive landed in separate merges (#20, then #21) is listed by neither if
the archive merge changed no app path, but its code merge still triggers the release, and its pull request's section
(if any) is used.

### 3. The preview asks about the merge

`release-notes-preview.sh` runs on GitHub's merge of the pull request into `main`: `app-changed.sh HEAD^1 HEAD` is
exactly what merging changes. No app change → "Nothing to release: this merge changes no app code (`scripts/ci/app-changed.sh`)."
Otherwise the notes, filtered the same way, with the open pull request judged by `HEAD^1..HEAD`.

## Risks / Trade-offs

- [The path list misses something that affects the APK] → Inclusions are broad (all of `shared/` and `androidApp/`
  but named exclusions, all of `gradle/`, every root build file); each exclusion is commented. A missed release is
  visible: the preview says "Nothing to release" on a pull request that does change the app.
- [An app change publishes with notes that don't mention it] → Unchanged from today: notes come from sections and
  archived changes, and #18's rule asks for a section.
- [A test-only change in `shared/src/commonTest`] → Excluded, so it doesn't publish; tests aren't in the APK.

## Migration Plan

None. Merging this change is the first merge the rule applies to (the workflow runs from `main`). Rollback is
reverting the merge.
