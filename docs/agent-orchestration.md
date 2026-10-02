# Agent orchestration: proposal → implementation → review → release

This is harness tooling, not app behavior — it has no `openspec/` change or spec. It governs *how* OpenSpec
changes get implemented, not what the app does, so it's documented here the same way `docs/distribution.md` and
`docs/test-strategy.md` document other parts of how this project is built and shipped.

## Goal and division of labor

Human time is spent at three points only: writing/refining a proposal (`/opsx:propose`, unchanged, interactive,
cloud SOTA as today), running the Maestro manifests on the finished change (final approval, see below), and
merging the resulting PR. Everything else — implementing `tasks.md` item by item, reviewing each task, iterating
on review feedback, fixing Maestro failures fed back from final approval, archiving the spec delta, and opening
the PR — runs unattended. A proposal that the pipeline can't finish on its own stops in a `needs-human` state
(see Escalation) instead of looping.

- **Proposer**: cloud SOTA (Claude Code, interactive). Unchanged from today, plus one step: marking the proposal
  ready (see The hand-off).
- **Implementer**: an LLM run non-interactively, one task at a time. **Not yet decided whether this is local or
  cloud** — no local LLM is provisioned yet (see Setup plan step 1). Pick a harness that supports both a local
  backend (Ollama or similar) and a cloud API behind the same interface, switchable by config rather than a code
  change — cost and privacy favor local for routine implementation work, but until one is actually running,
  cloud is the fallback, and the harness shouldn't need to change to move between them later.
- **Reviewer**: cloud SOTA (Claude Code, `claude -p`, non-interactive), reviewing each task's commit and, once
  every task is accepted, the whole change holistically. Also runs the archive step (`/opsx:archive`) once final
  approval passes, since syncing spec deltas can need judgment.
- **Orchestrator**: plain code (no LLM, no LLM API key), owns the queue and git/GitHub plumbing — assigns work,
  creates/tears down working copies, derives task/review state, pushes, opens and updates the PR.

**Scope for v1: Android only.** Neither the agents nor CI build or test `iosApp`/iOS targets (there's no Mac in
the loop). A task that needs a Mac escalates to `needs-human`.

## The hand-off: when a proposal enters the queue

Proposals are committed to `main` while they're still being refined, so the existence of
`openspec/changes/<name>/` is **not** a signal to start. The hand-off is an explicit `ready: true` field in that
change's `.openspec.yaml`, committed to `main` by the human when they're done refining. The orchestrator only
claims changes whose `.openspec.yaml` on `main` carries it. Like everything else here, this is read from git, not
remembered.

## Concurrency model

- A proposal's tasks run **strictly sequentially** — `tasks.md`'s sections are a dependency chain in practice
  (storage → UI → cross-cutting logic → Maestro → verification), confirmed by reading several archived changes'
  `tasks.md` files rather than assumed. Building scheduling for intra-proposal parallelism isn't worth it for v1.
- **Multiple proposals run concurrently**, each on its own branch (`change/<name>`), each bound to at most one
  active worker at a time.
- Each unit of work (one task's implementation, one round of addressing review feedback, or one review) gets a
  **fresh, ephemeral clone** of the proposal branch's current tip, taken from a local bare mirror (see
  Containerization), handed to whichever worker is free, and deleted when that unit of work ends. The branch is
  the persistent state; the clone is disposable — this is what lets review feedback be picked up by a different
  worker than the one who wrote the original code.
- **Restarting a task always starts clean, never resumes.** If a task needs to be restarted — orchestrator crash,
  worker crash, a hung worker getting killed on timeout — whatever partial work existed in that attempt's clone
  is discarded outright: not diffed, not merged, not used as a starting point. The next attempt gets a brand-new
  clone of the branch's current (last-pushed) tip, exactly as if the task were starting for the first time. This
  is a deliberate rule, not just a side effect of clones being ephemeral — the temptation to "resume the
  half-finished diff instead of wasting the compute already spent" needs to be ruled out explicitly, since a
  resumed partial edit from a crashed/killed worker is exactly the kind of unverified state this design otherwise
  avoids by treating every attempt as a clean, reproducible function of (branch tip, task, review notes).
