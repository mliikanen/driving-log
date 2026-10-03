# Proposal

## Why

The project has no CI. The regression gate (`./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
`openspec validate --all --strict`, including the linters from `add-lint-quality-gates`) runs only when someone
remembers to run it locally, and nothing stops a pull request that fails it from being merged. Releasing to testers is a
manual `scripts/distribute.sh` run from the developer's machine, with the developer's own Firebase login and
keystore. The planned agent pipeline (`docs/change-workflow.md`) needs both automated: PRs must be checked by
something independent of the agents that wrote them, and a merged change must reach testers without a human
cutting the release. `docs/app-distribution.md` already plans the release half ("Planned: publish from CI on
merge to `main`").

## What Changes

- A **PR check** GitHub Actions workflow runs on every pull request to `main`, and on every push to `main` as
  well, so direct pushes are checked too. It has two jobs, each its own status check:
  - `tests-and-build`: `:shared:allTests`, `:androidApp:assembleDebug`, `openspec validate --all --strict`, and the
    release-notes draft the merge would publish, in the job summary, so it can be reviewed before merging;
  - `code-quality`: `codeQuality` (ktlint, detekt, Android lint; there are no baselines since
    `clean-up-lint-baselines`).
- **Both checks block merging.** A repository ruleset on `main` requires both to pass, on the PR's latest commit,
  before a pull request can be merged. The repository admin keeps a bypass, so committing directly to `main` (how
  proposals and today's changes land) still works; those commits are checked after the fact.
- A **release** workflow runs on every push to `main` (and on manual dispatch). It builds the signed
  `production` release APK, uploads it to Firebase App Distribution's tester group, and pushes the
  `dist-v<versionName>` tag. When nothing was archived since the last distribution and the merged PR has no
  release notes, there is nothing to say: it publishes nothing and creates no tag.
- **Release notes in CI**: still generated from the OpenSpec archive since the last `dist-v*` tag. When the merged
  PR's body has a `## Release notes` section, that edited text is used instead. That's the planned herd reviewer
  output, and also how a human edits the notes.
- **Release secrets** go into the existing `firebase-deployment` GitHub environment, limited to `main`: the real
  `google-services.json`, the release keystore and its password. The upload uses a keyless service-account login.
  The PR check needs no secrets: it builds with a placeholder `google-services.json`. Each new standing credential
  needs the developer's explicit sign-off.
- `scripts/distribute.sh` stays as the local fallback. It fetches tags first and pushes the tag it creates, so a
  local and a CI release agree on what was last distributed.
- `docs/app-distribution.md`, `docs/distribution.md` and `docs/change-workflow.md` are updated to match.

**Out of scope:**
- Making `main` PR-only for everyone, and requiring a human approval on PRs. Both are open decisions in
  `docs/change-workflow.md`. This change only makes the checks required.
- iOS builds and tests: there is no macOS runner in the plan.
- Running Maestro in CI. It stays the human final check.
- Configuring the linters themselves. That's `add-lint-quality-gates`, which **must be applied first**: this
  change's `code-quality` job runs its `codeQuality` task.
- Any new test kind.
- Store releases (Play Store). Firebase App Distribution remains the only distribution channel.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `app-distribution`: distribution also runs from CI on every push to `main`. It's authenticated with a service
  account, its notes come from the archive or the merged PR's `## Release notes` section, and its tag is pushed
  to the remote. A local run fetches and pushes tags too.
- `test-strategy`: the final regression run of a change, static analysis included, also runs in CI on every pull
  request and push to `main`, and a pull request can't be merged until it passes.

## Impact

- Depends on `add-lint-quality-gates` being applied.
- New: `.github/workflows/pr-check.yml`, `.github/workflows/release.yml`, and a release-notes script shared by CI
  and `scripts/distribute.sh` (`scripts/release-notes.sh`).
- Changed: `scripts/distribute.sh` (fetch/push tags, shared notes script).
- Docs: `docs/app-distribution.md`, `docs/distribution.md`, `docs/change-workflow.md`.
- GitHub repo settings: a ruleset on `main` requiring both checks; the `firebase-deployment` environment with its
  secrets; Actions with write permission for tags on the release job only.
- **Prerequisite: GitHub Pro, or making the repository public.** On the current plan GitHub refuses rulesets and
  branch protection for this private repository ("Upgrade to GitHub Pro or make this repository public"), so no
  check can block a merge. Deployment-branch limits on environments have the same restriction. This is the
  developer's decision (task 4.1).
- The repo is private, so Actions minutes count against the account's quota. Each Android build costs several
  minutes.
- No app code or Gradle build changes: CI writes the keystore and `google-services.json` to the paths the build
  already reads.
