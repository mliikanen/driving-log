# Design

## Context

`App.kt` today starts `rememberAppNavBackStack` unconditionally at `LandingNavKey(graph)` — there is no gate, no
auth state, no network dependency anywhere in the app's own code. `AppGraph.kt` (Metro DI, `@DependencyGraph(AppScope::class)`)
exposes every repository/processor as a graph property; its `@DependencyGraph.Factory` takes the handful of
genuinely platform-specific pieces as plain parameters (`SqlDriver`, `DeviceLocale`, `ImageCodec`, `TextRecognizer`),
each constructed by whichever platform shell calls it — `DrivingLogApplication.kt` on Android, `MainViewController.kt`
on iOS — and passed straight in. The graph itself never asks which platform it's on; it only knows the interfaces.
That's a different, more specific precedent than `expect`/`actual`, which this project reserves for a lower,
point-of-use level — something a composable reaches for directly rather than something injected through the graph
(`LiveCameraPreview`/`rememberCameraPermission`, `add-live-scanner`: an Android implementation backed by real
platform APIs, an iOS one deferred until the Xcode project exists). `AuthRepository` is graph-scoped like
`TextRecognizer`/`ImageCodec` — every processor and screen that needs it gets it through `AppGraph`, never by
calling a composable directly — so it follows their injected-parameter shape, not `LiveCamera`'s `expect`/`actual`
one. No Firebase SDK beyond App Distribution's Gradle plugin is wired into the build; there is no
`google-services.json` in the repository.

See `proposal.md` for the full behavioral scope; this covers how it's built.

## Goals / Non-Goals

**Goals:**
- Gate the app behind Google Sign-In using Android's current, non-deprecated Credential Manager API — not the
  older `GoogleSignInClient`, which Google has deprecated in favor of it.
- Let the Firebase Auth SDK own session persistence itself (it already persists across process restarts natively)
  rather than building custom persistence.
- Touch nothing about how any existing capability stores or reads data — this change adds a gate in front of the
  app, not a new dependency inside any existing repository.

**Non-Goals:** everything `proposal.md`'s "Explicitly out of scope" section lists (Firestore/remote sync, vehicle
sharing, non-Google sign-in methods, data migration for pre-existing local installs, a full settings screen) — not
restated here.

## Decisions

