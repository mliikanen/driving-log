# Distributing a build to testers

`scripts/distribute.sh` cuts a signed, versioned release build and uploads it to the project's Firebase App
Distribution testers, with release notes generated from what's been archived under `openspec/changes/archive/`
since the last distribution. See `docs/app-distribution.md` for why it's built this way; this doc is the practical
steps.

## One-time setup

Only the first step is something you do yourself in a browser — the rest are Firebase CLI commands, which can be
run for you once you've logged in.

1. **Log in to the Firebase CLI** (interactive; needs your own Google account):
   ```sh
   firebase login
   ```
   Verify with `firebase projects:list` — it should show the existing project.

2. **Register the Android app**, if it isn't already:
   ```sh
   firebase apps:create ANDROID "Driving Log" --package-name com.mikonoma.drivinglog --project <project-id>
   ```
   Note the Firebase App ID it prints (`1:...:android:...`). App Distribution needs no separate "enable" step —
   it's usable for any registered app. Set it in `gradle.properties`:
   ```properties
   firebaseAppId=1:...:android:...
   ```

3. **Create a tester group**, if it doesn't already exist:
   ```sh
   firebase appdistribution:group:create "Testers" testers --project <project-id>
   ```
   The alias (`testers` above) must match `firebaseTesterGroup` in `gradle.properties` (already set to `testers` by
   default — only change both together if you want a different alias).

4. **Generate the release keystore**, if it doesn't already exist at `~/.android-keystores/`:
   ```sh
   mkdir -p ~/.android-keystores
   keytool -genkeypair -v -keystore ~/.android-keystores/driving-log-release.jks -alias driving-log \
     -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Driving Log, OU=Development, O=Driving Log"
   ```
   Use one strong, random password when prompted (PKCS12 keystores — the modern default — don't support a separate
   key password; `keytool` ignores one if you give it). Write it, alongside the keystore's path, to
   `~/.android-keystores/keystore.properties`:
   ```properties
   storeFile=/home/you/.android-keystores/driving-log-release.jks
   storePassword=<your password>
   keyAlias=driving-log
   keyPassword=<the same password>
   ```
   **Back this up now** — a password manager that supports file attachments, or an encrypted drive. This is a
   long-lived, non-rotatable credential: losing it means no future release can upgrade-install over an earlier one
   on a tester's device. It is never gitignored-in-place; it isn't in the repository at all.

## Cutting a release

```sh
scripts/distribute.sh
```

It will:
1. Check there's something new to distribute (refuses if `HEAD` is already tagged with a previous distribution).
2. Draft release notes from the OpenSpec changes archived since the last distribution, and open them in `$EDITOR`
   (`nano` if unset) for you to review or edit. If nothing was archived since last time, the draft is empty and you
   type notes by hand instead — the script requires *some* non-empty note either way.
3. Build a signed release APK (`versionCode`/`versionName` are computed from git — nothing to type; built in the same
   Gradle run as the upload, because the upload task uploads whatever APK is on disk and builds none) and upload it to
   Firebase App Distribution's configured tester group.
4. On success, tag the built commit `dist-v<versionName>` locally (not pushed), so the next run can find it.

If `~/.android-keystores/keystore.properties` is missing, the build fails immediately with that message, before
producing anything — see the one-time setup above. If the upload fails asking you to log in, run `firebase login`
again (CLI sessions expire).

## Retrying or checking a build without distributing

`./gradlew :androidApp:assembleRelease` builds the signed APK alone (`androidApp/build/outputs/apk/release/`),
without touching Firebase or git tags — useful for confirming signing works after setup, or after rotating the
keystore's backup.