- **Commits are additive, never amended, rebased or force-pushed**, so the commit history is an honest review
  trail (implement → review → revise → review → accept) worth surfacing in the final PR body.
- The reviewer pins its verdict to the exact commit SHA it evaluated (recorded in a `review-notes.md` sibling to
  `tasks.md`), so a verdict never becomes ambiguous if the branch moves under it. Because history is never
  rewritten (above, and see Keeping up with `main`), these pins stay valid for the life of the branch.

## Who commits, who pushes

**Workers commit; only the orchestrator pushes.** A worker (implementer or reviewer) makes exactly one commit in
its clone per unit of work — so commit messages are written by the agent that knows what changed — and writes a
status file on exit. The orchestrator then validates the commit before pushing it:
- exactly one new commit on top of the tip the unit started from;
- the commit contains the expected state transition (see below) and nothing outside the change's scope (e.g. an
  implementer commit must not touch `review-notes.md` or flip a checkbox to `[x]`);
- the status file agrees with the commit.

A commit that fails validation is discarded like any crashed attempt. Worker containers never hold GitHub
credentials; the orchestrator's GitHub token is the only push-capable credential in the system.

## Communication and the work queue: git + files, no message bus, no separate durable store

No queue service, no database. The queue is **derived from git on every orchestrator start**, not persisted
independently:
- `.openspec.yaml`'s `ready: true` on `main` is the intake queue (see The hand-off).
- `tasks.md` checkbox state (`[ ]` / `[r]` awaiting review / `[x]` accepted) is the task queue itself. `[r]` is
  OpenSpec-compatible: its parser counts only `x` as done, so `openspec list` and `openspec archive` treat `[r]` as
  still open, which is correct.
- `review-notes.md` (new, per change) carries reviewer feedback back to whichever worker picks up the task next,
  the Maestro result from final approval, and any `needs-human` marker. The implement→reject→revise sequence
  visible in the branch's commit history is how many review rounds a task has had — not a separate counter.
- Whether a proposal's PR exists, and whether it's still a draft, is answered by asking GitHub
  (`gh pr list --head change/<name> --json number,isDraft`), not remembered.

The only state that lives purely in the orchestrator process's memory — and is allowed to vanish on crash — is the
*current assignment lock* (which worker container currently holds which proposal), whose only job is to stop two
workers racing on the same branch while the process is alive.

**Crash recovery** (orchestrator container restart or a full host reboot look identical from here, given
Compose's `restart: unless-stopped`): on start, the orchestrator (1) lists every ready change on `main` and every
`change/*` branch without a merged PR, (2) reads each one's `tasks.md`/`review-notes.md` to compute its exact next
action from scratch — no assumption carried over from before the crash, (3) reconciles against `docker ps` (kill
any orphaned worker container rather than adopt it — its state is suspect) and `gh pr list` (don't open a second
PR for a branch that already has one), (4) re-enqueues and resumes. The cost of a crash is bounded to whatever
unpushed work was sitting in a worker's ephemeral clone — redone from the last pushed commit (and, per the rule
above, that redo never reuses the discarded partial work) — which is cheap specifically because workers are
stateless and task granularity is small (one `tasks.md` item at a time).

**The guarantee that a started-but-unfinished task can never be missed on restart** — not just redone, but never
silently dropped — comes from two rules together, not from the scan alone:
- Every state transition is a **single commit, pushed as a unit**. An implementer's "done" flips `tasks.md`'s
  checkbox to `[r]` *in the same commit* as the code change; a reviewer's verdict flips it to `[x]` (or back to
  `[ ]` with `review-notes.md` updated) *in the same commit* as writing its notes. A commit that never got pushed
  doesn't exist as far as recovery is concerned — it was in an ephemeral clone and is discarded. This rules out a
  4th, invisible in-between state — e.g. code pushed but still marked `[ ]`, which would make a restart redispatch
  it as unstarted and duplicate work on top of what's already there, instead of cleanly treating it as awaiting
  review.