**1. Credential Manager (`androidx.credentials` + Google's `googleid` library), not `GoogleSignInClient`.**
Google has deprecated `GoogleSignInClient`/`GoogleSignInApi` in favor of Credential Manager, which is also the
mechanism that supports passkeys and other credential types going forward. Building on the deprecated API now would
mean redoing this shortly after shipping it.

**2. `AuthRepository` is a commonMain interface with concrete implementations constructed by each platform shell and
passed into `AppGraph.Factory.create(...)`, following the `TextRecognizer`/`ImageCodec`/`SqlDriver` precedent — not
`expect`/`actual`.** It exposes an observable `Flow<AuthState>` (`Loading`, `SignedOut`,
`SignedIn(uid, displayName, email, photoUrl)`) plus `suspend fun signIn(): Result<Unit>` and `suspend fun signOut()`.
On Android, `DrivingLogApplication.kt` constructs the concrete implementation — which one depends on the product
flavor (decision 6): `FirebaseAuthRepository` (`androidApp/src/production/…`, wrapping
`FirebaseAuth.addAuthStateListener` for the Flow and `CredentialManager`/`GetGoogleIdOption` for `signIn()`) in the
`production` flavor, `FakeAuthRepository` (`androidApp/src/fake/…`, decision 7) in the `fake` flavor. On iOS,
`MainViewController.kt` constructs a permanent stub (`UnavailableAuthRepository`, mirroring
`UnavailableTextRecognizer`'s existing shape: always `SignedOut`, `signIn()` failing) directly — this one genuinely
has no platform API calls, so it lives in `commonMain`. Nothing about this proposal builds or tests iOS.

**Both Android implementations need a current `Activity` at call time, not construction time** — whether to launch
the real Credential Manager picker or (decision 7) a fake one, `signIn()` needs somewhere to show UI and get a
result back. But `AppGraph`/`AuthRepository` are constructed once, at `Application` scope, in
`DrivingLogApplication.kt`'s `by lazy`, before any `Activity` exists at all, and an `Activity` can be recreated
(rotation) during the app's lifetime — so a single `Activity` reference captured at construction would go stale.
`DrivingLogApplication.kt` instead registers `Application.ActivityLifecycleCallbacks` to track the current resumed
`Activity` in a small shared `CurrentActivityHolder`, passed to whichever `AuthRepository` implementation is
constructed; both implementations read it at the moment `signIn()` is actually called, not before. This was already
a latent gap in `FirebaseAuthRepository`'s own design before decision 7 made it unavoidable to address now.

**3. `App.kt` observes `AuthState` before deciding what to show, with an explicit `Loading` state.** Firebase Auth's
own listener fires asynchronously — there is a real window, however brief, before the first callback where whether
the user is signed in is genuinely unknown. `App.kt` shows nothing (the same plain background the launch window
already uses, per `app-shell`'s "Material Design 3 theming") during `Loading`, the sign-in screen for `SignedOut`,
and mounts `AppNavigation` at `LandingNavKey` for `SignedIn` — never starting the back stack before the state is
known, and never showing a flash of the sign-in screen for an already-signed-in user.

**Found via a real Maestro run, not anticipated here**: `MainActivity`'s `Modifier.semantics { testTagsAsResourceId
= true }` must wrap the `when` block as a whole, not just the `SignedIn` branch's `AppNavigation` — otherwise it
never reaches the `SignedOut` branch's sign-in screen, and none of that screen's test tags are exposed as
resource-ids at all. The same applies one level further to `TestAccountPickerActivity` (decision 7): a *separate*
`Activity` has its own Compose composition root, so it needs the same modifier set again inside its own
`setContent { }` — `MainActivity`'s doesn't reach it either.

**`signIn()`'s `Result<Unit>` needs a way to distinguish cancellation from every other failure, or the spec can't
actually be met.** `firebase-auth`'s own scenarios require cancelling the account picker to leave *no* error on
screen, while any other failure shows one — two different outcomes from the same `Result.failure(...)` return.
(An earlier draft of this requirement's own prose text said the opposite — "show an error ... when sign-in fails or
is cancelled" — contradicting its own scenario below it; the scenario is what was actually implemented and
verified, so the prose was corrected to match it, not the other way around.) A dedicated
`SignInCancelledException` (commonMain) carries this: `FakeAuthRepository`'s cancel outcome and
`FirebaseAuthRepository`'s catch of Credential Manager's own `GetCredentialCancellationException` both produce it
specifically, and `SignInProcessor` branches on the exception *type*, not a message string, to decide whether to
show an error at all.

**4. The account action lives directly on the Home screen's top app bar, not behind a new settings screen.**
`add-settings-screen` is filed but not built, and design-blocked on its own open questions (where it's reached
from). Building a whole settings screen just to hold "Sign out" would be scope creep this proposal doesn't need;
a single account icon showing a small menu (email + "Sign out") is the minimum this capability actually requires,
and doesn't preclude `add-settings-screen` from later absorbing it.

**5. On-device data is isolated per account using one local SQLite database file per signed-in account, not a
single shared database with a wipe-on-switch or an `account_uid` column threaded through every table.** The
existing single database file becomes whichever account signs in first's file; each subsequent new account gets its
own fresh file (e.g. `driving-log-<uid>.db` alongside today's fixed-name file). The database driver is opened
against whichever file matches the currently `SignedIn` account's UID; switching to a previously-used account
reopens its own file untouched, so nothing is ever merged, filtered, or deleted — each account's SQLite file is
simply not the one currently open. This needs no schema change (no new column on every table, no query rewriting)
and gives genuine isolation at the file level. One small piece of state has to live *outside* any account-scoped
database, since it must be readable before any account's file is chosen: a device-level flag (Android's own
un-scoped `SharedPreferences`, not `app_state`, since `app_state` lives inside an account-scoped file) recording
whether the device's original, un-scoped local data has already been claimed by an account, so a second account
signing in later doesn't re-claim it.

This has a real consequence for `AppGraph.kt`: today it builds its dependency graph, including the database driver
and every repository over it, unconditionally at process start. Once the driver's file depends on knowing which
account is signed in, that construction can no longer happen unconditionally before `AuthState` is known — and it
has to be able to happen *again*, re-pointed at a different file, when the user signs out and a different account
signs in within the same running process (not just across a process restart). This is the one piece of this change
that isn't simply "add a repository next to the existing ones" — see task 3 for how it's sequenced.

**Found while implementing this, not anticipated in the original plan: the pictures root needs the exact same
isolation as the database file, or the isolation guarantee breaks anyway.** `sweepPictures`/`sweepCaptures` run on
every graph build and delete any picture file no *current* vehicle references. If every account shared one
`pictures/` directory, switching to a different account would immediately sweep away the *previous* account's
picture files — the database row survives, but the photo it pointed at doesn't, which is exactly the kind of data
loss decision 5 exists to prevent. Fixed the same way as the database file: whichever account claims the device's
original data gets the original `pictures/` directory, every other account gets its own (`pictures-<uid>/`).

**6. A `production`/`fake` Android product-flavor split supplies the fake `AuthRepository`, rather than a
debug-only UI bypass, a real-account Maestro flow, or an `adb`-seeded fixture.** Every existing flow (22 files
across `vehicles`, `distance`, `resilience`, `appearance`, `picture`, `theme`, `clock`) opens with
`launchApp: clearState: true` and immediately expects Home — once this change ships, that's no longer the first
screen anywhere, and nothing in the current suite accounts for it. Three alternatives were considered and rejected:
scripting the real Google sign-in (no test credentials to store, no way to drive the system account picker);
seeding a signed-in session directly via `adb`, the way `docs/test-fixtures.md`'s database fixtures are seeded
(rejected because a signed-in session lives inside Firebase Auth's own SDK-managed, undocumented local storage,
unlike the app's own SQLite schema, which is ours to write into); and a `BuildConfig.DEBUG`-guarded second button on
the sign-in screen (rejected once the product-flavor approach was on the table, since it adds a permanent, if
guarded, test-only surface to the real screen instead of keeping the difference entirely at the dependency level).

Instead, `androidApp/build.gradle.kts` gains two product flavors: `production` (the real app — Firebase Auth,
Credential Manager, the Firebase console setup from task 1) and `fake` (everything Maestro needs, with zero
external dependencies). Each flavor has its own small source set (`androidApp/src/production/…`,
`androidApp/src/fake/…`) providing the concrete `AuthRepository` `DrivingLogApplication.kt` constructs (decision
2); the Firebase Auth/Credential Manager dependencies move to the `production` flavor's own dependency
configuration, so the `fake` flavor never links the Firebase SDK, never needs `google-services.json`, and never
needs Play Services on the test device at all. Both flavors show the exact same sign-in screen with the exact same
"Sign in with Google" tap target — only what that tap does underneath differs. `FakeAuthRepository` (decision 7)
starts already `SignedIn` with a fixed fake identity rather than `SignedOut`: a fresh install of the `fake`
flavor reaches Home immediately, exactly like every existing flow already assumes, so **none of the 22 existing
flow files need to change at all** — they just run against the `fakeDebug` build variant instead of today's
plain `debug`. A fresh install simply doesn't start in the `SignedOut` state the way `production` does; signing out
still reaches it from there like any other transition. Because every flow's `launchApp: clearState: true` wipes
local state before each run, the fixed test identity reliably claims the device's original database file each time,
composing cleanly with the per-account isolation in Decision 5 without leaving stale state between runs.

This is also the shape a future `add-firebase-sync` should reuse for its own Firestore-backed store: a `production`
implementation talking to real Firestore, a `fake` one backed by an in-memory fake, wired the same way. Not built
here, but worth that proposal's own design.md citing this one when it's written.

**7. `FakeAuthRepository.signIn()` shows its own small, testing-only account-picker screen and suspends for the
result, rather than resolving synchronously to one fixed identity.** The default (decision 6) covers every existing
flow's needs — start already signed in, never touch the gate at all — but says nothing about testing the gate
*itself*: the initial sign-in journey, cancelling it, a sign-in failure, and the account-switch/isolation guarantee
(decision 5) were all manual-only until now (tasks 4.2, 7.1), because scripting the real Credential Manager picker
was already ruled out. A `fake`-only screen doesn't have that problem — it's our own UI, not a system surface,
so Maestro can drive it exactly like any other screen in this app (a plain navigated screen, not a transient popup,
avoiding the `DropdownMenu`-over-testTag limitation this project already hit elsewhere).

Concretely: `TestAccountPickerActivity` (`androidApp/src/fake/…`, a small separate `ComponentActivity`, not part
of the shared Kide navigation graph — commonMain stays entirely unaware this screen exists) offers two fixed fake
identities ("Test Account A" / uid `maestro-test-user-a`, "Test Account B" / uid `maestro-test-user-b`), a "Cancel"
action, and a "Simulate failure" action — covering the `firebase-auth` spec's cancellation and failure scenarios
with real Maestro coverage too, not just the success path. `FakeAuthRepository.signIn()` launches it through the
classic `startActivityForResult`/`onActivityResult` (not a modern `ActivityResultLauncher`, which must be
registered before the activity reaches `STARTED` — too early for a call made long after, from an `AuthRepository`
built at `Application` scope), via a small shared `ActivityResultBridge` (`androidApp/src/main/…`, used by both
flavors so `MainActivity`'s override compiles either way) and suspends until it returns, translating the result
into an `AuthState` transition — the same "launch something, suspend until it resolves" shape
`FirebaseAuthRepository`'s real Credential Manager call already has, just backed by our own screen instead of a
system one. `signIn()`'s contract (suspend until picked or cancelled) is identical in both flavors; only what shows
the UI differs, entirely inside each implementation.

**A flow's starting auth state is a named value declared as test setup, the same way `docs/test-fixtures.md`'s
seeded database fixtures already work — not something built by tapping through real UI first, and not a bare
boolean either.** That doc's own rule: "a flow whose subject is how already-stored data renders (not the UI path
that builds it) may instead start from a fixture database... rather than building that state through the UI."
The same shape applies here: a flow whose subject is the sign-in screen or a particular account's starting state
(not the sign-in UI path that reaches it) may instead launch already in that state, declared up front.

Concretely, Maestro's `launchApp` already supports passing Android Intent extras (`arguments: { ... }`), so a flow
declares `testingStartUser: "none" | "A" | "B"` (`FakeAuthRepository`, `fake` flavor only — the extra doesn't
exist in `production`) — a named value, not a flag, so a future third identity is just another value, not a new
mechanism. `FakeAuthRepository` reads it once, at construction, to pick its *initial* `AuthState`: absent or `"A"`
signed in as Test Account A (decision 6's default, so the 22 existing flows stay untouched — they declare nothing
and get exactly today's behavior), `"none"` signed out, `"B"` signed in as Test Account B. `sign-in.yaml`,
`cancel.yaml`, and `failure.yaml` each declare `testingStartUser: "none"` and land directly on the sign-in screen —
no scaffolding "first sign out" step. `account-switch.yaml` declares `testingStartUser: "A"` explicitly — even
though that matches the default — the same way a fixture-seeded flow always names its fixture rather than relying
on an implicit one; its actual subject is the sign-out/sign-in *transitions themselves* (A → B → A), performed as
real UI actions because that's literally what's under test, not because it needed to reach a starting point.

**This keeps every flow's outcome independent of execution order**, which matters here specifically because the new
mechanism is stateful (decision 5's per-account SQLite files plus the `SharedPreferences` "claimed" flag) in a way
none of the existing flows were. Two things guarantee this, deliberately, not by accident:
- `launchApp: clearState: true` (already true of every flow, existing and new) wipes all of that per-flow state —
  every account's database file and the claim flag — so no flow can observe what an earlier flow left behind,
  regardless of what ran before it or in what order.
- The starting `AuthState` itself comes from an explicit argument on *that* flow's own `launchApp` call, not from
  whatever state a previous flow happened to leave the (now-wiped) app in. A flow's behavior is a pure function of
  its own declared setup, never of run order.

The same care applies one level down, to task 3.1's own unit tests of the per-account database file resolution
(the `SharedPreferences` flag plus per-UID files): they must use a fresh, isolated fake (an in-memory map or a
per-test temp directory), never a real, shared `SharedPreferences`/filesystem location reused across test cases —
that would silently reintroduce the same order-dependence this decision avoids at the Maestro level, just one layer
lower, where it's easier to overlook.

Task 7 adds this to `docs/test-strategy.md` as one more sentence on the existing fixture-database paragraph, not a
new standalone rule: a flow may likewise declare its starting *auth* state (signed out, or signed in as a specific
test account) via a `launchApp` argument, instead of building it through the sign-in UI — for the same reason and in
the same place a flow may start from a seeded fixture database. Every flow that doesn't declare one gets today's
default and touches none of this machinery at all, exactly as an ordinary flow with no fixture argument gets an
empty database and builds whatever state it needs through the UI.

This doesn't replace the manual,
real-Google-account verification in tasks 4.2/7.1 — Credential Manager itself is still unscripted — but it moves
everything *except* "does the real system picker work" out of manual-only territory.

## Risks / Trade-offs

- **Blocked on Firebase console setup and `google-services.json`, neither of which exists yet.** → Task 1 is
  explicitly "before any code," mirroring `update-app-icon`'s own precedent for a change blocked on an external
  asset the developer must supply. Unlike that stub, the rest of this proposal's design does not depend on the
  exact credentials, so it is not filed as a stub itself — only task 1 is gated on it.
- **Credential Manager's Google ID flow needs Google Play Services on the test device/emulator.** → **Confirmed in
  practice, not just a risk**: `dl34` (this project's own Maestro AVD) is a `google_apis` image, not
  `google_apis_playstore` (`PlayStore.enabled=no` in its `config.ini`) — it has GMS libraries to link against but
  no genuine, sign-in-capable account stack, so the real handshake can't be exercised on it at all. Real network
  connectivity on the device was independently confirmed working (including to Google's own servers); the
  limitation is specifically the image's lack of Play Store provisioning, not connectivity, despite the app's own
  generic error message pointing at "check your connection." Accepted as a known gap for this change (task 8.1) —
  `dl34` is also what the `fake` flavor's Maestro suite depends on, so replacing it isn't free, and the real
  Credential Manager call is the one piece of this change's surface no other test here exercises.
- **This update gates existing test installs behind sign-in, and the first account to sign in silently claims
  whatever local data already exists** (proposal.md's narrowly-scoped destructive transition). → No design
  mitigation needed — this was a deliberate scope decision, not an oversight; worth calling out in the release notes
  when this ships so testers aren't surprised, especially anyone who might sign in with a different account than
  they expect on their existing test device.
- **The database driver can no longer be built unconditionally at graph-construction time**, and must be rebuildable
  mid-process when the signed-in account changes. → This is the main implementation risk in this proposal (see
  Decision 5); it needs care to avoid leaking or double-opening SQLite connections when the account changes within
  one running process, and needs its own test coverage (task 3), not just manual verification.
- **Multiple per-account database files accumulate on a shared test device** over repeated account switches during
  development. → Acceptable; this is a test-device-only concern (no real user is expected to switch Google accounts
  routinely), and nothing in this proposal needs to clean up an account's file after they stop using a device.
- **Introducing product flavors silently breaks the existing release-signing guard.** `androidApp/build.gradle.kts`'s
  `checkReleaseSigning` task matches the literal task names `"packageRelease"`/`"packageReleaseBundle"`
  (`add-app-distribution`'s own safety check that a release can't be built unsigned). Once flavors exist, those
  tasks are renamed `packageProductionRelease`/`packageProductionReleaseBundle` (and their `fake` counterparts),
  so the existing `tasks.matching { it.name == "packageRelease" }` predicate would silently stop matching anything —
  the guard would still exist but would protect nothing. → Task 1 updates that predicate to the flavor-qualified
  names. `signingConfig` stays on `buildTypes.release` (shared by both flavors deliberately — a `fakeRelease` APK
  is just as unsigned without a real keystore as `productionRelease` would be, so `checkReleaseSigning` correctly
  guards *both* flavors' release package tasks, not just `production`'s). What *does* need scoping to `production`
  specifically is `firebaseAppDistribution { ... }` itself: moved into `productFlavors.production { ... }` rather
  than `buildTypes.release`, so `fakeRelease`'s own `appDistributionUploadFakeRelease` task exists (the plugin
  creates one per variant regardless) but never receives an `appId`/`groups` — it fails loudly if ever invoked,
  rather than silently uploading a fake-auth build to the real tester group. `scripts/distribute.sh`'s own
  `:androidApp:assembleRelease :androidApp:appDistributionUploadRelease` invocation, and `docs/distribution.md`'s
  own `./gradlew :androidApp:assembleRelease` reference, both need the same flavor-qualified update regardless —
  an unqualified `assembleRelease` would still build *both* flavors' release APKs even though only one uploads.

  **Verifying this stays correct** uses Gradle's own `--dry-run` (prints the resolved task graph without running
  anything, so it's a repeatable, scriptable check, not a one-off manual read) rather than a new Gradle TestKit test:
  `./gradlew :androidApp:packageProductionRelease --dry-run` and `./gradlew :androidApp:packageFakeRelease --dry-run`
  must both list `checkReleaseSigning` in their task graph (confirmed: both do). A TestKit-based regression test
  asserting this permanently would be more robust against a *future* refactor of the flavors silently reopening this
  gap, but is new test infrastructure this project has no precedent for, for one guard condition — not worth it;
  worth reconsidering if this class of bug recurs. The dry-run check is instead run twice: once while building task
  1.4, and once more as part of this change's own final regression (task 7), so it isn't a check that only happens
  once and bitrots.

## Migration Plan

Purely additive: a new SDK dependency, a new screen, a gate in `App.kt`. No schema migration, no data touched. If
this needs reverting, reverting the commit is sufficient — nothing here makes an irreversible change to stored data.
