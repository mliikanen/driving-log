# Change workflow

**Every change lives on its own branch, `change/<name>`, from its first proposal commit until it is merged.**
`main` gets a change only as one merge: code and archive together. So `main`'s `openspec/changes/` holds only
`archive/`, and `openspec/specs/` describes only what is built. This holds today, while changes are still applied
interactively, and it is also how the herd (the planned unattended pipeline) finds work once Driving Log is onboarded
to it. How the herd itself works is documented only in the herd repository: `../herd/docs/using-the-herd.md` for
using it, `../herd/docs/design.md` for its design.

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

On the change's branch, in this order:

1. `/opsx:apply`, then the final regression run. The PR stays a **draft** while tasks are open.
2. **Mark the PR ready for review** once every task is done and the regression run passes. That's the signal that
   review starts.
3. **Review cycles**: Copilot's review and/or a person's. Fix what's raised on the branch, reply to each comment and
   resolve its thread, and repeat until nothing is open, including the "Previously missed" notes in Copilot's review
   summary, which have no thread. A fix that changes behavior updates the change's spec deltas, design and tasks too.
4. **`/opsx:archive` as the last commit**, once review is done: it syncs the deltas into `openspec/specs/` and moves the
   change into `archive/`. Archiving earlier would mean editing the main specs and archived artifacts by hand when
   review changes something. The checks must pass again after it.
5. **Merge** the PR into `main`. The branch must be up to date with `main` to merge (the ruleset requires it), so
   merge `main` into it first if `main` has moved on.

## With the herd

Once Driving Log is onboarded, the herd implements, reviews and archives ready changes on their branches; you
propose, run final approval and merge. What that looks like from your side (marking ready, revising a ready change,
`needs-human` stops, watching the herd) is in the herd repository's guide, `../herd/docs/using-the-herd.md`. This
section covers only what's specific to Driving Log.

### Before marking a proposal ready

`/herd-ready` checks the herd's generic rules (the guide lists them). On top of those, for this project:

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

### Final approval: Maestro

The herd can't run Maestro, because Android emulators in containers are too fragile, so running it is your final
check. Instructions for the change's branch (also shown in the PR and the status pane, from `.herd/project.yaml`):

1. Build and install the `fake` flavor: `./gradlew :androidApp:assembleFakeDebug`, then
   `adb install -r androidApp/build/outputs/apk/fake/debug/androidApp-fake-debug.apk`.
2. Run the manifests of the areas the change touches: `maestro/run.sh <area>...` (areas in `CLAUDE.md`).
3. Record pass, or the failing flow and what happened, with `/herd-resolve`. A failure goes back to the herd as new
   tasks; the draft PR just gets more commits.

## Merging

You merge the PR once its checks pass (and, with the herd, once it marks the PR ready for review). Every PR, and every
push to `main`, runs three required checks (`.github/workflows/pr-check.yml`): `tests-and-build` (shared tests, debug
build), `code-quality` (`./gradlew codeQuality`) and `spec-validation` (`openspec validate --all --strict`). A fourth,
informational one, `release-notes` (`release-notes.yml`), shows what merging would publish and updates when you edit
the description.

The ruleset on `main` (`main-is-pr-only`, exported in `.github/rulesets/main.json`) blocks the merge until all three
pass on the PR's latest commit, with the branch up to date with `main`. `main` takes changes only through PRs, from
everyone: there is no bypass, the administrator included, and no approving review is required. A second ruleset
(`main-retain`, `.github/rulesets/main-retain.json`) stops `main` from being deleted or force-pushed. Merging publishes
to the testers (`docs/distribution.md`).

## Cleaning up after merge

Remove a worktree or branch once it has no further use, so the `change/*` branches keep showing only what's in
flight. GitHub deletes a PR's branch when the PR is merged (the repository's "Automatically delete head branches"
setting), so what's left is local:

- **Before removing anything, check two things.** Both must hold, or keep the worktree and branch:
  - The worktree has nothing unsaved: `git -C ../driving-log-<name> status --short` is empty (`git worktree remove`
    refuses otherwise). Gitignored local files copied in for the build (`local.properties`,
    `androidApp/src/production/google-services.json`) are fine to drop.
  - The branch's PR is merged and the local branch is exactly what it merged: the `headRefOid` from
    `gh pr list --head <branch> --state merged --json number,headRefOid` equals `git rev-parse <branch>`. A commit
    made after the merge would make them differ, and deleting the branch would lose it. Look the PR up by branch,
    not with `git branch --merged`, which doesn't recognize squash merges.
- **Then remove the change's worktree and local branch, and prune the deleted remote branch:**
  `git worktree remove ../driving-log-<name>`, `git branch -D change/<name>` and `git fetch --prune`. Use `-D`: a
  squash merge leaves the branch's own commits off `main`, so `-d` refuses even though the check above passed. The
  same goes for `chore/*` branches. A branch merged before the setting was turned on is deleted by hand:
  `git push origin --delete <branch>`.
- **A PR closed without merging keeps its branch** until you decide the change is abandoned. It may be reopened or
  reworked.

With the herd, its planner skills remove the worktrees they created, and the orchestrator prunes merged branches from
its mirror.

## Onboarding status

Onboarding follows the herd repo's "Onboarding a project" steps. Driving Log specifics:
- The proposals that were on `main` were moved to `change/<name>` branches (one commit removed them from `main`; each
  branch holds only its own proposal).
- `.herd/project.yaml` is drafted (its comments explain the values); `herd init` validates it once the herd
  exists.
- CI is set up (`add-ci-workflows`): the workflows (`pr-check.yml`, `release-notes.yml`, `release.yml`), the rulesets
  on `main`, and the one-time setup in `docs/distribution.md` (the `firebase-deployment` environment limited to
  `main`, its secrets, and Workload Identity Federation for the upload).
- `.herd/toolchain.Dockerfile` doesn't exist yet.
- Publishing to Firebase on merge is this project's release automation, separate from the herd
  (`docs/app-distribution.md`).
