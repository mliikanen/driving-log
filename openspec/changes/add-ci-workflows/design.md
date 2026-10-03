# Design

## Context

- The Gradle build reads two things from outside version control. `androidApp/src/production/google-services.json`
  is a per-developer download, gitignored, and needed by every `production` variant, including the
  `productionDebug` that `:androidApp:assembleDebug` builds. `~/.android-keystores/keystore.properties` and the
  keystore it names are needed by every release variant; `checkReleaseSigning` fails a release build without them.
- `versionCode`/`versionName` are computed from `git rev-list --count HEAD` at configuration time, for every build.
- `scripts/distribute.sh` finds the last `dist-v*` tag locally, lists changes archived since then
  (`git log --no-renames --diff-filter=A` under `openspec/changes/archive/`), opens the draft in `$EDITOR`,
  builds and uploads with `-PdistributionReleaseNotesFile=<path>`, and tags locally without pushing.
- Toolchain: Gradle 9.7.1 wrapper, AGP 9.4.1, Kotlin 2.4.20, compileSdk 37, the developer's JDK 25, and
  OpenSpec CLI 1.13.1. iOS targets are skipped on Linux (`kotlin.native.ignoreDisabledTargets=true`).
- The repository is private on GitHub (`mliikanen/driving-log`).

## Goals / Non-Goals

**Goals:**
- No Gradle build change. CI puts files where the build already looks.
- The PR check needs no secrets at all.
- Release credentials are reachable only from runs on `main`.

**Non-Goals:**
- Caching beyond what the Gradle action gives by default.
- Making the release reproducible bit-for-bit.
- Notifying anyone beyond Firebase's own tester emails.

## Decisions

### 1. Two workflows; the release is triggered by a successful check, not by the push

`pr-check.yml` runs on `pull_request` to `main` and on `push` to `main`. `release.yml` runs on `workflow_run`
(`PR check` completed, branch `main`, event `push`, conclusion `success`), plus `workflow_dispatch`. It checks out
the exact `head_sha` the check ran on.

*Alternative:* `release.yml` on `push` running the gate itself. Rejected: it doubles the build minutes of every
merge on a private repo, and two copies of the gate can drift. *Alternative:* `pull_request: closed` with
`merged == true`. Rejected: the release must build a commit that passed the checks itself, and only the run on the
merge commit (a push to `main`) shows that.

Concurrency: `release.yml` uses `concurrency: { group: release, cancel-in-progress: false }`. GitHub keeps at
most one *pending* run per group. With three quick merges the middle one is replaced by the newest, which is
harmless: notes are computed from the last `dist-v*` tag to the run's commit, so the newest run covers what the
replaced one would have distributed. The spec's two-merge scenario holds exactly.

### 2. The PR check needs no secrets: a placeholder `google-services.json`

The PR check copies `.github/ci/google-services.placeholder.json` to `androidApp/src/production/` before building.
It has the right shape and package name (`com.mikonoma.drivinglog`) with dummy project/API values. That's enough
for the Google Services plugin to generate resources. The debug APK is never run in CI, so the dummy values are
never used.

*Alternative:* the real file as a secret. Rejected: the PR job would then hold a secret. It would also fail for
pull requests that get no secrets (Dependabot, forks), and its value is not needed to prove the code compiles.
The release job uses the real file, from the release secrets.

### 3. Release secrets live in the `firebase-deployment` environment, limited to `main`

The release job declares `environment: firebase-deployment`. That environment already exists in the repository
(created by the developer, with no protection rules yet). Its deployment branches are limited to `main`, and it
holds the secrets:

| Secret | Content | Written to |
|---|---|---|
| `GOOGLE_SERVICES_JSON_BASE64` | the real `google-services.json` | `androidApp/src/production/google-services.json` |
| `RELEASE_KEYSTORE_BASE64` | the release `.jks` | `~/.android-keystores/driving-log-release.jks` |
| `KEYSTORE_PASSWORD` | store and key password (PKCS12: one password) | `~/.android-keystores/keystore.properties` (with `keyAlias=driving-log`) |

