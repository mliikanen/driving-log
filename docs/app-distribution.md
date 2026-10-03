# How app distribution is designed

For the day-to-day "how do I cut a release" steps, see `docs/distribution.md`. This is the durable summary of *why*
it's built this way — the decisions from `add-app-distribution`'s `design.md`, kept here since that file moves under
`openspec/changes/archive/` once the change is archived and becomes harder to find.

## What's automated, what's manual, and why

Only the developer's own interactive `firebase login` is irreducibly manual — it needs a browser OAuth flow tied to
their Google account. Everything else that looked like a manual "Firebase console" step turned out to have a
Firebase CLI equivalent, confirmed by using them directly rather than assumed from documentation alone:
- Registering the Android app and getting its Firebase App ID: `firebase apps:create ANDROID ...`.
- Creating a tester group: `firebase appdistribution:group:create ...`.

Both can be run for the developer once they've logged in — there's no reason to treat them as console-only manual
steps.

**No `google-services.json`.** The App Distribution Gradle plugin accepts `appId` directly in `build.gradle.kts`
(confirmed against Firebase's own plugin docs) — that file/plugin only matters for other Firebase products (Auth,
Firestore) this project isn't using in code. `appId` isn't a secret (it's visible in Firebase console URLs and every
distributed APK's manifest anyway), so it's a plain, tracked value in `gradle.properties`.

**No service-account credential file.** A local distribution (`scripts/distribute.sh`) authenticates with the
developer's own `firebase login` CLI session, so no key file exists for it. CI distributes too (Publishing from CI,
below), and even there no long-lived key exists: it signs in as a service account through Workload Identity
Federation.

## The release keystore

Generated once by whoever applies this change (`keytool -genkeypair`), not the developer by hand — a
script-generated random password is stronger than one a person tends to pick. **One password, not two**: PKCS12
keystores (the modern default since JDK 9) don't support a key password different from the store password —
`keytool` silently ignores `-keypass` and uses `-storepass` for the key entry too. (Discovered by hitting it: an
earlier attempt with two independently generated passwords produced a keystore `apksigner` couldn't actually sign
with — "Given final block not properly padded," the classic wrong-key symptom.)

