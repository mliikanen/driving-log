# Tasks

## 1. Release notes script and local distribution

- [ ] 1.1 Add `scripts/release-notes.sh [<commit>]`: prints the names of changes archived since the last `dist-v*`
      tag reachable from `<commit>` (default `HEAD`), one per line, using `distribute.sh`'s existing `git log`
      pipeline moved verbatim (design.md decision 5). Verify: on the current `main` its output is identical to the
      old inline pipeline's output (run both and `diff`), and `release-notes.sh <older commit>` lists only changes
      archived up to that commit.
- [ ] 1.2 Change `scripts/distribute.sh` to `git fetch --tags origin` before computing the range, to call
      `release-notes.sh` for the draft, and to `git push origin dist-v<versionName>` after tagging (spec: "A
      signed build uploads to the configured tester group"). Verify: `bash -n`, and a dry read of the script
      shows fetch → notes → build/upload → tag → push in that order, with the existing refusal when `HEAD` is
      already tagged now applying to a tag fetched from the remote.

## 2. PR check workflow

- [ ] 2.0 Check that `add-lint-quality-gates` is applied: `./gradlew codeQuality --dry-run` resolves. Verify: it does;
      if not, stop and apply that change first.
- [ ] 2.1 Add `.github/ci/google-services.placeholder.json`: the shape of the real file with package name
      `com.mikonoma.drivinglog` and dummy project/API values (design.md decision 2). Verify: with the real file
      moved aside and the placeholder copied to `androidApp/src/production/google-services.json`,
      `./gradlew :androidApp:assembleDebug :androidApp:lintProductionDebug` succeeds; then restore the real file.
- [ ] 2.2 Add the composite action `.github/actions/setup-build`: checkout with `fetch-depth: 0` and tags,
      Temurin 25, `setup-android`, `setup-gradle` (cache written only on `main`), OpenSpec CLI 1.13.1, placeholder
      copy. Third-party actions pinned to commit SHAs (design.md decisions 7 and 8). Verify: `actionlint` reports no
      errors once a workflow uses it (2.3).
- [ ] 2.3 Add `.github/workflows/pr-check.yml` (`pull_request` and `push` to `main`, `contents: read`) with two jobs
      using `setup-build`: `tests-and-build` (`:shared:allTests`, `:androidApp:assembleDebug`,
      `openspec validate --all --strict` as separate steps) and `code-quality` (`./gradlew codeQuality`) (design.md
      decision 8). Verify: `actionlint` reports no errors.
- [ ] 2.4 Add the release-notes preview step to the `tests-and-build` job: on a pull request, the PR body's
      `## Release notes` section if non-empty, else `release-notes.sh` on the checked-out merge commit, else
      "nothing to release". Written to the job summary (spec: "The draft is shown on the pull request"). Verify:
      the section-extraction logic, run locally on a sample body with and without the section and with a following
      `## ` heading, prints the expected text.
- [ ] 2.5 Open this change's branch as a pull request. Verify: both checks run and pass, and the job summary shows
      the notes preview. Then push two deliberately failing commits, one at a time, and revert each: a broken test
      (`tests-and-build` fails naming `:shared:allTests`) and a misformatted line (`code-quality` fails naming ktlint
      and the file).

## 3. Release workflow

- [ ] 3.1 Add `.github/workflows/release.yml`: `workflow_run` on `PR check` (completed, `main`, event `push`,
      success only) and `workflow_dispatch`, `concurrency: { group: release, cancel-in-progress: false }`,
      `environment: firebase-deployment`, permissions `contents: write`, `pull-requests: read`, `id-token: write`. Checkout of
      the checked `head_sha` with full history and tags (design.md decisions 1 and 6). Verify: `actionlint`
      reports no errors.
- [ ] 3.2 In `release.yml`, the first step fails naming every missing secret or variable (`GOOGLE_SERVICES_JSON_BASE64`,
      `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, and the Workload Identity provider and service account
      variables), before any build (spec: "A release secret is missing"). Then it writes the keystore,
      `keystore.properties` and `google-services.json` to the paths the build reads (design.md decision 3).
      Verify: after merging, the first run fails at this step and lists all of them, while nothing is built or
      tagged.
- [ ] 3.3 In `release.yml`: skip with "already distributed" when `HEAD` has a `dist-v*` tag. Choose the notes as in
      design.md decision 5 (merged PR's `## Release notes` via `gh api repos/{repo}/commits/{sha}/pulls`, else
      `release-notes.sh`), and skip with "nothing to release" when they're empty. Otherwise authenticate
      (`google-github-actions/auth`, Workload Identity Federation), run
      `:androidApp:assembleProductionRelease :androidApp:appDistributionUploadProductionRelease
      -PdistributionReleaseNotesFile=<file>`, then tag and push `dist-v<versionName>`. Verify: `actionlint`, and
      the end-to-end run in 4.5.

## 4. Repository and cloud setup (developer, with explicit sign-off)

- [ ] 4.1 **(developer, decision)** Choose how to get rulesets and environment branch limits for this repository:
      upgrade the account to GitHub Pro, or make the repository public (GitHub refuses both on the current plan;
      design.md decision 8). Until one is done, tasks 4.7 and 4.8 can't be completed, and the change isn't done.
      Verify: `gh api repos/mliikanen/driving-log/rulesets` returns a list instead of the "Upgrade to GitHub Pro"
      error.
- [ ] 4.2 **(developer)** Limit the existing `firebase-deployment` environment's deployment branches to `main`.
      Verify: `gh api repos/mliikanen/driving-log/environments/firebase-deployment` shows a deployment branch policy
      with `main` only.
- [ ] 4.3 **(developer, sign-off)** Create the Google Cloud service account with only
      `roles/firebaseappdistro.admin`, and a workload identity pool and GitHub OIDC provider restricted to
      `repo:mliikanen/driving-log` and `environment:firebase-deployment`, allowed to impersonate that account. Add the
      provider name and account email as `firebase-deployment` environment variables. Verify: the `gcloud` commands used are
      recorded in `docs/distribution.md` (task 5.2), and `gcloud iam service-accounts get-iam-policy` shows only
      the workload identity binding.
- [ ] 4.4 **(developer, sign-off)** Add `GOOGLE_SERVICES_JSON_BASE64`, `RELEASE_KEYSTORE_BASE64` and
      `RELEASE_KEYSTORE_PASSWORD` as `firebase-deployment` environment secrets (`base64 -w0` of each file). Verify:
      `gh secret list --env firebase-deployment` lists all three.
- [ ] 4.5 Run `release.yml` by hand (`workflow_dispatch`) on `main`. Verify: the build reaches the tester group in
      Firebase App Distribution with the expected notes, and `git ls-remote --tags origin 'dist-v*'` shows the new
      tag. If the upload rejects the federated credential, switch to the key-file fallback
      (`FIREBASE_SERVICE_ACCOUNT_JSON_BASE64`, design.md decision 4), record that in design.md, and rerun.
- [ ] 4.6 Confirm a pull request can't reach the release secrets. Verify: in a throwaway PR, a workflow step that
      references `secrets.RELEASE_KEYSTORE_PASSWORD` gets an empty value, and a job declaring
      `environment: firebase-deployment` from the PR branch is refused. Close the PR without merging.
- [ ] 4.7 Add `.github/rulesets/main.json` ("CI must pass": target `main`, required status checks
      `tests-and-build` and `code-quality` from GitHub Actions, branches up to date before merging, the repository
      admin role on the bypass list, nothing else) and create it with
      `gh api -X POST repos/mliikanen/driving-log/rulesets --input .github/rulesets/main.json`, after both checks
      have passed on `main` once (design.md, Migration Plan). Verify: `gh api repos/mliikanen/driving-log/rulesets`
      lists it as active.
- [ ] 4.8 Confirm merging is blocked (spec: "A pull request cannot be merged until its checks pass"). Verify: on a
      throwaway PR with a misformatted line, `gh pr merge` is refused while `code-quality` is pending and after it
      fails; after fixing the line and both checks passing, the merge is allowed (close it instead of merging).
      Then confirm a direct push to `main` by the admin is still accepted.

## 5. Documentation

- [ ] 5.1 Update `docs/app-distribution.md`: replace "Planned: publish from CI on merge to `main`" with how it
      works now (trigger, environment, Workload Identity Federation or key, notes from the PR or the archive,
      tag push), and update the "No service-account credential file" paragraph to say it still holds for the
      local flow. Verify: no "planned"/"not built yet" wording remains for CI in the file.
- [ ] 5.2 Update `docs/distribution.md`: CI is now the normal release path, `distribute.sh` is the manual
      fallback (it fetches and pushes tags), the one-time CI setup (tasks 4.1–4.4 with the exact commands), and
      how to edit release notes through the PR body. Verify: the doc's "Cutting a release" section describes both
      paths.
- [ ] 5.3 Update `docs/change-workflow.md` ("Merging" and "Onboarding status": the two checks now exist and block
      merging; PR-only and human approval are still open) and `CLAUDE.md`'s Commands section with one line saying CI
      runs the final regression run on every PR and push to `main`, and a PR can't merge until both checks pass.
      Verify: both files mention `pr-check.yml` and the ruleset.
- [ ] 5.4 Update `openspec/specs/app-distribution/spec.md`'s Purpose (it says "from a developer's machine") to cover
      CI distribution too. Verify: the Purpose mentions both.
- [ ] 5.5 Update `docs/code-quality.md` (from `add-lint-quality-gates`) to say the `code-quality` check runs
      `codeQuality` on every PR and blocks merging. Verify: the doc names the check.

## 6. Final regression run

- [ ] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all --strict`
      (no Maestro: this change touches no app screen or flow). Verify: both pass, and both PR checks on the change's
      PR are green.
