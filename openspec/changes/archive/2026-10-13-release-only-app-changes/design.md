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

### 2. Skips are recorded once, and the notes read the record

`scripts/ci/release-skips.sh <last dist tag> <target>` walks the first-parent commits since the last release, oldest
first, skipping any that already carry a `dist-skip-v*` tag, and judges each with `app-changed.sh <commit>^1 <commit>`.
One that changed no app path gets a lightweight `dist-skip-v<count>-<short sha>` tag (the same shape as `dist-v*`);
the release run pushes them. The run publishes nothing when every commit since the last release carries a skip tag.

The notes never judge paths themselves: `choose-release-notes.sh` drops a merged pull request whose merge commit has a
skip tag (its section and the changes it archived), and `release-notes.sh` drops a change archived by a commit with a
skip tag. Everything else is collected. So the rule is explicit: only a recorded skip leaves notes out. An app merge
whose release didn't happen (a failed upload, a run GitHub replaced in the queue) has no skip tag and no `dist-v*` tag,
so its notes go out with the next release; a merge never judged at all is collected too, rather than dropped.

*Alternative (the first implementation):* re-judge each commit by path whenever notes are built. Rejected in review:
the skip would be implicit and could change with the path list, instead of being a decision the release made and
recorded. *Alternative:* a per-PR declaration. Rejected: the developer chose decisions recorded by the release run.

Pruning: once a release covers a skip, the skip tag is never read again (notes only look since the last release), so
the release run deletes every `dist-skip-v*` tag its new `dist-v*` tag contains (`scripts/ci/prune-release-skips.sh`,
listing only that pattern, so release tags are never touched). At most the skips since the last release exist. The
build is already out when it prunes, so a failed deletion only warns; the next release deletes them again.

A change whose code and archive landed in separate merges (#20, then #21) is still listed: its archive merge is a skip,
but its code merge isn't, and its pull request's section, if any, is used; only the generated name is lost, as the
archive merge's name.

### 3. The preview asks what the release will ask

`release-notes-preview.sh` runs on GitHub's merge of the pull request into `main` (HEAD) and makes the release's
decisions there: `release-skips.sh <last dist tag> HEAD` as local tags, deleted on exit (only the release run pushes
skips). All skips → "Nothing to release: no change to the app since …, this pull request included". Otherwise the
notes, read from those tags exactly as the release will, so a docs pull request merged while an app change waits on
`main` shows the release that will go out, without itself in the notes. *Alternative:* judge only
`HEAD^1..HEAD`. Rejected after implementing it: with an unreleased app change already on `main`, it would say "nothing
to release" for a merge that does publish.

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
