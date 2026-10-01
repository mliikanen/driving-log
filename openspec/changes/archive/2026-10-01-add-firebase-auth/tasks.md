# Tasks

## 1. Before any code

- [x] 1.1 In the Firebase console, for the existing `driving-log-49c48` project: enable the Google sign-in
      provider, register the Android app's OAuth client with both the debug and release keystores' SHA-1
      fingerprints (`keytool -list -v -keystore ...` for each), and download `google-services.json` into
      `androidApp/src/production/` (the `production` flavor's own source set, not the module root — see task 1.3).
      Verified: the file is in place and `:androidApp:compileProductionDebugKotlin`/`assembleProductionDebug` both
      succeed (task 2.1).

      Both SHA-1 fingerprints were registered via the Firebase CLI (`firebase apps:android:sha:create`), no
      console access needed for that part. Enabling the Google sign-in provider itself had no CLI equivalent
      (checked: `firebase auth` only has `auth:export`/`auth:import`, and there's no `gcloud` installed) — the one
      genuinely manual step in this whole change, done via the console at
      https://console.firebase.google.com/project/driving-log-49c48/authentication/providers. The re-fetched
      `google-services.json` confirms three OAuth clients: the debug and release Android clients (matching the
      two registered SHA-1s) and a `client_type: 3` web client, which is what generates
      `R.string.default_web_client_id`.

      `google-services.json` is gitignored (`.gitignore`), not committed — a per-developer download, the same
      "kept outside version control" precedent as the release keystore (`docs/distribution.md`), just inside the
      repo tree since AGP requires it at this exact flavor-specific path.
- [x] 1.2 Add two product flavors to `androidApp/build.gradle.kts`: `production` and `fake` (design.md decision
      6), each inheriting the existing `debug`/`release` build types. Verify
      `./gradlew :androidApp:assembleProductionDebug :androidApp:assembleFakeDebug` both succeed (no dependency
      differences yet, just the flavor scaffolding).
- [x] 1.3 Add the Google Services Gradle plugin and the Firebase Auth / Credential Manager dependencies
      (`com.google.firebase:firebase-auth`, `androidx.credentials:credentials`,
      `androidx.credentials:credentials-play-services-auth`,
      `com.google.android.libraries.identity.googleid:googleid`) to the version catalog, scoped to the `production`
      flavor's own dependency configuration (`productionImplementation`), not the whole module. Verify
      `./gradlew :androidApp:assembleFakeDebug` still succeeds with `google-services.json` temporarily removed
      (confirming the `fake` flavor has no Firebase dependency at all), then restore the file and confirm
      `./gradlew :androidApp:assembleProductionDebug` succeeds.
- [x] 1.4 Update the release-signing guard and distribution tooling for flavor-qualified task names (design.md
      Risks): `checkReleaseSigning`'s `tasks.matching { it.name == "packageRelease" || ... }` predicate now matches
      both flavors' flavor-qualified names (`packageProductionRelease`/`packageFakeRelease` and their `...Bundle`
      counterparts) — signing is shared, so both genuinely need it. `firebaseAppDistribution { ... }` moved into
      `productFlavors.production { ... }`, scoped to `production` only. Updated `scripts/distribute.sh`'s
      `:androidApp:assembleRelease :androidApp:appDistributionUploadRelease` invocation, and `docs/distribution.md`'s
      "Retrying or checking a build without distributing" section (`./gradlew :androidApp:assembleRelease`), to the
      flavor-qualified task names (`assembleProductionRelease`, `appDistributionUploadProductionRelease`).

      Verified with the dry-run check from design.md Risks — no real build or upload happens either way:
      ```sh
      ./gradlew :androidApp:packageProductionRelease --dry-run | grep checkReleaseSigning   # prints it ✓
      ./gradlew :androidApp:packageFakeRelease --dry-run | grep checkReleaseSigning          # prints it too ✓ (shared signing)
      ```
      and confirmed `appDistributionUploadFakeRelease` exists as a task but has no `appId`/`groups` configured
      (only `productFlavors.production` sets them), so it fails safely if ever invoked rather than silently
      uploading. End to end: temporarily moved `~/.android-keystores/keystore.properties` aside —
      `./gradlew :androidApp:assembleProductionRelease` failed with `checkReleaseSigning`'s exact message; restored
      the file and the same command then succeeded.

## 2. AuthRepository

- [x] 2.1 Add a commonMain `AuthRepository` interface (`AuthState` sealed interface: `Loading`, `SignedOut`,
      `SignedIn(uid, displayName, email, photoUrl)`; `observeAuthState(): Flow<AuthState>`; `suspend fun signIn():
      Result<Unit>`; `suspend fun signOut()`), taken as a new parameter of `AppGraph.Factory.create(...)` — the same
      shape as `SqlDriver`/`TextRecognizer`/`ImageCodec` (design.md decision 2), not `expect`/`actual`. Two
      concrete implementations:
      - `FirebaseAuthRepository` (Android-specific: `androidApp/src/production/kotlin/...`, wraps
        `FirebaseAuth.addAuthStateListener` and `CredentialManager`/`GetGoogleIdOption`) — task 1.1's console step
        is done and the real `google-services.json` is in place; `:androidApp:compileProductionDebugKotlin` and
        `:androidApp:assembleProductionDebug` both now succeed, confirming `R.string.default_web_client_id`
        resolves (the Google Services plugin generates it from the config's `client_type: 3` web OAuth client,
        confirmed present in the downloaded file alongside both the debug and release Android clients').
      - `UnavailableAuthRepository` (commonMain, mirrors `UnavailableTextRecognizer`'s shape: permanently
        `SignedOut`, `signIn()` always fails) — compiles on every target including `iosSimulatorArm64`.

      `FakeAuthRepository` was built ahead of schedule (task 7) since it had no external blocker — see 7.1.
      `DrivingLogApplication.kt` constructs `FirebaseAuthRepository` in its `production`-flavor source set (via a
      flavor-specific `createAuthRepository(...)` factory function, not a direct reference — `DrivingLogApplication`
      itself never names either concrete type); `MainViewController.kt` (iOS) constructs `UnavailableAuthRepository`
      directly. Verified with unit tests: `TestAuthRepository` (a hand-written test-only fake, distinct from
      `FakeAuthRepository`) covering success/cancellation/failure for `SignInProcessorTest` (task 4.1).
      `UnavailableAuthRepository` itself has no dedicated test (it's three lines with no branches) but is exercised
      transitively by `VehicleNavKeysTest`.
- [x] 2.2 Add `CurrentActivityHolder` (design.md decision 2): `DrivingLogApplication.kt` registers
      `Application.ActivityLifecycleCallbacks` to track the current resumed `Activity`, exposed to whichever
      `AuthRepository` implementation needs it at `signIn()` call time (not construction time, since the graph is
      built once at `Application` scope before any `Activity` exists, and an `Activity` can be recreated on
      rotation). `FirebaseAuthRepository` and `FakeAuthRepository` both take it as a constructor dependency.
      Exercised by `FakeAuthRepositoryTest`'s "no current activity" case; rotating the device mid-sign-in without a
      crash or stale reference is real activity-lifecycle behavior a unit test can't exercise — folded into the
      manual verification in task 8.1.

## 3. Per-account local data isolation

- [x] 3.1 Resolve the database driver's file to the signed-in account's UID instead of a single fixed filename
      (design.md decision 5): `AccountDatabaseResolver` + `ClaimedAccountStore` (commonMain, pure), backed by
      `SharedPreferencesClaimedAccountStore` (`androidApp/src/main/...`, plain, un-scoped `SharedPreferences`, not
      `app_state`). The first account ever signed in on the device gets the original file; every other account
      gets its own fresh file named from its UID. Verified with `AccountDatabaseResolverTest` (4 cases, a fresh
      in-memory fake per test case — never shared/real storage, so results can't depend on test order): no flag
      set yet → claims the original file; the same account again → reuses it; a different account → its own fresh
      file, original claim untouched; a third account → independent of the second.
- [x] 3.2 Make the database driver (and the repositories built over it in `AppGraph.kt`) re-buildable when the
      signed-in account changes within one running process, not just constructed once at process start —
      `DrivingLogApplication.graphFor(uid)` closes the previous driver and builds a fresh one when a *different*
      uid is requested, reusing the cached graph for the same uid. **Also fixed a real bug this surfaced**: the
      pictures root was account-agnostic, so `sweepPictures`/`sweepCaptures` (which delete any picture no
      *current* vehicle references) would have deleted a previous account's picture files the moment a different
      account's graph was built, even with its database file untouched — not caught by the plan, found while
      implementing it. Fixed by isolating the pictures directory the same way as the database file
      (`"pictures"` for whichever account claims the original data, `"pictures-$uid"` for every other account).
      Not unit-tested directly — `graphFor` needs a real `Context` (`DatabaseDriverFactory`, `AndroidImageCodec`,
      etc.), the same Robolectric-shaped limitation already noted for `FakeAuthRepository`'s picker outcomes;
      verified instead by `AccountDatabaseResolverTest`'s filename correctness plus the new
      `maestro/auth/account-switch.yaml` flow (task 7.2) and the manual check in task 8.1, which exercise the
      actual rebuild end to end.

## 4. The sign-in screen and the navigation gate

- [x] 4.1 Add a sign-in screen and processor (MVI/Kide, following the project's existing pattern) offering "Sign in
      with Google," showing an error on failure/cancellation without crashing (`firebase-auth`'s "Signing in uses
      Google" requirement). `SignInContract`/`SignInProcessor`/`SignInScreen` (`shared/src/commonMain/.../auth/`).
      Verified with `SignInProcessorTest` using `TestAuthRepository`: success reaches a signed-in state with no
      error; both cancellation and failure leave the screen on itself with an error message set.
- [x] 4.2 Gate `App.kt`'s navigation on `AuthState` (design.md decision 3): nothing shown while `Loading`, the
      sign-in screen for `SignedOut`, `AppNavigation` (over the account-scoped `graphFor(uid)` result) for
      `SignedIn`. `App.kt`'s signature changed to `App(authRepository, graphFor)`; `MainActivity.kt`/
      `MainViewController.kt` updated to match (iOS always resolves to the same single graph, since
      `UnavailableAuthRepository` never reaches `SignedIn`). Compiles and passes `:shared:allTests` on every
      target. The actual manual verification this task calls for (real emulator, real Google account, cold
      start/restart behavior) needs task 1.1 finished first and is folded into task 8.1 rather than duplicated
      here — the real Credential Manager handshake still can't be scripted (no test credentials to store, no way
      to drive the system account picker); task 7 covers the sign-in *journey* itself (cancel, failure, account
      switching) against the `fake` flavor's picker instead.

## 5. Account and sign-out

- [x] 5.1 Add an account action to the Home screen's top app bar showing the signed-in account; tapping it shows
      the email and a "Sign out" action, which ends the session immediately (no confirmation) and returns to the
      sign-in screen (`firebase-auth`'s "The signed-in account and sign-out are reachable from the Home screen").
      `LandingProcessor` now also observes `authRepository.observeAuthState()` for the email and dispatches
      `signOut()`; the actual return to the sign-in screen follows `AuthState` in `App.kt` automatically, the same
      pattern as reaching `SignedIn` after sign-in — no new navigation wiring needed. Verified with 4 new
      `LandingProcessorTest` cases: the signed-in email is shown; the account menu opens/closes on toggle; it
      closes on dismiss; sign-out closes the menu and reaches `AuthState.SignedOut`. Manually confirming sign-out
      returns to the sign-in screen and signing back in restores Home with data intact is folded into task 8.1.

## 6. Keeping the existing Maestro suite runnable

- [x] 6.1 Confirm `FakeAuthRepository` (task 2.1) starting already `SignedIn` means a freshly installed `fake`
      build reaches Home immediately after `launchApp: clearState: true`, with no sign-in screen shown — matching
      what every existing flow already assumes. Updated `maestro/run.sh`'s (and `CLAUDE.md`'s) documented install
      step to build and install the `fake` flavor (`./gradlew :androidApp:assembleFakeDebug`,
      `androidApp/build/outputs/apk/fake/debug/androidApp-fake-debug.apk` — not `fakeDebug/androidApp-fakeDebug.apk`,
      which both this task and `docs/distribution.md`'s own equivalent were written assuming before actually
      checking the real AGP output path) instead of today's plain `debug` variant.
- [x] 6.2 Ran the full existing suite against a freshly installed `fake` build on the `dl34` emulator:
      `vehicles` (3/3), `distance` (4/4), `appearance` (3/3) all passed unmodified on the first try. `resilience`'s
      `rotation.yaml` failed (`No visible element found: id: vehicle_color_E53935`, consistently, not flaky) —
      **confirmed pre-existing and unrelated to this change**: stashed the entire change, rebuilt the original
      single-variant `debug` APK, and the identical flow fails identically on the unmodified codebase. Not this
      change's to fix; `offline-and-restart` (the manifest's other flow) passed. No flow file needed changing.
      Confirmed order-independence (design.md decision 7) by running `vehicles` then `auth` (task 7.2) together —
      identical results to running `auth` alone.

## 7. A testing-only account picker, and Maestro coverage of the sign-in journey

- [x] 7.1 Add `TestAccountPickerActivity` (`androidApp/src/fake/kotlin/...`, design.md decision 7): a small,
      separate `ComponentActivity` (not part of the shared Kide navigation graph — commonMain stays unaware it
      exists) offering "Test Account A" (`id: test_account_a`), "Test Account B" (`id: test_account_b`),
      "Simulate failure" (`id: simulate_failure`), and "Cancel" (`id: sign_in_cancel`) — each tagged and showing
      matching visible text, since it's a plain screen, not a transient popup (avoids the `DropdownMenu`-over-testTag
      limitation this project already hit elsewhere). `FakeAuthRepository` (`androidApp/src/fake/kotlin/...`, not
      commonMain, since it now launches an Activity) starts already `SignedIn` as Test Account A by default
      (design.md decision 6, keeps the 22 existing flows untouched); `applyLaunchArguments(startUser: String?)` —
      called once from `MainActivity.onCreate`, a no-op afterward — maps `"none"`/`"B"`/else to the matching initial
      state (the `Intent` itself is read at the call site in `AuthRepositoryFactory.kt`, not inside this class, so
      the branch logic is a plain-value unit test, not one needing Robolectric for a real `Intent`). `signIn()`
      launches the picker via the classic `startActivityForResult`/`onActivityResult` (not a modern
      `ActivityResultLauncher`, which needs registering before the activity starts — too early for a call made long
      after, from an `AuthRepository` built at `Application` scope) through a new shared `ActivityResultBridge`
      (`androidApp/src/main/...`, used by both flavors) and suspends for the result.

      **Scope note**: the picker *outcomes* (`signIn()` actually reaching `TestAccountPickerActivity`) need a real
      `Activity`, which this project has no Robolectric setup for yet (`docs/test-strategy.md`'s own "not yet
      adopted" tier, deliberately deferred to its own future change) — not unit-tested directly, verified instead
      by the new `maestro/auth/` flows (task 7.2). What *is* unit-tested (`FakeAuthRepositoryTest`, 6 cases, run via
      `:androidApp:testFakeDebugUnitTest`): the default initial state, all three `applyLaunchArguments` branches,
      its once-only guard, `signOut()`, and `signIn()`'s no-current-activity failure path.
- [x] 7.2 Added `maestro/auth/` and `maestro/manifests/auth.yaml` (`sign-in`, `cancel`, `failure`,
      `account-switch`, run with `maestro/run.sh auth`) and `auth` to `CLAUDE.md`'s documented areas. All 4/4 pass,
      confirmed twice — once standalone, once interleaved as `maestro/run.sh vehicles auth` — with identical
      results, confirming order-independence (design.md decision 7).

      **Two real bugs found and fixed by actually running these on a device, not anticipated in the plan**:
      1. `App.kt`'s `testTagsAsResourceId` modifier (set once in `MainActivity`) only wrapped the `SignedIn`
         branch's `AppNavigation`, never the `SignedOut` branch's `SignInScreen` — so none of the sign-in screen's
         test tags were ever exposed as resource-ids, and Maestro couldn't find `sign_in_google` at all. Fixed by
         moving the wrapping `Box(modifier)` outside the `when`, covering every branch uniformly.
         `TestAccountPickerActivity` has the exact same problem one level further: it's a *separate* `Activity`
         with its own Compose composition root, which `MainActivity`'s modifier never reaches — fixed by setting
         `testTagsAsResourceId` again inside its own `setContent { }`.
      2. **A genuine `firebase-auth` spec violation**: cancelling and a real failure both produced the exact same
         generic error message — `firebase-auth`'s own "Cancelling the account picker" scenario requires *no*
         error shown on cancel, only on an actual failure. My own `SignInProcessorTest` didn't catch this because
         its "cancellation" case used a generic `IllegalStateException` instead of modeling cancellation as its own
         case — it was asserting the wrong expected behavior, not just missing coverage. Fixed by adding
         `SignInCancelledException` (commonMain): `FakeAuthRepository`'s cancel outcome and `FirebaseAuthRepository`'s
         catch of the real `GetCredentialCancellationException` both now produce it specifically, and
         `SignInProcessor` branches on the exception type (no error on cancel, the generic message otherwise).
         Corrected the test to assert no error on cancellation, matching the actual spec.
      3. `account-switch.yaml` itself had a bug (not the app): it asserted a vehicle's *name* directly on the Home
         screen, which never shows one (only generic tile labels, "Vehicles"/"Add vehicle"). Fixed by asserting the
         tile label for presence/absence and navigating into the vehicle list for the direct name check.
- [x] 7.3 Extend `docs/test-strategy.md`'s existing fixture-database paragraph with one more sentence (design.md
      decision 7): a flow may likewise declare its starting *auth* state via a `launchApp` argument instead of
      building it through the sign-in UI, for the same reason and in the same place a flow may start from a seeded
      fixture database. Also updated `CLAUDE.md` and `maestro/run.sh` for the `fake`-flavor install path and the
      new `auth` area. Confirmed the 22 pre-existing flows declare no `testingStartUser` argument (unchanged files).

## 8. Verification

- [ ] 8.1 Install a `productionDebug` build on a real Android emulator with Google Play Services, signed into a
      real Google account (replaces any installed `fake` build — both flavors share one `applicationId`, so only
      one can be installed at a time): confirm the real Credential Manager handshake itself still works end to
      end (the one thing task 7's fake picker can't stand in for) — cold start → sign in for real → reaches Home
      → a full restart afterward reaches Home directly with no prompt. Also confirm every existing capability
      works normally and entirely offline once signed in, and that a restart with no network still reaches Home
      directly.

      **Done**: built and installed fresh on `dl34` — confirms `assembleProductionDebug` succeeds and a clean
      install launches directly to the sign-in screen (`app-shell`'s modified requirement), matching the `fake`
      flavor's behavior exactly except for what's under the one button.

      **Left undone, deliberately, with explicit sign-off**: the real Credential Manager handshake itself. First
      found to need a Google account on the device (`dl34` had none — `adb shell dumpsys account` showed
      `Accounts: 0`); investigating further (confirmed real network connectivity works, including to Google's own
      servers — this was never actually a connectivity problem despite the app's own error message blaming one)
      found the deeper cause: `dl34` is a **Google APIs** system image (`tag.id=google_apis`,
      `PlayStore.enabled=no`, `~/.android/avd/dl34.avd/config.ini`), not a **Google Play** image — it bundles GMS
      libraries to compile and link against, but has no genuine, sign-in-capable Play Store/account stack. This is
      exactly the risk design.md's Risks section already named ("a system image without [Play Services]... cannot
      exercise the real sign-in flow"), now confirmed in practice rather than hypothetical. Asked the user how to
      proceed (create a new Play Store–enabled AVD, test on a real device, or accept the gap) — chose to archive
      without it: every other code path `FirebaseAuthRepository`/the sign-in screen/`App.kt`'s gate touches is
      already covered (unit tests, the full Maestro suite including the new `auth` flows against the equivalent
      `fake` code paths), so the only genuinely unverified piece is the real Credential Manager call itself,
      accepted as a known gap rather than blocking this change on acquiring a Play Store–capable test device.

      **Correction found while building this**: the claim that this change adds no new system permission was only
      ever true for the `fake` flavor (confirmed — `maestro/check-permissions.sh` passes against it with zero
      change). `production` genuinely gains permissions: `INTERNET` (re-added deliberately — the base manifest
      strips it for ML Kit's on-device guarantee, but real network-based sign-in needs it, see design.md Risks),
      plus `USE_BIOMETRIC`/`USE_FINGERPRINT`/`READ_GSERVICES`, pulled in transitively by Credential Manager/Play
      Services Auth's own manifests, not declared directly. `maestro/check-permissions.sh` only ever inspects
      whatever's installed as `com.mikonoma.drivinglog` — reinstalling `production` over it and re-running it would
      just re-check the same package under a different build, not add real coverage; skip it here, it's already
      covered by task 6.2 having passed against `fake`.
- [x] 8.2 Ran `./gradlew :shared:allTests :androidApp:assembleDebug` (builds both `productionDebug` and
      `fakeDebug`) and `openspec validate --all --strict`; both pass (25/25 specs, 0 failed).
- [x] 8.3 Re-ran task 1.4's dry-run distribution check: `checkReleaseSigning` appears (`SKIPPED`, meaning wired into
      the task graph — Gradle marks a dry-run task `SKIPPED` rather than executing it, which is what confirms the
      dependency exists without actually running it) for both `packageProductionRelease` and `packageFakeRelease`
      — unchanged since task 1.4, confirming nothing later in this change's implementation reopened that gap.