A first step checks every one is set and fails naming the missing ones, before any build (spec: "A release
secret is missing").

Environment secrets matter here because repository secrets are readable by any workflow run that isn't from a
fork, including a `pull_request` run whose branch edits the workflow file to print them. An environment limited to
`main` can't be entered from a PR branch. Limiting deployment branches needs GitHub Pro on a private repository,
the same prerequisite as decision 8, so the developer's plan decision (task 4.1) covers both. Either way, the herd
manifest's commit validation also rejects worker commits that touch `.github/`, so no agent can write a workflow
that reads secrets.

### 4. Upload identity: a service account via Workload Identity Federation, key file as fallback

The release job authenticates with `google-github-actions/auth` using Workload Identity Federation. A Google Cloud
workload identity pool trusts GitHub's OIDC tokens, but only for `repo:mliikanen/driving-log` with
`environment:firebase-deployment`, and lets them impersonate a dedicated service account. That account's only role is
`roles/firebaseappdistro.admin`. The action writes a short-lived credential config and sets
`GOOGLE_APPLICATION_CREDENTIALS`, which the App Distribution Gradle plugin reads. No long-lived key exists
anywhere. The pool provider and account email are environment *variables*, not secrets.

*Fallback:* if the plugin can't use an external-account credential (task 4.5 verifies), a JSON key for the
same service account, as environment secret `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64`, written to `$RUNNER_TEMP`, with
`GOOGLE_APPLICATION_CREDENTIALS` pointing to it.

The keystore can't avoid being a standing secret. That's the real cost of this change, and it gets the explicit
sign-off `docs/app-distribution.md` asks for.

### 5. One release-notes script, used locally and in CI

`scripts/release-notes.sh [<commit>]` prints the names of changes archived since the last `dist-v*` tag reachable
from `<commit>` (default `HEAD`), one per line, using the existing `git log` pipeline moved verbatim (including
`--no-renames` and the `archive/[^/]+/` filter). `distribute.sh` calls it; so do both workflows.

PR body notes: the release job finds the pull request for its commit
(`gh api repos/{repo}/commits/{sha}/pulls`) and takes the text of its `## Release notes` section, up to the next
`## ` heading. If that text is non-empty it's the notes. Otherwise it uses the script's output. If both are empty,
it publishes nothing (summary: "nothing to release").

The PR check writes the same choice to its job summary. On a pull request it uses the PR's current body and the
merge commit GitHub checks out, so the summary shows what merging would publish. The PR body is read through the
API on each run, so edits to the body show up the next time the check runs.

### 6. Tags: fetched first, pushed after upload, rerun-safe

Both the release job and `distribute.sh` run `git fetch --tags` before computing anything. CI checks out with
`fetch-depth: 0` (needed for `versionCode` anyway) and `fetch-tags: true`. After a successful upload:
`git tag dist-v<versionName>` and `git push origin <tag>`. The release job has `permissions: contents: write`
for that, plus `pull-requests: read` and `id-token: write` for decision 4. Both PR check jobs have
`contents: read` only.

If `HEAD` already carries a `dist-v*` tag (a manual rerun of a finished release), the job publishes nothing.
That's the same guard `distribute.sh` has. If the upload succeeds but the tag push fails, a rerun uploads the
same `versionCode` again. Firebase accepts that as a new release of the same version; it's a visible but
harmless duplicate.

### 7. Toolchain in CI

- `actions/setup-java`, Temurin 25 (the developer's JDK; nothing in the build pins a toolchain).
- `android-actions/setup-android` to accept licenses; AGP downloads the compileSdk 37 platform and build-tools on
  demand.
- `gradle/actions/setup-gradle` for wrapper validation and caching. The cache is written only from `main` runs,
  so PR runs read it and can't poison it.
- `npm install -g @fission-ai/openspec@1.13.2`, pinned to the version the project uses (1.13.2 is what's installed
  locally; the CLI reported 1.13.1 when this was written). Bumping it is a one-line edit.
- **During apply:** the toolchain steps are a composite action, `.github/actions/setup-build`, used by every job. The
  checkout can't be inside it: a local action only loads from an already checked-out repository, so each job checks
  out first (full history and tags). The release-notes choice is one script, `scripts/ci/choose-release-notes.sh`,
  shared by the PR check's preview and the release job.
- Third-party actions are pinned to full commit SHAs, with the version in a comment.
- The gate is one step per part (`:shared:allTests`, `:androidApp:assembleDebug`,
  `openspec validate --all --strict`), so a failure names its step (spec scenario "A test fails").

### 8. Two PR check jobs, both required by a ruleset on `main`

`pr-check.yml` has two jobs, running in parallel, each reported as its own status check:
- `tests-and-build`: `:shared:allTests`, `:androidApp:assembleDebug`, `openspec validate --all --strict`, and the
  release-notes preview (decision 5).
- `code-quality`: `./gradlew codeQuality` (ktlint, detekt, both Android lint debug variants, from
  `add-lint-quality-gates`). There are no baselines to check since `clean-up-lint-baselines`.

Both need the placeholder `google-services.json` (lint on `productionDebug` reads it too) and the same toolchain
setup, which goes in a local composite action, `.github/actions/setup-build`, so the two jobs can't drift.
Separate jobs give two clearly named required checks, and a lint failure reports without waiting for the tests.
The cost is running the setup twice, which Gradle caching mostly absorbs.

A repository ruleset on `main` ("CI must pass") requires both checks (`tests-and-build`, `code-quality`, from
GitHub Actions) and requires branches to be up to date before merging, so a PR is checked against the `main` it
merges into. It also requires a pull request for every change to `main`, and its bypass list is empty: since every
change lives on its own `change/<name>` branch until it's merged (`docs/change-workflow.md`), nobody needs to push to
`main` directly, the administrator included. "Require a human approval" is left unset; it's an open decision in
`docs/change-workflow.md`. The ruleset is created with `gh api` from a checked-in JSON file
(`.github/rulesets/main.json`), so it's reviewable and re-creatable rather than clicked together.

**Prerequisite:** GitHub currently refuses rulesets and branch protection on this private repository ("Upgrade to
GitHub Pro or make this repository public to enable this feature", checked while writing this change). Without one
of those, the checks still run and report, but nothing blocks a merge. Task 4.1 is the developer's decision, and
the change isn't complete until the ruleset exists.

*Alternative:* one job with all steps. Rejected: one required check that fails for either reason, and lint waits
behind the tests. *Alternative:* classic branch protection. Rulesets are GitHub's current mechanism, can be
exported and imported as JSON.

## Risks / Trade-offs

- [Actions minutes on a private repo: an Android build plus the shared tests is several minutes, and a merge
  runs the check and then the release.] → Gradle caching; the release reuses the check instead of rerunning it.
  Watch usage in the first weeks. (The `push` run is the one the release waits for, so it stays.)
- [Release keystore in GitHub. A leak lets someone sign builds that upgrade-install over testers' copies.] →
  Environment secret reachable only from `main`, explicit sign-off, and the backup procedure in
  `docs/distribution.md` stays the source of truth.
- [Automatic release on every archive-bearing push changes today's habit of releasing by hand.] → Intended. Merges
  that archive nothing (docs, tooling) publish nothing. `distribute.sh` remains for manual releases.
- [Placeholder `google-services.json` could hide a broken real file.] → The release job builds with the real one
  on every distribution, so a broken real file fails there.
- [A PR's release-notes preview is computed against the last tag as of the check; a release that lands between
  the check and the merge shifts it.] → The release job recomputes at merge time, and the spec defines the notes
  by what the release computes.

## Migration Plan

1. Merge the workflows. The PR check starts immediately. The release job fails at its first step, naming the
   missing secrets, until the environment exists. That failure is visible but publishes nothing.
2. With the developer's sign-off: settle the plan prerequisite (decision 8), limit `firebase-deployment` to `main`,
   create the Google Cloud workload identity pool and service account, and add the secrets.
3. Run `release.yml` by hand (`workflow_dispatch`) on `main` once to prove the path end to end.
4. Create the ruleset last, once both checks have passed on `main` at least once. GitHub only offers check names
   it has seen.

Rollback: disable `release.yml` in the Actions UI (or delete it). Local `distribute.sh` keeps working throughout,
since it reads its own files and login.
