# Change workflow

**Every change lives on its own branch, `change/<name>`, from its first proposal commit until it is merged.**
`main` gets a change only as one merge: code and archive together. So `main`'s `openspec/changes/` holds only
`archive/`, and `openspec/specs/` describes only what is built. This holds today, while changes are still applied
interactively, and it is also how the herd (the planned unattended pipeline) finds work once Driving Log is onboarded
to it. How the herd itself works is documented only in the herd repository: [using the herd][herd-guide] and
[its design][herd-design].

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
   review starts. Before marking it ready, give the PR description a **`## Release notes` section**: plain-language
   bullets for testers about what they will notice, not how it was built. It is what merging publishes
   (`docs/distribution.md`); without it the notes are only the archived changes' names. The `release-notes` check's job
   summary shows the result.
3. **Review cycles**: Copilot's review and/or a person's. Fix what's raised on the branch, reply to each comment and
   resolve its thread, and repeat until nothing is open, including the "Previously missed" notes in Copilot's review
   summary, which have no thread. A fix that changes behavior updates the change's spec deltas, design and tasks too.
4. **`/opsx:archive` as the last commit**, once review is done: it syncs the deltas into `openspec/specs/` and moves the
   change into `archive/`. Archiving earlier would mean editing the main specs and archived artifacts by hand when
   review changes something. The checks must pass again after it.
5. **Squash-merge** the PR into `main`, the only merge method the repository allows. The branch doesn't need to be up
   to date with `main` first; CI on `main` runs again after the merge.

## With the herd

Once Driving Log is onboarded, the herd implements, reviews and archives ready changes on their branches; you propose,
run the Maestro check in PR review and merge. What that looks like from your side (marking ready, revising a ready
change, `needs-human` stops, watching the herd) is in the herd repository's guide, [Using the herd][herd-guide]. This
section covers only what's specific to Driving Log.

### Before marking a proposal ready

`/herd-ready` checks the herd's generic rules ([the guide][herd-ready-rules] lists them). On top of those, for this
project:

- **Android only.** Nothing in the change may need a Mac, Xcode, a physical device or the `production` flavor's
  real Firebase/Google Sign-In; those are the manifest's `missing_capabilities`, and a task needing one stops the
  change at `needs-human`. Keep iOS work in its own change and apply it interactively.
- **Maestro tasks write or update flows; they don't run them.** Running Maestro is your check in PR review. A task
  like "run the vehicles manifest" can't be done by the herd. Phrase it as a check the gate can run, or leave it
  to your check.
- **The gate is `CLAUDE.md`'s final regression run** (`./gradlew :shared:allTests :androidApp:assembleDebug
  codeQuality` and `scripts/validate-specs.sh`). Every task must leave it green.
- **Anything the agents need from outside the repo** (test photos, sample receipts) is committed with the
  proposal, as `maestro/assets/` already does, so the herd doesn't have to stop and ask for it.

### Your check in PR review: Maestro

The herd doesn't run Maestro yet (there's no harness for an emulator in a worker), so running it is your check, as a
reviewer of the PR (`pr_review.human_check` in `.herd/project.yaml`). When the herd marks the PR ready, it puts these
instructions in the PR body and the status pane and requests your review:

1. Check out the change's branch, then build and install the `fake` flavor: `./gradlew :androidApp:assembleFakeDebug`,
   then `adb install -r androidApp/build/outputs/apk/fake/debug/androidApp-fake-debug.apk`.
2. Run the manifests of the areas the change touches: `maestro/run.sh <area>...` (areas in `CLAUDE.md`).
3. **Approve** the PR if they pass. If a flow fails, run `/herd-resolve` and let it submit a **Request changes** review
   with the failing flow and what happened: it first has you rerun the flow and, for a flow the change didn't touch,
   try it on the commit the change is based on, so a regression becomes new tasks and a flow that's broken on `main`
   too stops for you instead. The details are in [Using the herd][herd-guide].

The change isn't archived until you approve. If the change's content changes after your approval but before the
archive, the herd asks you again; the archive commit itself doesn't, and a content change after the archive stops at
`needs-human` instead.

## Merging

You merge the PR once its checks pass. With the herd, that's not when it marks the PR ready for review, which comes
before your Maestro check and the archive: wait until the herd's status pane shows the change as *ready-to-merge*
(archived, checks passing, reviews settled; see [Using the herd][herd-guide]). Every PR, and every push to `main`, runs
three required checks (`.github/workflows/pr-check.yml`): `tests-and-build` (shared tests, debug build), `code-quality`
(`./gradlew codeQuality`) and `spec-validation` (`scripts/validate-specs.sh`, after its own tests). Each fails on a
failure of any of its parts and names the failing step or finding; `spec-validation` needs no build, so it reports
within a minute without waiting for the others. CI builds and tests the Android targets only, never iOS. A fourth,
informational one, `release-notes` (`release-notes.yml`), shows what merging would publish and updates when you edit
the description.

What protects `main` lives in GitHub's settings, not in this repository; this is what they are meant to say:

- **Ruleset `main-is-pr-only`:** `main` takes changes only through PRs, from everyone: there is no bypass, the
  administrator included. The merge waits until all three checks pass on the PR's latest commit and every review
  thread is resolved. No approving review is required, and the branch doesn't have to be up to date with `main`.
- **Ruleset `main-retain`:** `main` can't be deleted or force-pushed.
- **Repository merge settings:** squash merges only (merge commits and rebase merges are off), so `main`'s history is
  one commit per PR. The release scripts rely on that: a skip tag is on the commit that brought the change.

To see what is live: `gh api repos/mliikanen/driving-log/rulesets`, then `gh api repos/mliikanen/driving-log/rulesets/<id>`.
Merging publishes to the testers (`docs/distribution.md`).

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

Onboarding follows the herd repo's ["Onboarding a project"][herd-onboarding] steps. Driving Log specifics:
- The proposals that were on `main` were moved to `change/<name>` branches (one commit removed them from `main`; each
  branch holds only its own proposal).
- `.herd/project.yaml` is drafted (its comments explain the values); `herd init` validates it once the herd
  exists.
- CI is set up (`add-ci-workflows`): the workflows (`pr-check.yml`, `release-notes.yml`, `release.yml`), the rulesets
  on `main`, and the one-time setup in `docs/distribution.md` (the `firebase-deployment` environment limited to
  `main`, its secrets, and Workload Identity Federation for the upload).
- `.herd/toolchain.Dockerfile` exists, and the gate passes in it under worker conditions (read-only root filesystem,
  egress only through the allow-list); `herd doctor` re-checks it once the herd exists.
- Publishing to Firebase on merge is this project's release automation, separate from the herd
  (`docs/app-distribution.md`).

[herd-guide]: https://github.com/mliikanen/herd/blob/main/docs/using-the-herd.md
[herd-ready-rules]: https://github.com/mliikanen/herd/blob/main/docs/using-the-herd.md#before-marking-a-proposal-ready
[herd-design]: https://github.com/mliikanen/herd/blob/main/docs/design.md
[herd-onboarding]: https://github.com/mliikanen/herd/blob/main/docs/design.md#onboarding-a-project
