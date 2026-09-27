# Tasks

## 1. One-time Firebase setup

Only 1.1 (the developer's own interactive login) is a step the assistant genuinely cannot perform — it needs a
browser OAuth flow tied to the developer's own Google account. **Revised during implementation**: 1.2 and 1.3 were
originally planned as manual Firebase console steps; researched and confirmed both have real Firebase CLI
equivalents (`apps:create`, `appdistribution:group:create`), so once 1.1 is done, the assistant can run them.
None of these block writing the code in sections 2-5 (which reads the values these produce from tracked,
initially-placeholder config), only its end-to-end verification.

- [ ] 1.1 Install the Firebase CLI if not already present and run `firebase login` (interactive; the developer does
      this themselves). Verify with `firebase projects:list` showing the existing project.
- [ ] 1.2 Register an Android app with package `com.mikonoma.drivinglog`:
      `firebase apps:create ANDROID "Driving Log" --package-name com.mikonoma.drivinglog --project <project-id>`.
      App Distribution needs no separate "enable" step — it's usable for any registered app. Verify by noting the
      Firebase App ID the command prints (`1:...:android:...`), or `firebase apps:list ANDROID --project
      <project-id>`.
- [ ] 1.3 Create a tester group: `firebase appdistribution:group:create "Testers" testers --project <project-id>`
      (the alias `testers` matches `firebaseTesterGroup`'s placeholder in `gradle.properties`, task 4.1 — use a
      different alias only if updating that placeholder to match). Verify with `firebase appdistribution:group:list
      --project <project-id>`.

## 2. Release signing in the Android build

- [x] 2.1 Generate the release keystore: `mkdir -p ~/.android-keystores`, then
      `keytool -genkeypair -v -keystore ~/.android-keystores/driving-log-release.jks -alias driving-log -keyalg RSA
      -keysize 2048 -validity 10000 -storepass <generated> -dname "CN=Driving Log, OU=Development, O=Driving Log"`
      with one high-entropy random password (not developer-chosen — e.g. 32 random bytes, base64-encoded). **Not
      two independent passwords, corrected during implementation**: PKCS12 keystores (the modern default) don't
      support a key password different from the store password — `keytool` ignores `-keypass` and uses `-storepass`
      for the key entry too (see 2.4's note). Write `~/.android-keystores/keystore.properties` (`storeFile` as the
      keystore's absolute path, `storePassword`, `keyAlias`, `keyPassword` — the same value as `storePassword`)
      alongside it — both files live outside the repository entirely, never gitignored-in-place (see design.md's
      signing decision). Verify the keystore file exists and
      `keytool -list -keystore ~/.android-keystores/driving-log-release.jks` reads it. Tell the developer to back up
      `~/.android-keystores/` (both files) somewhere durable — a password manager that supports file attachments, or
      an encrypted drive — since this is a long-lived, non-rotatable credential: losing it means no future release
      can upgrade-install over an earlier one on a tester's device.

      Generated via `openssl rand -base64 32` (256 bits of entropy). Verified
      with `keytool -list`: PKCS12, one `PrivateKeyEntry` under alias `driving-log`. Both files chmod 600 as an
      extra local hardening beyond what the design called for. **Passwords were not printed to this session's
      visible output** — generated and piped directly into `keytool`/the properties file within one script.
      **Action needed from you**: back up `~/.android-keystores/` (both files) now, per the note above — I can't do
      this step for you (it needs your own password manager/encrypted storage).
- [x] 2.2 In `androidApp/build.gradle.kts`, load `keystore.properties` from the fixed path
      `~/.android-keystores/keystore.properties` (`System.getProperty("user.home")` + the fixed subpath, not a
      project-relative or gitignored-in-repo file — it never enters the project directory) and wire it into a
      `release` `signingConfig`. Verify by confirming the file this reads from is outside `git status`'s view
      entirely (nothing to ignore, because nothing is there to be tracked).

      Scoped the properties-loading and signing-config logic inside a local `run { }` block rather than top-level
      script `val`s — referencing a top-level `.gradle.kts` `val` from inside a later `doFirst`/task-registration
      closure captures a reference to the script object itself, which Gradle's configuration cache cannot
      serialize (confirmed by hitting exactly that error and fixing it; see 2.3's note for the full story).
- [x] 2.3 Add a `doFirst` check on the tasks that build a release artifact (`assembleRelease`, `bundleRelease`)
      that fails with a clear `GradleException` naming `~/.android-keystores/keystore.properties` when that file is
      missing. Verify by running `./gradlew :androidApp:assembleRelease` with that file absent (or temporarily
      renamed) and confirming it fails before producing an APK, with that message.

      Not a plain `doFirst` on `assembleRelease`/`bundleRelease` in the end — those are lifecycle tasks, and their
      own `doFirst` only runs *after* every dependency (including the tasks that actually write the APK/AAB) has
      already executed, which let a real build run to completion before the check ever fired. Fixed by adding a
      dedicated `checkReleaseSigning` task that the real artifact-writing tasks (`packageRelease` for the APK,
      `packageReleaseBundle` for the AAB) `dependsOn`, guaranteeing it runs — and can fail — strictly before either
      writes anything. Verified: with `keystore.properties` renamed away, the build fails at `checkReleaseSigning`
      after only 10 tasks (module setup, no packaging), and `androidApp/build/outputs/apk/release/` is never
      created.
- [x] 2.4 With the real keystore in place (from task 2.1), run `./gradlew :androidApp:assembleRelease` and verify
      the produced APK is signed with the release key (`apksigner verify --print-certs
      androidApp/build/outputs/apk/release/androidApp-release.apk` shows the release keystore's certificate, not
      the debug one).

      Hit and fixed a real bug from task 2.1 here: PKCS12 keystores (the modern default `keytool` uses) do not
      support a key password different from the store password — `keytool` printed "Warning: Different store and
      key passwords not supported for PKCS12 KeyStores. Ignoring user-specified -keypass value." and silently used
      `storepass` for the key entry too, so the separately-generated `keyPassword` I'd written to
      `keystore.properties` didn't actually decrypt the key (`apksigner`/AGP failed with "Given final block not
      properly padded" — a wrong-key symptom). Fixed by setting `keyPassword` equal to `storePassword` in
      `keystore.properties` (the keystore file itself needed no change — it was already correctly generated with
      one effective password). Task 2.1's own text above is corrected to match: one random password, used for
      both, not two independent ones. Verified: `apksigner verify --print-certs` on the built APK shows SHA-256
      `98:3B:CF:CE:97:02:A9:4F:CC:D6:AB:17:BB:BA:3C:CD:A4:AD:F0:43:5C:EF:48:1B:F1:3E:A2:7E:D1:B9:0E:D6`, matching
      `keytool -list`'s fingerprint for the release keystore exactly — not the debug key.

## 3. Versioning (fully automated — no hand-edited version value)

- [x] 3.1 Replace the hardcoded `versionCode = 1` with a value computed via `providers.exec { commandLine("git",
      "rev-list", "--count", "HEAD") }` at configuration time, and replace the hardcoded `versionName = "0.1.0"`
      with `"<that count>-<short SHA>"`, the short SHA from `providers.exec { commandLine("git", "rev-parse",
      "--short", "HEAD") }`. No tracked `gradle.properties` version value is added — see design.md for the
      alternatives this scheme was evaluated against. Verify `./gradlew :androidApp:assembleDebug` still succeeds
      and that a temporary `println` (removed before finishing) or `./gradlew :androidApp:dependencies` output
      shows a `versionCode` equal to `git rev-list --count HEAD`'s current output and a `versionName` matching
      `git rev-parse --short HEAD`.

      Verified with a temporary `println` in `defaultConfig` (removed after): `versionCode=140
      versionName=140-4cda033`, matching `git rev-list --count HEAD` / `git rev-parse --short HEAD` at the time
      exactly.
- [x] 3.2 Confirm two builds from the same commit produce the same `versionCode` and `versionName`, and a build
      after a new commit produces a strictly greater `versionCode` and a different `versionName` (make a trivial
      commit, e.g. to a scratch file, then revert it, and compare the computed values before and after).

      Committed a scratch file (`140-4cda033` → `141-43c7a35`, strictly greater and different, as expected), then
      committed its removal as a follow-up commit (both now in history: `43c7a35`, `f2eed39`).

## 4. Firebase App Distribution upload

- [x] 4.1 Add the Firebase App Distribution Gradle plugin to `gradle/libs.versions.toml` and apply it to
      `androidApp/build.gradle.kts`, configured with `appId` from a new tracked `firebaseAppId` gradle property
      (placeholder value until task 1.2's real one is known), `groups` from a new tracked `firebaseTesterGroup`
      gradle property (placeholder until task 1.3), and `releaseNotesFile` from a `-PdistributionReleaseNotesFile=`
      project property (no default — the caller must always supply one). Verify
      `./gradlew :androidApp:tasks --all | grep -i appDistribution` lists the upload task.

      Plugin `com.google.firebase.appdistribution` version `5.3.0` (confirmed current via Firebase's own docs and
      Maven Central). `appId`/`firebaseTesterGroup` placeholders added to the tracked `gradle.properties`;
      `releaseNotesFile` reads `project.findProperty("distributionReleaseNotesFile") as String? ?: ""` (no baked-in
      default, per design — an empty path will fail the upload task's own way if the caller omits the property).
      Verified: `appDistributionUploadRelease`, `appDistributionUploadDebug`, `appDistributionAddTesters`,
      `appDistributionRemoveTesters` all listed by `:androidApp:tasks --all`.
- [ ] 4.2 Fill in the real `firebaseAppId` and `firebaseTesterGroup` values from tasks 1.2/1.3. With a signed
      release build available (task 2.4) and the developer logged in to the Firebase CLI (task 1.1), run
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
