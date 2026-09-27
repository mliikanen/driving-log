# Proposal

## Why

`CLAUDE.md`/the project context already states "Firebase app distribution is to be used to distribute app test
builds," but nothing exists yet to do that: no Firebase App Distribution wiring in the Android build, no release
signing config, no versioning scheme beyond the two hardcoded values `versionCode = 1` / `versionName = "0.1.0"` in
`androidApp/build.gradle.kts` (never bumped since the initial commit), and no way to hand testers notes on what
changed. The developer needs a single, repeatable local command that cuts a signed, versioned build and uploads it
to the testers already in the existing Firebase project, with release notes a reader can act on.

## What Changes

- Add a release signing config for `androidApp`'s `release` build type, backed by a keystore generated once as part
  of applying this change, with one high-entropy random password (PKCS12 keystores, the modern default, don't
  support a separate key password — `keytool` ignores one if given). The keystore and its password
  (`keystore.properties`) live at `~/.android-keystores/`, entirely outside the repository — not merely
  gitignored-in-place. See design.md's signing decision for the security reasoning (including the `.gitignore`-only
  and commit-the-encrypted-keystore alternatives that were considered and declined).
- Add the Firebase App Distribution Gradle plugin to `androidApp`, wired to upload the signed release build to a
  tester group, authenticated via the developer's own `firebase login` CLI session (no service-account key file to
  manage for this local-only flow).
- Replace the two hardcoded version fields with a fully automated scheme, with no manually maintained version value
  anywhere: `versionCode` is derived from the number of commits on the branch, and `versionName` is derived from
  that same commit count plus the built commit's short hash (`"<count>-<sha>"`), so version increments never depend
  on a developer remembering to bump anything. See design.md for the alternatives this was evaluated against
  (calendar versioning, hand-set-then-auto-patch semver, Conventional-Commits-derived semver) and why this one was
  chosen.
- Add a release-notes step that **must** generate its draft from the change names archived under
  `openspec/changes/archive/` since the last distributed build (found via a `dist-v*` git tag the distribution step
  creates) — this is the required source, not an optional convenience, tying distribution directly to the OpenSpec
  archive history: every distributed build's notes are literally the list of specced changes it contains. The
  developer reviews and can edit the generated draft before the upload runs; hand-typing notes from nothing is only
  reached when there is genuinely nothing archived to generate from.
- Document the one-time Firebase setup. Only the developer's own interactive `firebase login` is a step this change
  truly cannot do on their behalf (it needs a browser OAuth flow tied to their Google account); registering the
  Android app (obtaining its Firebase App ID, no `google-services.json` needed — see design.md) and creating a
  tester group both have real Firebase CLI equivalents (`apps:create`, `appdistribution:group:create`), so once the
  developer has logged in, this change's tooling can run them too.
- Out of scope for this change (noted as explicit follow-ups, since this is already sizeable and CI needs its own
  secret-management design): CI-triggered distribution (GitHub Actions, `workflow_dispatch` or on-push), and any
  real app store submission (Play Store), which the project context says isn't happening yet regardless.

## Capabilities

### New Capabilities
- `app-distribution`: local versioning, release signing, release-notes generation from the OpenSpec archive history,
  and uploading a signed build to Firebase App Distribution testers.

### Modified Capabilities
(none — no existing spec's user-observable behavior changes; this is entirely new, developer-facing build tooling)

## Impact

- `androidApp/build.gradle.kts`: signing config, versioning, Firebase App Distribution plugin application and its
  tester-group/release-notes wiring.
- `gradle/libs.versions.toml`: new entry for the Firebase App Distribution Gradle plugin only — no
  `google-services.json`/plugin (see design.md's decision on why).
- No `.gitignore` entry needed for the keystore: both the keystore file and `keystore.properties` live at
  `~/.android-keystores/`, outside the repository entirely, so neither is ever in a position to be committed — see
  design.md's signing decision.
- New: `scripts/distribute.sh`, which computes `versionCode`/`versionName` from git and generates release notes from
  `openspec/changes/archive/` since the last `dist-v*` tag.
- `docs/distribution.md`: a runbook for the one-time Firebase console setup and the day-to-day "cut a release" steps.
- `docs/app-distribution.md`: a design-facing doc for agents (and developers) working near this area — what's
  automated versus manual, the versioning scheme and why, and the release-notes-from-archive requirement — so a
  future change doesn't have to re-derive these decisions from `design.md` once this change is archived and that
  file is harder to find. `CLAUDE.md`'s `docs/` list gains a pointer to it, matching the existing pattern for
  `docs/test-strategy.md` etc.
- No changes to app runtime behavior, the database, or any existing spec.