- The state space is **exhaustive and structurally derived**, both per-task and per-proposal, so recovery is a
  total function of current facts, never a remembered checkpoint. Per task: `[ ]` / `[r]` / `[x]`, nothing else
  possible. Per proposal, checked in this order (first match wins):
  1. *needs-human* — `review-notes.md` on the branch carries an unresolved `needs-human` marker (see Escalation).
     Next action: none; shown on the dashboard until a human resolves it.
  2. *not ready* — no `ready: true` in the change's `.openspec.yaml` on `main`, and no branch. Not queued.
  3. *unclaimed* — ready, but no `change/<name>` branch pushed yet. "Claiming" a proposal **is** that push, so
     there's no half-claimed window: a crash mid-claim just leaves it unclaimed, picked up fresh next scan.
  4. *implementing* — branch exists, ≥1 task not `[x]`. This includes tasks appended after a Maestro failure,
     even when a draft PR already exists.
  5. *holistic-review-pending* — every task `[x]`, no holistic-accept recorded in `review-notes.md` for the
     current tip, change not archived.
  6. *awaiting-maestro* — holistic review accepted, change not archived. Next action: ensure a **draft** PR
     exists; the human runs Maestro (see Final approval).
  7. *archiving* — `review-notes.md` records a Maestro pass, change not yet archived on the branch. Next action:
     the reviewer runs `/opsx:archive` and commits. A crash mid-archive never gets pushed, so it's discarded with
     the clone and redone, same as any other unit of work.
  8. *ready-to-merge* — archive commit pushed. Next action: mark the PR ready for review; a human merges.

  Because every branch is always in exactly one of these states and each has a defined next action, a full scan
  over all open branches cannot skip anything — there's nothing outside the enum for a task or proposal to
  silently fall into.

This forces every multi-step git/GitHub operation (push, open-PR, mark-ready, update-branch) to be written as
check-before-act/idempotent rather than blindly re-run — needed anyway for ordinary network failures, so crash
recovery exercises the same code path rather than a separate one. It also means recovery must always be a **full**
re-scan of every open branch, never a delta/"resume from last known position" scheme — a remembered checkpoint is
itself exactly the kind of state that could be lost in the same crash, which would reintroduce the missed-task risk
this design is meant to rule out. Deliberately not reaching for Redis/SQLite/a job queue here: a second source of
truth that can drift from git's would undercut the property that git is the only infrastructure dependency this
whole design has. Revisit only if rescanning branches on startup becomes slow with many concurrent proposals — not
a problem at today's scale.

## Adding tasks mid-apply

Past changes added tasks during apply (e.g. `adjust-picture-crop`'s "2b. Edge to edge (added during apply)"), so
the pipeline allows it, narrowly:
- **Only the reviewer appends tasks**, as part of a verdict commit, under a section marked "(added during apply)".
  The implementer can only *request* one, via its status file; the reviewer decides on its next pass.
- Tasks fixing Maestro failures from final approval are appended the same way, under "(added during final
  approval)".
- Added tasks count toward a per-proposal cap (start with 3); appending past it escalates to `needs-human`
  instead, since that much scope drift means the proposal itself needs revisiting.

## Escalation: the `needs-human` state

A proposal stops and waits for a human when any of these happens:
- a single task reaches the review-round cap (start with 3 rejections);
- the added-tasks cap is exceeded (above);
- merging `main` into the branch conflicts (see Keeping up with `main`);
- the regression gate keeps failing after the cap of fix attempts;
- the holistic review rejects with feedback that can't be mapped to a specific task;
- a task needs something the pipeline doesn't have (a Mac, a real device, a credential), or content from outside
  the project (see the request/provide cycle under Containerization).

The marker is a committed line in `review-notes.md` (`needs-human: <reason>`, pinned to a SHA like any verdict),
so the state is derived from git like everything else. The human resolves it by fixing whatever's wrong (editing
`tasks.md`, resolving the conflict, revising the proposal) and committing the marker as resolved on the branch;
the next scan picks the proposal back up from whatever state its files now describe.

