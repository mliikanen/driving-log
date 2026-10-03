# Change workflow

**Every change lives on its own branch, `change/<name>`, from its first proposal commit until it is merged.**
`main` gets a change only as one merge: code and archive together. So `main`'s `openspec/changes/` holds only
`archive/`, and `openspec/specs/` describes only what is built. This holds today, while changes are still applied
interactively, and it is also how the herd (the planned unattended pipeline) finds work once Driving Log is onboarded
to it. How the herd itself works is documented only in the herd repository (`../herd/docs/design.md`).

## Proposing

1. Create the branch from an up-to-date `main`, in a worktree so your main checkout stays on `main`:
   `git worktree add ../driving-log-<name> -b change/<name> main`. (Once the herd is set up, `/herd-propose` does this
   and the steps below.)
2. Before proposing, see what else is in flight: `git fetch` and `git branch -r --list 'origin/change/*'`, or the open
   draft PRs. If the new change needs another one merged first, record it in its `.openspec.yaml`:
   `depends_on: [<other-change>]`.
3. `/opsx:propose` in the worktree, review and refine the artifacts, and commit on the branch.
4. Push the branch and open a **draft PR**. That's where the proposal is reviewed. Refining it is more commits on the
   branch.

Never commit a proposal to `main`.

## Applying, until the herd is onboarded

On the change's branch: `/opsx:apply`, the final regression run, `/opsx:archive`, then merge into `main` (through
the PR once branch protection is enabled). Merge `main` into the branch first if it has moved on.

## With the herd

1. **You mark the proposal ready** (below). From then on the herd owns it.
2. **The herd** implements `tasks.md` item by item on the branch, has each task reviewed, runs the gate, reviews the
   whole change, and updates the draft PR.
3. **You run final approval** (Maestro, below) on the branch and record the result.
4. **The herd** archives the change and marks the PR ready for review. **You merge it.**

The herd stops and waits for you (`needs-human`) whenever it can't continue on its own. Run `herd` to open its
herdr session: the status pane lists what's waiting on you first, with the reason, and every change still being
drafted.

### Before marking a proposal ready

The herd's `herd-ready` skill (installed by `herd init`) checks the generic rules: the change validates, each
`tasks.md` item is one reviewable commit, sections are in dependency order, and so on. On top of those, for this
project:

- **Android only.** Nothing in the change may need a Mac, Xcode, a physical device or the `production` flavor's
  real Firebase/Google Sign-In; those are the manifest's `missing_capabilities`, and a task needing one stops the
  change at `needs-human`. Keep iOS work in its own change and apply it interactively.
- **Maestro tasks write or update flows; they don't run them.** Running Maestro is your final approval. A task
  like "run the vehicles manifest" can't be done by the herd. Phrase it as a check the gate can run, or leave it
  to final approval.
- **The gate is `CLAUDE.md`'s final regression run** (`./gradlew :shared:allTests :androidApp:assembleDebug
  codeQuality` and `openspec validate --all --strict`). Every task must leave it green.
- **Anything the agents need from outside the repo** (test photos, sample receipts) is committed with the
  proposal, as `maestro/assets/` already does, so the herd doesn't have to stop and ask for it.

### Marking ready

`/herd-ready` checks the proposal, then sets `ready: true` in the change's `.openspec.yaml` on its branch, commits
and pushes. A change whose `depends_on` changes aren't merged yet waits for them.

After that, don't `/opsx:apply` it yourself. To revise a ready proposal, set `ready: false` on the branch (the herd
stops after the step it's on), make your changes, and set it back. If you push while the herd is working, its own
next push is rejected and that step is redone from your commit: nothing you push is lost.

### When the herd asks for you

A `needs-human` stop names its reason: a task rejected too many times, scope growing past the cap, a merge
conflict with `main`, something the pipeline can't do, or a file it needs. Fix it on the branch (edit `tasks.md`,
resolve the conflict, commit the requested file, revise the proposal) and mark the stop resolved. `/herd-resolve`
walks you through it. The herd picks the change back up on its next pass.

### Final approval: Maestro

The herd can't run Maestro, because Android emulators in containers are too fragile, so running it is your final
check. Instructions for the change's branch (also shown in the PR and the status pane, from `.herd/project.yaml`):

1. Build and install the `fake` flavor: `./gradlew :androidApp:assembleFakeDebug`, then
   `adb install -r androidApp/build/outputs/apk/fake/debug/androidApp-fake-debug.apk`.
2. Run the manifests of the areas the change touches: `maestro/run.sh <area>...` (areas in `CLAUDE.md`).
3. Record pass, or the failing flow and what happened, with `/herd-resolve`. A failure goes back to the herd as new
   tasks; the draft PR just gets more commits.

## Merging

You merge the PR once its checks pass (and, with the herd, once it marks the PR ready for review). Every PR runs two
checks (`.github/workflows/pr-check.yml`): `tests-and-build` (shared tests, debug build), `code-quality`
(`./gradlew codeQuality`) and `spec-validation` (`openspec validate --all --strict`). A fourth, informational one,
`release-notes` (`release-notes.yml`), shows what merging would publish, and updates when you edit the description. Once the ruleset
on `main` exists (`.github/rulesets/main.json`, `docs/distribution.md`'s one-time CI setup), all three must pass on the PR's
latest commit, with the branch up to date, before it can merge, and `main` takes changes only through PRs, from
everyone, with no bypass. Until the GitHub plan allows rulesets, the checks run and report but don't block. Merging
publishes to the testers (`docs/distribution.md`).

**Still to decide:** whether every PR also needs a human approval, or the herd's review plus your Maestro pass is
enough.

## Onboarding status

Onboarding follows the herd repo's "Onboarding a project" steps. Driving Log specifics:
- The proposals that were on `main` were moved to `change/<name>` branches (one commit removed them from `main`; each
  branch holds only its own proposal).
- `.herd/project.yaml` is drafted (its comments explain the values); `herd init` validates it once the herd
  exists.
- The CI workflows exist (`pr-check.yml`, `release.yml`); the ruleset that makes the checks block merging, and the
  release secrets, wait on the one-time CI setup in `docs/distribution.md`.
- `.herd/toolchain.Dockerfile` doesn't exist yet.
- Publishing to Firebase on merge is this project's release automation, separate from the herd
  (`docs/app-distribution.md`).
