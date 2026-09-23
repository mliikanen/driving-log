# Tasks

## 1. One-time Firebase & signing setup

These are manual steps the developer performs with their own Firebase/Google account and signing identity — the
assistant applying this change cannot perform them and should pause here, report what is needed, and wait rather
than guess or skip ahead. They do not block writing the code in sections 2-5 (which reads the values these produce
from tracked, initially-placeholder config), only its end-to-end verification.

- [ ] 1.1 In the existing Firebase project's console, register an Android app with package
      `com.mikonoma.drivinglog` and enable App Distribution for it. Verify by noting the Firebase App ID it is
      assigned (`1:...:android:...`).
- [ ] 1.2 In Firebase App Distribution, create a tester group and note its name. Verify the group is visible in the
      Firebase console's App Distribution testers page.
- [ ] 1.3 Generate a release keystore (`keytool -genkeypair -v -keystore <path outside this repo> -alias <alias>
      -keyalg RSA -keysize 2048 -validity 10000`) and store it somewhere outside the repository, with its passwords
      kept safely (this is a long-lived credential; losing it means no future release can upgrade-install over an
      earlier one). Verify the keystore file exists at the chosen path and `keytool -list -keystore <path>` reads it.
- [ ] 1.4 Install the Firebase CLI if not already present and run `firebase login`. Verify with `firebase
      projects:list` showing the existing project.

## 2. Release signing in the Android build

- [ ] 2.1 In `androidApp/build.gradle.kts`, load a gitignored `keystore.properties` (`storeFile`, `storePassword`,
      `keyAlias`, `keyPassword`) and wire it into a `release` `signingConfig`. Add `keystore.properties` to
      `.gitignore`. Verify with `git status` that a locally created `keystore.properties` is ignored.
- [ ] 2.2 Add a `doFirst` check on the tasks that build a release artifact (`assembleRelease`, `bundleRelease`)
      that fails with a clear `GradleException` naming `keystore.properties` when that file is missing. Verify by
      running `./gradlew :androidApp:assembleRelease` with no `keystore.properties` present and confirming it fails
      before producing an APK, with that message.
- [ ] 2.3 With a real `keystore.properties` in place (from task 1.3), run `./gradlew :androidApp:assembleRelease`
      and verify the produced APK is signed with the release key (`apksigner verify --print-certs
      androidApp/build/outputs/apk/release/androidApp-release.apk` shows the release keystore's certificate, not
      the debug one).

## 3. Versioning (fully automated — no hand-edited version value)

- [ ] 3.1 Replace the hardcoded `versionCode = 1` with a value computed via `providers.exec { commandLine("git",
      "rev-list", "--count", "HEAD") }` at configuration time, and replace the hardcoded `versionName = "0.1.0"`
      with `"<that count>-<short SHA>"`, the short SHA from `providers.exec { commandLine("git", "rev-parse",
      "--short", "HEAD") }`. No tracked `gradle.properties` version value is added — see design.md for the
      alternatives this scheme was evaluated against. Verify `./gradlew :androidApp:assembleDebug` still succeeds
      and that a temporary `println` (removed before finishing) or `./gradlew :androidApp:dependencies` output
      shows a `versionCode` equal to `git rev-list --count HEAD`'s current output and a `versionName` matching
      `git rev-parse --short HEAD`.
- [ ] 3.2 Confirm two builds from the same commit produce the same `versionCode` and `versionName`, and a build
      after a new commit produces a strictly greater `versionCode` and a different `versionName` (make a trivial
      commit, e.g. to a scratch file, then revert it, and compare the computed values before and after).

## 4. Firebase App Distribution upload

