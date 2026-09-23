# Design

## Context

`androidApp/build.gradle.kts` today has no signing config (the `release` build type only sets
`isMinifyEnabled = false`), a hardcoded `versionCode = 1` / `versionName = "0.1.0"` that has never moved since the
initial commit, and no Firebase dependency of any kind — no `google-services.json`, no `google-services` plugin
entry in `gradle/libs.versions.toml`. A Firebase project for this app already exists (confirmed with the developer),
but the Android app is not yet registered in it and no release keystore exists yet either. There is no CI
(`.github/` does not exist) and, per the developer's explicit choice, this change stays local-only: no GitHub
Actions workflow, no service-account secret to manage. `git remote -v` shows a real `origin` on GitHub, but that is
not used by this change.

I confirmed against Firebase's own Gradle plugin docs that the App Distribution Gradle plugin's default
authentication is the developer's own `firebase login` CLI session (a service-account key is only needed as an
alternative, e.g. for CI), and that it accepts an explicit `appId` in `build.gradle.kts` without requiring
`google-services.json` or the `google-services` plugin at all — that file/plugin only matters for other Firebase
products (Auth, Firestore) this project isn't using in code yet. This is the fact the whole design turns on: it lets
this change add App Distribution without pulling in `google-services.json` or any secret file.

See proposal.md for the motivation and scope; see the `app-distribution` spec delta for the exact required
behavior.

## Goals / Non-Goals

**Goals:**
- One local command a developer runs to cut a signed, versioned, release-noted build and upload it to Firebase App
  Distribution testers.
- Never commit a signing credential, and never require a service-account key for this local-only flow.
- Make both `versionCode` and `versionName` impossible to forget, mistype or collide: both are fully derived from
  git, never typed by hand.
- Make release notes come from the project's own OpenSpec archive history as the required generation source, not
  written from scratch each time.

**Non-Goals:**
- CI automation (GitHub Actions) — explicitly deferred; would need its own secret-management design (a
  service-account key as a GitHub secret) and is out of scope here.
- Play Store or any real app store submission — the project context already says this isn't happening yet.
- Any change to app runtime behavior, `google-services.json`-backed Firebase products (Auth, Firestore), or the
  database.

## Decisions

### `appId` set directly in `build.gradle.kts`/`gradle.properties`, no `google-services.json`
Alternative considered: add the `google-services` Gradle plugin and commit `google-services.json`, the usual
Firebase Android setup. Rejected for now: this project uses no other Firebase product in code yet, so the file would
exist solely to hand the App Distribution plugin a value it can take directly as `appId` — confirmed from Firebase's
own docs. `appId` is not a secret (it is visible in Firebase console URLs and inside every APK's manifest once
distributed anyway), so it is tracked in `gradle.properties` as a plain value (`firebaseAppId=...`), filled in once
during the one-time manual setup (task 1). Adding `google-services.json` becomes worth it once a change actually
adds Firebase Auth or Firestore; nothing here blocks that later.