## Final approval: Maestro

Gradle tests and builds run inside the workers (see Containerization); **Maestro does not** — Android emulators in
Docker (KVM passthrough) are fragile, and the Maestro suite is the happy path end to end, which suits a human
final check. Maestro tasks in `tasks.md` (writing/updating flows) are still implemented and reviewed like any
other task; only *running* them is deferred.

When a proposal reaches *awaiting-maestro*, the orchestrator opens a **draft** PR. The human checks the branch out,
runs the manifests the change touches (per `CLAUDE.md`: `maestro/run.sh <area>...` with the `fake` flavor), and
records the result in `review-notes.md`, pinned to the SHA tested:
- **pass** → the proposal moves to *archiving*;
- **fail** → the human writes the failure (which flow, what happened) as the note; the reviewer turns it into
  appended task(s) under "(added during final approval)" and the proposal goes back to *implementing*. The draft PR
  stays open throughout and simply gets more commits.

Archiving happens only after the Maestro pass, deliberately: `openspec archive` moves the change directory, so
feeding failures back as new tasks after an archive would mean un-archiving.

## Keeping up with `main`

No rebasing (that would rewrite SHAs and break the reviewer's pins and the additive-history rule). Instead,
branch protection requires PR branches to be **up to date with `main`** before merging, and the orchestrator
uses GitHub's update-branch (`gh api -X PUT repos/<owner>/<repo>/pulls/<n>/update-branch`, a merge commit) when
the PR is behind. That merge triggers the CI checks again, which is where two concurrent proposals that both
touched a shared file (version catalog, `CLAUDE.md`, a shared spec) actually collide — per-task testing alone
won't catch that. A conflicting update-branch escalates to `needs-human`; a CI failure after the update is
treated like a failed regression gate (fix task, then escalate past the cap). A Maestro pass recorded before an
update-branch merge is not invalidated by it — CI re-runs the Gradle gate, and Maestro is re-run by the human at
their discretion.

## Containerization

One Docker image per **role**, not per worker instance — the role's system prompt and tooling are baked into the
image, workers are disposable containers started from it:

- `agents/implementer/Dockerfile` — the implementer harness, plus JDK and Android SDK so it can run
  `./gradlew :shared:allTests :androidApp:assembleDebug` on its own work before committing. Concrete default:
  [Aider](https://aider.chat), which already supports both an Ollama-served local model and cloud APIs
  (Anthropic/OpenAI/etc.) behind the same `--model` config — pick a harness with that property regardless of which
  one it ends up being, since the local/cloud decision is explicitly open (see above). The backend (model name +
  endpoint) is passed as config/env at container start, not baked into the image, so switching targets is a config
  change, not a rebuild. The role's system prompt still ships as a file in the image (`SYSTEM_PROMPT.md`), versioned
  with it. Gets its clone as a volume at invocation; reads its task from an env var/arg the orchestrator passes,
  commits, writes a status file on exit. No GitHub credentials.
- `agents/reviewer/Dockerfile` — wraps `claude -p` with the review system prompt (structured accept/revise
  output, modeled on this project's own `/code-review` conventions), plus JDK and Android SDK to re-run the Gradle
  gate itself rather than trusting the implementer's claim. Also runs `/opsx:archive` (so it needs the `openspec`
  CLI). Needs `ANTHROPIC_API_KEY`; no GitHub credentials.
- `agents/orchestrator/Dockerfile` — the plain-code piece: bare-mirror and clone lifecycle, queue, commit
  validation, push, `gh pr create`/update-branch/mark-ready. Needs a GitHub token; no LLM key.
- `agents/dashboard/` — see Monitoring below.

**Repo access: a local bare mirror, one fresh clone per unit of work.** The orchestrator keeps a bare mirror of
the GitHub repo in a Docker volume (fetched before each assignment). For each unit of work it clones the
proposal branch from the mirror into a per-unit volume, mounts that into the worker, and after the worker exits
fetches the worker's commit back from the clone, validates it, pushes it to GitHub, and deletes the clone. This
is used instead of `git worktree` on a bind-mounted host checkout because worktrees record absolute `gitdir`
paths (which break across container mount points) and share one `.git` whose locks concurrent workers would
contend on. It also means nothing changes if workers ever run on more than one machine — they'd clone from the
mirror over the network instead.

**Least privilege for agent containers** (implementer and reviewer):
- Each image contains **exactly** the CLI utilities its role needs, and nothing else: the agent harness itself,
  `git` (local operations only), the JDK, Android SDK and Gradle wrapper's prerequisites, and for the reviewer the
  `openspec` CLI. No `gh`, no `docker`, no `ssh`/`curl`-style network tools, no package managers at runtime, no
  general-purpose extras "just in case". Each role's Dockerfile lists its tools explicitly, and adding one is a
  reviewed change to that Dockerfile, not something an agent can do itself.
- **No filesystem access outside the project.** The only mount is the unit's own clone of the repo. No host bind
  mounts (not the host checkout, not `$HOME`, not `~/.android-keystores/`, not the Docker socket), no access to
  other units' clones or the bare mirror. The container's root filesystem is read-only apart from the project
  mount and a scratch `tmpfs`. Secrets reach a container only as the env vars its role needs (above).
- The one practical tension is the Gradle dependency cache: pointing `GRADLE_USER_HOME` inside each fresh clone
  keeps the rule literally but re-downloads every dependency per unit of work. If that's too slow, the exception
  would be a dedicated, per-role Docker volume used only as that cache — decide when building step 2.
- Network egress is limited to what the role needs (the model API endpoint, and Maven/Google repositories for
  Gradle), not open internet.

**When an agent needs content from outside the project** (a reference photo, a sample file, a vendor doc, a
model file), it never fetches it itself. The human provides it, in one of two ways, through a request/provide
cycle that is recorded in git like every other state:

1. **Request.** The implementer names what it needs in its status file (what, why, which task); the reviewer turns
   that into a `needs-human: input <name> — <why>` marker in `review-notes.md`. The proposal stops there (see
   Escalation); the dashboard lists the request.
2. **Provide, preferred: commit it.** If the content is fine to live in the repo (e.g. test photos, as
   `maestro/assets/` already does), the human commits it to the change branch at a path inside the project, with
   a line in the change's `inputs.md` saying where it came from. From then on it's ordinary project content.
3. **Provide, when it can't be committed** (too large, licensed, or not to be published): the human runs
   `scripts/herder.sh provide <change> <file>...`, which copies the files into a per-change **inputs volume**
   (never a host bind mount) and records each file's name, SHA-256 and origin in `inputs.md`, committed to the
   branch. The orchestrator mounts that volume **read-only** at `.agent-inputs/` inside the unit's clone
   (gitignored), and only for units of that change. A file whose hash doesn't match `inputs.md` is not mounted.
4. **Resume.** The human marks the `needs-human` request resolved; the next scan picks the proposal back up.

This path is for content, never credentials. A task that needs a secret escalates and stays with the human; it
isn't solved by handing the secret to an agent.

`docker-compose.yml` wires these together; `./scripts/herder.sh` (matching the `scripts/distribute.sh` precedent)
is the single command — it has attach-or-create semantics (see The herder, below), not a plain `docker compose up`.
The implementer role scales horizontally (`docker compose up --scale implementer=N`) — that's the knob for how
many proposals run concurrently.

## The herder

"Herder" is the name for the *running ecosystem instance* — orchestrator, active worker containers, and the
dashboard together — not just the dashboard on its own. The single launch command has **attach-or-create**
semantics, the same shape as `tmux new-session -A` or `docker compose up -d` followed by an attach: if a herder is
already running, the command connects to it (opens/prints the dashboard, doesn't touch the containers); if not, it
bootstraps the whole ecosystem per the Setup plan below and then attaches to the instance it just created. One
command either way — the user never needs to know in advance whether it's already up.

Concretely:
- "Is a herder running?" is answered by checking for a known Docker Compose project (e.g.
  `docker compose -p agent-herder ps` returning anything) — no separate lock file needed, Compose's project name
  is already the singleton key.
- Not running → `docker compose -p agent-herder up -d` (orchestrator, implementer pool, reviewer, dashboard), wait
  for the dashboard's health check, then attach.
- Running → skip straight to attach: open the dashboard URL (or, for a TUI, connect to its socket).
- "Attach" itself is just the dashboard experience described below — there's no separate state between "running"
  and "attached," so reconnecting after closing the terminal/browser is free.

**What the dashboard shows**: for every proposal currently in flight, its state (from the enum above), which task
it's on, which role (implementer/reviewer) is holding it, how many review rounds it's taken, and a tail of that
worker's log. Proposals in `needs-human` or `awaiting-maestro` are listed first, with the reason — those are the
ones waiting on the human. The cheapest version worth building first: the orchestrator writes one structured JSON
event per state transition (task assigned, commit pushed, review verdict, PR opened, escalation) to an append-only
log; a small single-page web app (or a TUI) tails that log plus `docker compose logs -f` for the currently-active
containers. **The event log is display-only, never read back by the orchestrator** — git stays the only source of
truth; losing the log loses history on the dashboard, nothing else. Don't build more than this until the simple
version proves insufficient — it's the piece of this whole plan with the least precedent to copy from elsewhere
in the project, and the attach-or-create launch behavior matters more up front than the dashboard's polish.

## GitHub Actions (independent of the agent review loop)

Every PR, regardless of how it was produced, also gets `:shared:allTests`, `:androidApp:assembleDebug`, and
`openspec validate --all --strict` (the same final regression gate `CLAUDE.md` defines), plus whatever
GitHub-native AI review is available — a second, independent net, not a replacement for the reviewer agent's pass.
No linter is configured in the project today (no ktlint/detekt/spotless); adding one is a separate change, not
part of this plan. Android only, per the v1 scope.

## Firebase publish on merge to main

A `.github/workflows/` job triggered on push to `main`: builds the signed release APK and runs
`appDistributionUploadProductionRelease`, same Gradle tasks `scripts/distribute.sh` already uses. This changes some
decisions `docs/app-distribution.md` made deliberately for the no-CI world — flagged there as the thing to revisit
once CI automation exists ("deliberately out of scope here, this project has no CI at all yet"):
- Needs a Firebase **service-account key** as a repo secret (today: the developer's own `firebase login` session).
- Needs the **release keystore** as a repo secret too (today: lives only at `~/.android-keystores/`, deliberately
  never in git).
- `versionCode` is `git rev-list --count HEAD`, so the job must check out **full history** (`fetch-depth: 0`); the
  default shallow checkout would make every build's `versionCode` 1.
- `scripts/distribute.sh` tags each release `dist-v<versionName>` locally and finds the previous tag to build
  release notes. The CI job must push that tag (and fetch tags), or the next run's "since last distribution" range
  is wrong.
- Release notes: today a human reviews/edits a draft in `$EDITOR` before upload; in CI there's no one to do that.
  Source the notes from the PR body instead — the reviewer agent should write the final release-note text into the
  PR description as part of its holistic pass, and the CI job lifts that text rather than re-deriving it or
  publishing an unreviewed auto-draft.

## Branch protection

`main`: no direct pushes, PR-only. Require the Actions checks above as required status checks (not just present —
actually required, or protection doesn't gate anything), and require branches to be up to date before merging
(see Keeping up with `main`). Decide explicitly whether a human approval is also required on every PR, or whether
the reviewer agent's accept plus the human's Maestro pass is sufficient — that's the literal GitHub setting
matching "humans interact at review/merge."

The one exception to PR-only is the hand-off itself: marking a proposal `ready: true` and committing proposals is
done on `main` today. Either keep that as a direct push allowed for the developer (admin bypass), or route
proposals through PRs too — decide when enabling protection.

## Setup plan

Steps marked **(manual)** need a human; everything else is written once and then run by `scripts/herder.sh`.

1. **(manual, decide with the user before building anything else)** No local LLM is provisioned yet. Confirm
   explicitly whether the implementer role should run against a local model or a cloud API (at least initially) —
   don't assume local just because it was the original framing. If local: install Ollama on the host and pull a
   coder model (e.g. a Qwen2.5-Coder or DeepSeek-Coder variant), or run Ollama as its own container; if using a
   GPU, install the NVIDIA Container Toolkit on the host first — Docker can't pass a GPU through without it. If
   cloud (even temporarily, until local is set up): just need the relevant API key as a secret, same shape as the
   reviewer's. Either way, the harness choice above should make this swappable later without rework.
2. Scaffold `agents/{orchestrator,implementer,reviewer}/Dockerfile` (implementer and reviewer with JDK + Android
   SDK; reviewer with the `openspec` CLI; each with exactly its role's tools and the mount/network limits from
   Least privilege) and each role's `SYSTEM_PROMPT.md`.
3. Write the orchestrator: bare mirror and per-unit clone lifecycle, intake from `ready: true`, queue/assignment
   logic, the per-proposal state derivation above, worker-commit validation and push, escalation markers,
   draft-PR creation, update-branch, mark-ready, and `gh pr create` with the PR body template (summary, test
   coverage, review-round count, flagged human-review-worth items, release notes for the Firebase job to consume).
4. Write the dashboard as described above (event log + a thin viewer).
5. Write `docker-compose.yml` and `scripts/herder.sh` (the single command, with attach-or-create semantics —
   checks for the `agent-herder` Compose project before deciding whether to bootstrap or just attach — plus the
   `provide <change> <file>...` subcommand for outside content).
6. **(manual)** Provide secrets: `ANTHROPIC_API_KEY` for the reviewer (and the implementer if it runs on a cloud
   API); a GitHub token with repo + PR scopes (`gh auth login` or a PAT) for the orchestrator only.
7. **(manual, GitHub repo settings, admin-only)**:
   - Enable branch protection on `main`: require PRs, require the CI status checks, require up-to-date branches,
     decide the human-approval policy and the hand-off exception (see above).
   - Add repo secrets: Firebase service-account JSON (base64), the release keystore (base64) and its password —
     get explicit sign-off on this step specifically, since it's a new standing credential each, not a convenience
     default (see `docs/app-distribution.md`'s existing reasoning on why these were kept out of any automated
     store until now).
8. Write `.github/workflows/` for the PR-check job and the merge-to-main Firebase publish job (full-history
   checkout, tag push).
9. **(manual)** Smoke test: run one small, low-risk proposal through the whole pipeline by hand, watching the
   dashboard — including one deliberate review rejection and one Maestro failure fed back — before trusting it to
   run unattended on a real proposal queue.
10. Update `CLAUDE.md` with the new workflow (where it fits alongside the existing OpenSpec/Maestro sections, and
    this file in its `docs/` list) once the above is proven out.

## Open questions deferred, not forgotten

- **Local vs. cloud for the implementer role** — not decided; confirm with the user when setup actually begins
  (Setup plan step 1), since no local LLM is provisioned today.
- Implementer harness choice (Aider vs. something custom) — proposed as a default above; the hard requirement is
  that it support both local and cloud backends behind one switchable config, not which specific tool it is.
- Whether to require human PR approval in addition to the reviewer agent's accept and the Maestro pass (branch
  protection setting, listed above but not decided), and how the `ready: true` hand-off coexists with PR-only
  `main`.
- The cap values (review rounds per task, added tasks per proposal, regression-gate fix attempts) — 3 is a
  starting guess; tune from the smoke test.
- Whether an emulator container for automated Maestro runs is worth building later, replacing the human run at
  final approval.
- iOS — out of scope for v1.
- Gradle dependency cache: per-clone (strict, slow) vs. a dedicated per-role cache volume (see Least privilege).