- [ ] 4.1 Add the Firebase App Distribution Gradle plugin to `gradle/libs.versions.toml` and apply it to
      `androidApp/build.gradle.kts`, configured with `appId` from a new tracked `firebaseAppId` gradle property
      (placeholder value until task 1.1's real one is known), `groups` from a new tracked `firebaseTesterGroup`
      gradle property (placeholder until task 1.2), and `releaseNotesFile` from a `-PdistributionReleaseNotesFile=`
      project property (no default — the caller must always supply one). Verify
      `./gradlew :androidApp:tasks --all | grep -i appDistribution` lists the upload task.
- [ ] 4.2 Fill in the real `firebaseAppId` and `firebaseTesterGroup` values from tasks 1.1/1.2. With a signed
      release build available (task 2.3) and the developer logged in to the Firebase CLI (task 1.4), run
      `./gradlew :androidApp:appDistributionUploadRelease -PdistributionReleaseNotesFile=<a scratch notes file>`
      once as a real end-to-end check. This uploads a real build to real testers — pause and get the developer's
      go-ahead before running it, rather than running it unprompted. Verify the build appears in the Firebase
      console's App Distribution release history for the configured tester group.
- [ ] 4.3 Run the same command signed out of the Firebase CLI (`firebase logout`, then log back in afterward) and
      verify the upload fails with an error that tells the developer to run `firebase login`, not a generic
      stack trace.

## 5. The distribute script and documentation

- [ ] 5.1 Write `scripts/distribute.sh`: find the most recent `dist-v*` tag (or none); refuse and exit before any
      build work if `HEAD` is the same commit that tag points at; draft release notes into a scratch file by
      listing the top-level names newly added under `openspec/changes/archive/` in `git log --diff-filter=A
      --name-only <last-tag-or-empty>..HEAD -- openspec/changes/archive` (the full archive history when there is no
      previous tag); open the draft for the developer to review/edit and require it to be non-empty before
      continuing; run `./gradlew :androidApp:appDistributionUploadRelease
      -PdistributionReleaseNotesFile=<that file>`; on success, tag the built commit `dist-v<versionName>` locally
      (no push; `versionName` already contains the commit count and short SHA). Verify by making the script executable
      (`chmod +x`) and dry-running its tag-lookup and notes-drafting logic (e.g. `bash -x` up to the point it would
      invoke Gradle) against this repo's real history.
- [ ] 5.2 Verify the "nothing archived since the last distribution" path: with a `dist-v*` tag already at `HEAD`'s
      immediate parent commit (simulate with a temporary tag on a merge/no-archive commit) confirm the script's
      draft comes out empty and it requires the developer to type notes rather than proceeding with a blank file.
- [ ] 5.3 Write `docs/distribution.md`: the one-time setup (section 1) and the day-to-day "cut a release" steps
      (run `scripts/distribute.sh`, what it asks for, what it produces). Verify by re-reading it start to finish
      and confirming every command it names matches what the script and build actually do.
- [ ] 5.4 Add a one-line pointer to `docs/distribution.md` from `README.md`. Verify with `grep -n
      distribution.md README.md`.
- [ ] 5.5 Write `docs/app-distribution.md`: a design-facing doc for agents (and developers) working near this area,
      distinct from `docs/distribution.md`'s practical runbook — what's fully automated versus what's a one-time
      manual step and why (signing, `appId`/no `google-services.json`, CLI-login auth), the versioning scheme
      (`<commitCount>-<shortSha>`, both values always derived, never hand-edited) and why it was chosen over the
      alternatives in `design.md`'s table, and the release-notes-MUST-be-generated-from-the-archive requirement
      (including the one exception). This is the durable summary of `design.md`'s decisions, since `design.md`
      itself moves under `openspec/changes/archive/` once this change is archived and becomes less discoverable to
      a future agent working in this area. Add a pointer to it from `CLAUDE.md`'s `docs/` list, matching the
      existing entries for `docs/test-strategy.md`, `docs/test-fixtures.md` and `docs/color-palette.md`. Verify by
      re-reading it against `design.md` and confirming no decision or its rationale was dropped in the summary.

## 6. Regression

- [ ] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass. No Maestro manifest applies to this change: it touches only the Android build's signing,
      versioning and distribution tooling and a new developer-run script — no app screen or runtime behavior
      changes.