### CLI-login auth, no service-account file
The plugin's default authentication is the developer's own `firebase login` session (verified against Firebase's
docs). Chosen over a service-account key file because this change is explicitly local-only (developer's choice):
CLI-login needs nothing checked in and nothing to protect beyond the developer's own machine login, where a
service-account key would be a standing credential to store and rotate for no benefit yet. CI automation, if added
later, is exactly where a service-account key earns its keep (as a GitHub secret, never on disk in the repo).

### Release signing: agent-generated keystore, both it and `keystore.properties` outside the repo entirely
**Revised from the original design**, which had the developer generate the keystore themselves. The developer asked
for this to be automated too, and specifically asked me to research and contest the security tradeoff before
agreeing — recorded here since it's a real decision, not a default.

`androidApp/build.gradle.kts` loads `keystore.properties` (`storeFile` as an absolute path, `storePassword`,
`keyAlias`, `keyPassword`) from a fixed path, `~/.android-keystores/keystore.properties`, and wires them into a
`release` `signingConfig`. Both the keystore file and `keystore.properties` live there — outside the repository
entirely, not merely gitignored-in-place. Generating the keystore (`keytool -genkeypair ...`) and its two
independent, high-entropy random passwords is now something I do as part of applying this change (task 2.1), not a
step the developer performs by hand — a script-generated random password is stronger than what a human tends to
pick, and `keytool` is deterministic and safe to automate. Regenerating the keystore itself remains something that
must never happen by accident (it would invalidate every previously-distributed build's upgrade path), so the task
generates it once and leaves it alone afterward; I flag this explicitly to the developer before running it.

**The security question asked, and answered**: is `.gitignore` alone strong enough defense if both the keystore
file and its password lived inside the repo directory? Researched directly rather than assumed — modern PKCS12 (the
default keystore format since JDK 9) uses `PBEWithHmacSHA256AndAES_256` with a 100,000-iteration KDF as of
[JDK-8228481](https://bugs.openjdk.org/browse/JDK-8228481) (landed JDK 16; this project's JDK 25 postdates it by
years), so the encryption itself is genuinely strong — not the weak legacy JKS scheme. But that's not the risk that
matters: encryption strength is irrelevant once the decryption password sits next to the ciphertext under the same
protection boundary. `.gitignore` only stops `git add .` from picking a file up; it does nothing against
`git add -f`, a backup or cloud-sync tool mirroring the whole project directory regardless of git's ignore rules, or
a compromised dev machine. A leaked release key isn't rotatable the way an API key is — Android requires the same
key to sign every future update — so this decision favors the conservative default over the convenient one: keeping
both files outside the repo keeps the filesystem access-control boundary as the only boundary that has to hold,
rather than adding "no `.gitignore` gap, ever" as a second one.

A related question the developer also raised: since the encryption is strong enough that offline brute-force against
a high-entropy random password isn't practically feasible (not even against a plausible future quantum computer —
Grover's algorithm only halves AES-256's effective strength, to a still-infeasible ~128-bit equivalent), would it be
safe to commit the *encrypted keystore file itself* to git (keeping only `keystore.properties` outside), using git as
the distribution/backup mechanism? This repo is currently private (`mliikanen/driving-log`, confirmed), which bounds
today's exposure — but committing to git is a one-way door: history persists in every clone made before a repo ever
goes public, a collaborator is added, or an account is compromised, and purging history to guarantee removal after
the fact is real work and easy to get wrong. Given a release signing key is essentially non-rotatable once
distributed, this design accepts the more conservative option (both files outside git) even though the encrypted
file alone would very likely be safe to commit — the asymmetry between "saved a `mkdir`" and "burned the app's
identity for every tester" favors caution. The backup gap this previously left as an accepted risk (see Risks below)
is instead solved by an explicit backup task, not by using git as the backup mechanism for a secret that was
deliberately kept out of it.

If `~/.android-keystores/keystore.properties` is missing, the `release` signing config is left unconfigured, and a
`doFirst` check on the tasks that build a release artifact (`assembleRelease`, `bundleRelease`, and transitively the
App Distribution upload task) throws a clear `GradleException` naming the missing file — satisfying the spec's
"fails before producing or uploading any artifact" requirement explicitly, rather than relying on whatever the App
Distribution plugin happens to do with an unsigned build.

### Both `versionCode` and `versionName` are fully automated — no hand-edited version value anywhere
**Revised from the original design**, which had the developer hand-edit `versionName` as a deliberate semver
judgment call. The developer asked for that manual step to be removed too: version increments must be automated,
not just `versionCode`.

`versionCode` is unchanged: computed at Gradle configuration time via `providers.exec { commandLine("git",
"rev-list", "--count", "HEAD") }` — the `ProviderFactory.exec` API, which (unlike `project.exec {}` or a raw
`Runtime.exec` call at configuration time) is configuration-cache-compatible, and this project already has
configuration cache on (`gradle.properties`). It runs for every build, so it is always in step with the checked-out
commit and can never be typed twice for two different commits.

`versionName` is now derived the same way, with no tracked value to edit: `"<versionCode>-<short SHA>"` (e.g.
`"142-a1b2c3d"`), computed alongside `versionCode` from `providers.exec { commandLine("git", "rev-parse",
"--short", "HEAD") }`. This is the developer's own proposed scheme, evaluated against the alternatives below and
adopted as-is.

**Alternatives considered for `versionName`:**

| Option | Automated? | Traces to an exact commit? | Readable at a glance? | Needs a new convention? |
|---|---|---|---|---|
| **`<commitCount>-<shortSha>`** (chosen) | Yes | Yes, exactly | Ordering only (bigger = newer) | No |
| `<commitCount>` alone, no hash | Yes | Only if history stays linear (true today, not guaranteed forever) | Ordering only | No |
| Calendar version (`YYYY.MM.DD.<n>`) | Yes | No — same-day builds only disambiguated by `<n>`, not tied to a commit | Yes — a human can tell roughly how old a build is | No, but needs a build-time clock read |
| Semver with a hand-set MAJOR.MINOR, auto PATCH | **No** — MAJOR.MINOR is still a manual, deliberate choice | No | Yes — communicates the scale of a change | No |
| Semver derived from Conventional Commits (`feat:`/`fix:`/`BREAKING CHANGE:` prefixes) | Yes | No | Yes | **Yes** — this project's commit messages don't follow that convention (they read as prose tied to the OpenSpec change being archived, e.g. "add-event-notes: implement all tasks"), so adopting it is a separate, bigger process change, not a versioning tweak |

Chosen: `<commitCount>-<shortSha>`, because it is the only option that is both fully automated (satisfying the
requirement directly) and traceable to the *exact* commit a tester's build was cut from — the single most useful
property for "which build is this, exactly?" debugging, and cheap to compute (one more `providers.exec` call
alongside the one `versionCode` already makes). Plain commit count without the hash was rejected only because the
hash is free insurance against a future non-linear history (a rebase or a force-push) silently making the count
ambiguous; today's history is linear, but there is no cost to not depending on that staying true. Calendar
versioning and semver (either form) were rejected: both either reintroduce a manual step (hand-set MAJOR.MINOR) or
need a commit-message convention this project does not use and would have to adopt project-wide just to make
versioning derivable — out of proportion for what a version string needs to do here (identify a build precisely,
nothing more; the actual human-readable "what changed" story is the release notes, not the version string).

Rejected alternative for `versionCode` (unchanged from the original design): deriving it from the count of `dist-v*`
tags instead of commits. It would stay flat across every ordinary commit between releases and only move on a
release, which is weaker (a rebuild of the same tagged commit, or a mistake, could silently reuse a code) than a
monotonic count tied to the commit graph itself.

With `versionName` now always prefixed by `versionCode`, the distribution tag (see below) drops the separate
`-<versionCode>` suffix it originally had: `dist-v<versionName>` already contains it.

### Release notes MUST be generated from OpenSpec changes archived since the previous release
This is a requirement, not a convenience default: the distribution flow does not accept freely hand-typed notes as
its normal path. Release notes are always *generated* from `git log --diff-filter=A` over
`openspec/changes/archive/` between the last `dist-v*` tag and `HEAD` first, and only then opened for the
developer's review/edit — editing a generated draft is expected (wording, trimming), replacing generation with a
blank sheet is not, and the one narrow exception (below) is deliberately the only path around it.

A distribution tag (`dist-v<versionName>`, unchanged in spirit, simplified in form now that `versionName` already
contains `versionCode` — see the versioning decision above) records exactly the commit an earlier distribution was
cut from. `scripts/distribute.sh` finds the most recent such tag (`git tag -l 'dist-v*' --sort=-v:refname`, or, if
none exists yet, the whole history), diffs `openspec/changes/archive/` for added top-level entries in that range
(every `/opsx:archive` run adds exactly one new `openspec/changes/archive/<name>/` directory in its own commit, so
this reliably enumerates "changes archived since the last distribution" without needing any new bookkeeping file or
a change to the archive skill itself, which is vendored/updatable and not something this change should hook into),
and writes one line per archived change name to a draft file the developer opens to review/edit before the upload
proceeds. The one narrow exception: if the draft comes out empty (nothing was archived since the last distribution —
generation legitimately has nothing to generate from), the script requires the developer to type notes by hand
instead, rather than silently uploading with blank notes — a build with nothing to say usually still has *some*
reason for existing (a config or signing fix, say), and the spec calls this out explicitly as the one case where
hand-typed notes are the whole story rather than an edit on top of a generated one.

Alternative considered: maintain a hand-curated `RELEASE_NOTES.md` "Unreleased" section, updated as a step of every
`/opsx:archive` run. Rejected for this change: that skill is a vendored, `openspec init`-managed file
(`.claude/skills/openspec-archive-change/SKILL.md`); teaching it a new, project-specific side effect is a bigger,
separate change of its own (and would need to survive a future `openspec update`), not something to fold in here.
The git-log approach needs no changes to any vendored tooling and reaches the same result from data that already
exists.

### One command: `scripts/distribute.sh`
A single shell script, not a bare Gradle task, because the flow has steps a Gradle task graph doesn't fit well: it
must refuse early (before any build work) when there is nothing new to distribute, open the drafted release notes
for the developer's editing, and only tag the commit *after* a successful upload. It shells out to
`./gradlew :androidApp:appDistributionUploadRelease -PdistributionReleaseNotesFile=<path>` for the actual build and
upload (letting Gradle/AGP/the App Distribution plugin do the parts they already do well), then runs the `git tag`
itself once that succeeds. The tag is created locally only; per this project's standing convention, nothing pushes
without an explicit, separate request.

## Risks / Trade-offs

- **[Risk]** A developer's Firebase CLI session expires or was never logged in on a given machine, so the upload
  fails after the (possibly slow) build already ran → **Mitigation**: accepted for v1 — the spec requires a clear,
  actionable error ("run `firebase login`"), not that the check happens before the build; catching it earlier would
  mean also invoking the Firebase CLI's own auth-status check as a separate dependency, for a failure mode that is
  rare (login persists across runs) and cheap to retry.
- **[Risk]** `git rev-list --count HEAD` counts commits on whatever branch is checked out; a history rewrite (force
  push, rebase of already-distributed commits) could make a future `versionCode` (and, since it is a prefix of
  `versionName` too, `versionName`) collide with or fall below one already distributed → **Mitigation**: accepted;
  this project's history is linear so far (no force-pushes in the log), and the "no new commit since last
  distribution" check catches the common accidental case (re-running the script twice in a row) even though it
  can't catch a deliberate rewrite. `versionName`'s appended short SHA at least keeps two builds from ever looking
  identical even if this happened.
- **[Risk]** The keystore is a single file on one developer's machine with no backup → **Mitigation**: task 2.1
  explicitly tells the developer to back up `~/.android-keystores/` (both the keystore and its passwords) to a
  password manager or encrypted drive right after it's generated, since losing it means every future release cannot
  upgrade-install over an earlier one on a tester's device. Deliberately not solved by committing the (encrypted)
  keystore to git instead — see the signing decision above for why that trade was considered and declined.
- **[Risk]** Both `~/.android-keystores/driving-log-release.jks` and its passwords are generated and briefly visible
  to whatever ran task 2.1 (a terminal session, an agent's tool output/transcript) → **Mitigation**: accepted; the
  passwords are only ever written to `keystore.properties` and the `keytool` invocation that creates the keystore,
  never to a file this change commits or a log this change persists beyond that one-time setup. This is the same
  exposure any locally-run `keytool`/password-generation step has; nothing about automating it widens it.