Both the keystore file and `keystore.properties` live at `~/.android-keystores/`, entirely outside the repository —
not merely gitignored-in-place. Two things were deliberately *not* done, on request, after research:
- **Keeping both inside the repo behind `.gitignore`**: rejected. Modern PKCS12 encryption
  (`PBEWithHmacSHA256AndAES_256`, a strong PBES2 KDF since [JDK-8228481](https://bugs.openjdk.org/browse/JDK-8228481))
  is genuinely strong, but that's irrelevant once the password sits next to the ciphertext under the same
  protection boundary. `.gitignore` only stops `git add .` — not `git add -f`, a backup/sync tool mirroring the
  whole project directory, or a compromised dev machine.
- **Committing the encrypted keystore file to git** (keeping only `keystore.properties` outside): also rejected,
  even though a high-entropy password would likely make this safe against offline brute-force. Committing to git is
  a one-way door — history persists in every clone made before a private repo ever goes public or a collaborator's
  access is compromised. A release signing key is essentially non-rotatable once distributed (Android requires the
  same key to sign every future update), so this favors the conservative option even where the convenient one would
  probably also be fine.

The backup gap this leaves (a single file, one machine) is solved by an explicit one-time task — back it up to a
password manager or encrypted drive — not by using git as the backup mechanism.

Because Gradle's release lifecycle tasks (`assembleRelease`, `bundleRelease`) run their dependencies — including the
tasks that actually write the APK/AAB — before their own `doFirst` fires, the "fail if the keystore is missing"
check is a dedicated task (`checkReleaseSigning`) that the real artifact-writing tasks (`packageRelease`,
`packageReleaseBundle`) depend on, not a `doFirst` on the lifecycle tasks themselves. A `doFirst` there was tried
first and let a full unsigned build complete before ever checking anything.

## Versioning: fully automated, no hand-edited value anywhere

`versionCode` is the count of commits reachable from the built commit (`git rev-list --count HEAD`). `versionName`
is `"<that count>-<short SHA>"`. Both computed at Gradle configuration time via `providers.exec` (not `project.exec`
or a raw process call), the config-cache-compatible API — this project has configuration cache on.

Considered and rejected: calendar versioning (doesn't trace to an exact commit), semver with a hand-set MAJOR.MINOR
(reintroduces a manual step), and semver derived from Conventional Commits (this project's commit messages don't
follow that convention, and adopting one just for versioning is out of proportion). Commit count alone, without the
short SHA, was also rejected — the hash is free insurance against a future non-linear history (a rebase or
force-push) making the count ambiguous.

**A real Gradle pitfall hit while building this**: referencing a top-level `.gradle.kts` script `val` from inside a
later `doFirst`/task-registration closure captures a reference to the script object itself, which the configuration
cache cannot serialize. The fix is to scope such values inside a local block (a `run { }`, or any nested scope) so
they're plain lambda captures instead of script-class fields.

## Release notes: generated from the OpenSpec archive, not typed from scratch

This is a requirement, not a convenience default — the distribution script does not accept freely hand-typed notes
as its normal path. `scripts/distribute.sh` finds the most recent `dist-v<versionName>` tag (or, on the first-ever
distribution, uses the whole history), and lists the OpenSpec changes newly archived since then as the draft, which
the developer reviews and edits before uploading. **The one exception**: if nothing was archived since the last
distribution, there's nothing to generate, and the developer types notes by hand instead.

Considered and rejected: a hand-curated `RELEASE_NOTES.md` "Unreleased" section updated by the archive skill.
Rejected because that skill is vendored (`openspec init`-managed) — teaching it a new, project-specific side effect
would need to survive a future `openspec update`, which is a bigger, separate change of its own.

**A real bug found while implementing this**: `openspec/changes/archive/` has a tracked `.gitkeep` file directly in
it (from the initial commit). `git log --diff-filter=A --name-only` matches it too, and a naive `sed` pattern
expecting every matched path to be `<change-name>/<file>` left `.gitkeep`'s bare path unchanged, leaking a bogus
"change name" into the generated notes. The fix requires a subdirectory in the filter
(`openspec/changes/archive/[^/]+/`) before extracting the name. Also worth knowing: `git commit`'s own summary shows
an archived change's files as renames (old path → new path), but `git log`'s diff — without rename detection, which
is off by default — sees them as a plain delete-and-add, which is what lets `--diff-filter=A` find them at all;
`scripts/distribute.sh` passes `--no-renames` explicitly so a different git config can't silently break this.

## ABIs

A release APK carries native code for arm64-v8a only, and a debug build for arm64-v8a and x86_64 (the emulator), with
native libraries stored compressed (`androidApp/build.gradle.kts`). The on-device OCR libraries (ML Kit, and ONNX
Runtime from `add-seven-segment-ocr`) are 20-40 MB per ABI, so a universal APK would carry four copies of each; every
device at this minSdk is 64-bit. A tester with an x86 or 32-bit ARM device could not install a release. Sizes are in
`add-seven-segment-ocr`'s design.md.


## Publishing from CI

Every merge to `main` publishes to the testers, with no developer machine involved (`add-ci-workflows`):
- **Trigger.** `.github/workflows/release.yml` runs when the PR check (`pr-check.yml`) has passed on a push to `main`,
  that is, on a merge commit, and builds exactly the commit that passed. It can also be run by hand (*Run workflow*).
  One release runs at a time; a later one waits.
- **Nothing to say, nothing published.** When no change was archived since the last `dist-v*` tag and the merged PR
  has no `## Release notes` section (a docs or tooling merge), it publishes nothing and creates no tag. A commit that
  is already tagged isn't published again.
- **Release notes.** The merged PR's `## Release notes` section when it has one (the herd's reviewer writes it; a
  person can edit it before merging), otherwise the archive-generated list (`scripts/release-notes.sh`, the same
  generation the local script drafts from). The PR check shows that text in its job summary, so it's reviewed before
  merging: that is the review the local script does in `$EDITOR`.
- **Secrets** live in the `firebase-deployment` GitHub environment, which only `main` can enter (no pull request, even
  one that edits a workflow, can read them): the real `google-services.json`, the release keystore and its password,
  base64-encoded where they're files. The job writes them to the same paths the build reads locally
  (`androidApp/src/production/`, `~/.android-keystores/`), so the Gradle build is the same as on a laptop. The
  keystore is the one standing credential this adds, added with explicit sign-off.
- **Upload identity.** A dedicated service account whose only role is Firebase App Distribution Admin, impersonated
  through Workload Identity Federation by this repository's `firebase-deployment` runs only. Its provider and email
  are environment *variables* (`WIF_PROVIDER`, `WIF_SERVICE_ACCOUNT`), not secrets.
- **Full history and tags.** `versionCode` is `git rev-list --count HEAD`, so the job checks out with full history.
  It fetches the remote's tags first and pushes the `dist-v<versionName>` tag it creates, and so does
  `scripts/distribute.sh`, so a local and a CI distribution agree on what was distributed last.
- **The PR check needs none of this.** It builds the `production` flavor with a placeholder `google-services.json`
  (`.github/ci/`), so pull requests run with no secrets at all.

The one-time setup (environment, service account, secrets) is in `docs/distribution.md`.
