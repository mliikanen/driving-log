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

**Normally you don't: merging a PR into `main` publishes it** (`.github/workflows/release.yml`, see
`docs/app-distribution.md`). What goes out is shown in the PR check's job summary before you merge. To change the
notes, add or edit a `## Release notes` section in the PR description; its text replaces the generated list. A merge
that archives no change and has no such section publishes nothing. To publish the current `main` again by hand, run the
**Release** workflow (*Actions → Release → Run workflow*); a commit that's already tagged is skipped.

### Manual fallback: from your machine

```sh
scripts/distribute.sh
```

It will:
1. Fetch the remote's tags, then check there's something new to distribute (refuses if `HEAD` is already the last
   distribution, from here or from CI).
2. Draft release notes from the OpenSpec changes archived since the last distribution (`scripts/release-notes.sh`),
   and open them in `$EDITOR` (`nano` if unset) for you to review or edit. If nothing was archived since last time,
   the draft is empty and you type notes by hand instead — the script requires *some* non-empty note either way.
3. Build a signed release APK (`versionCode`/`versionName` are computed from git — nothing to type; built in the same
   Gradle run as the upload, because the upload task uploads whatever APK is on disk and builds none) and upload it to
   Firebase App Distribution's configured tester group.
4. On success, tag the built commit `dist-v<versionName>` and push the tag, so the next run, here or in CI, finds it.

If `~/.android-keystores/keystore.properties` is missing, the build fails immediately with that message, before
producing anything — see the one-time setup above. If the upload fails asking you to log in, run `firebase login`
again (CLI sessions expire).

## One-time CI setup

Done once, by the repository admin, with explicit sign-off on each credential (`add-ci-workflows` tasks 4.1 to 4.4).
`<project-id>` and `<project-number>` are the Firebase project's (`firebase projects:list`, or the Google Cloud
console).

1. **A plan that allows it.** Rulesets, and limiting an environment to `main`, need GitHub Pro on a private
   repository (or the repository made public). `gh api repos/mliikanen/driving-log/rulesets` answers with a list, not
   "Upgrade to GitHub Pro", once it's available.
2. **Limit the `firebase-deployment` environment to `main`:**
   ```sh
   gh api -X PUT repos/mliikanen/driving-log/environments/firebase-deployment \
     -F 'deployment_branch_policy[protected_branches]=false' -F 'deployment_branch_policy[custom_branch_policies]=true'
   gh api -X POST repos/mliikanen/driving-log/environments/firebase-deployment/deployment-branch-policies -f name=main
   ```
3. **The service account and Workload Identity Federation** (`gcloud auth login` first):
   ```sh
   gcloud iam service-accounts create driving-log-ci --project <project-id> --display-name "Driving Log CI distribution"
   gcloud projects add-iam-policy-binding <project-id> \
     --member "serviceAccount:driving-log-ci@<project-id>.iam.gserviceaccount.com" --role roles/firebaseappdistro.admin
   gcloud iam workload-identity-pools create github --project <project-id> --location global --display-name GitHub
   gcloud iam workload-identity-pools providers create-oidc driving-log --project <project-id> --location global \
     --workload-identity-pool github --issuer-uri https://token.actions.githubusercontent.com \
     --attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.environment=assertion.environment" \
     --attribute-condition "assertion.repository == 'mliikanen/driving-log' && assertion.environment == 'firebase-deployment'"
   gcloud iam service-accounts add-iam-policy-binding driving-log-ci@<project-id>.iam.gserviceaccount.com \
     --project <project-id> --role roles/iam.workloadIdentityUser \
     --member "principalSet://iam.googleapis.com/projects/<project-number>/locations/global/workloadIdentityPools/github/attribute.repository/mliikanen/driving-log"
   gh variable set WIF_PROVIDER --env firebase-deployment \
     --body "projects/<project-number>/locations/global/workloadIdentityPools/github/providers/driving-log"
   gh variable set WIF_SERVICE_ACCOUNT --env firebase-deployment --body "driving-log-ci@<project-id>.iam.gserviceaccount.com"
   ```
   The provider only accepts tokens from this repository's `firebase-deployment` runs, and the account can do nothing
   but distribute builds. If the upload plugin turns out not to accept the federated credential, the fallback is a
   JSON key for the same account as the secret `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` (the change's design.md,
   decision 4).
4. **The secrets:**
   ```sh
   gh secret set GOOGLE_SERVICES_JSON_BASE64 --env firebase-deployment --body "$(base64 -w0 androidApp/src/production/google-services.json)"
   gh secret set RELEASE_KEYSTORE_BASE64 --env firebase-deployment --body "$(base64 -w0 ~/.android-keystores/driving-log-release.jks)"
   gh secret set RELEASE_KEYSTORE_PASSWORD --env firebase-deployment   # prompts for the password
   ```
5. **The ruleset on `main`**, once both PR checks have passed on `main` at least once (GitHub only offers check names
   it has seen):
   ```sh
   gh api -X POST repos/mliikanen/driving-log/rulesets --input .github/rulesets/main.json
   ```
   It requires a pull request for every change to `main`, both checks passing and the branch up to date, for everyone.

Then run the **Release** workflow once by hand to prove the path end to end.

## Retrying or checking a build without distributing

`./gradlew :androidApp:assembleProductionRelease` builds the signed APK alone
(`androidApp/build/outputs/apk/production/release/`), without touching Firebase or git tags — useful for confirming
signing works after setup, or after rotating the keystore's backup. (`add-firebase-auth` added a second, `fake`,
product flavor used only by the Maestro suite — it has no distribution configuration at all, so always use the
`production`-qualified task name here, not a bare `assembleRelease`.)
