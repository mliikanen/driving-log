# Proposal

## Why

The project has anticipated user accounts from the start: the tech stack names "Firebase for user accounts and
data storage," and the offline-first requirement is explicitly phrased as "after the **initial login**, the app
MUST remain fully usable without a network connection" — a login step has always been assumed to exist. Sharing a
vehicle with another user by invite (also already in the project context) needs a real, identifiable account, not
an anonymous one. This is the first of two changes: this one adds sign-in itself; a separate, later change
(`add-firebase-sync`) adds the remote data store (Firestore) and syncs the vehicle log to it. Splitting them keeps
each independently reviewable, matching how every other change in this project has stayed narrowly scoped.

## What Changes

- The app requires the user to sign in with a Google account before reaching the Home screen or any other
  functionality. There is no anonymous or offline-before-sign-in mode: `app-shell`'s "Application launches to the
  Home screen" requirement, which today explicitly promises a Home screen "without requiring login, network access
  or any other setup," is replaced by a requirement that launches to a sign-in screen when signed out, and to the
  Home screen once signed in.
- A sign-in screen offers a single "Sign in with Google" action (Android's Credential Manager API, not the older,
  deprecated `GoogleSignInClient`). A successful sign-in persists the session (the user stays signed in across app
  restarts, until they sign out) and proceeds to the Home screen. The app SHALL show the signed-in account (name,
  email, or avatar) and a "Sign out" action somewhere reachable — see the open design question below for exactly
  where, since no settings/account screen exists yet (`add-settings-screen` is filed but not built).
- Once signed in, existing functionality is otherwise unaffected by this change: no remote data storage, no syncing.
  Every existing capability (vehicles, distance/refueling logging, pictures, etc.) keeps working exactly as it does
  today, entirely against the local SQLite database, with no network dependency beyond the one-time sign-in itself.
- **On-device data is isolated per signed-in Google account, never shared or lost across an account switch.** Each
  account's local data (vehicles, events, photos) lives separately on the device. Signing in as a different account
  never sees, overwrites, or deletes another account's data — it's just not visible while that other account isn't
  the one signed in. Switching back to a previously-used account restores exactly what it had, with nothing lost.
  The first account to ever sign in on a device claims whatever local data already exists there — including data
  created before this capability existed, so this also serves as the upgrade story for existing installs. This is
  achieved purely locally (one isolated local database per account on the device), so it belongs in this change
  rather than `add-firebase-sync`, which will later add real per-account cloud storage layered on top of it.
- **This update is explicitly allowed to be destructive only in the narrow upgrade sense above**: existing
  pre-authentication local data is claimed by whichever account signs in first on the device, with no choice or
  preservation step offered. Switching between accounts afterward is never destructive — see the isolation
  guarantee above.

## Capabilities

### New Capabilities
- `firebase-auth`: Google Sign-In, session persistence, sign-out, the sign-in screen that gates the rest of the app,
  and isolating on-device data per signed-in account.

### Modified Capabilities
- `app-shell`: "Application launches to the Home screen" no longer promises a Home screen without login; it now
  launches to the sign-in screen when signed out.

## Impact

- New dependency: Firebase Authentication (`com.google.firebase:firebase-auth`) and Credential Manager
  (`androidx.credentials`, `com.google.android.libraries.identity.googleid`) on Android. iOS stays deferred, per the
  project's existing "opportunistic" iOS policy — the same platform-note pattern already used for the live scanner
  and other Android-only-for-now capabilities.
- **One-time setup, like `docs/distribution.md`'s own precedent**: enabling the Google sign-in provider in the
  Firebase console for the existing project (`driving-log-49c48`), registering the Android app's OAuth client
  (needs its release *and* debug signing certificates' SHA-1 fingerprints), and downloading `google-services.json`
  into `androidApp/` — none of which exists in the repository today. This is a real prerequisite, not something
  code alone can produce; task 1 covers it explicitly, mirroring how `docs/distribution.md` documents Firebase App
  Distribution's own one-time setup.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/App.kt`: the root composable gates `AppNavigation` behind
  a signed-in check instead of always starting at `LandingNavKey`.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/di/AppGraph.kt`: a new `AuthRepository` dependency, taken as
  a `Factory` parameter and constructed by each platform shell, the same pattern `SqlDriver`/`TextRecognizer`/
  `ImageCodec` already use — not `expect`/`actual`.
- The local database driver moves from one fixed file to one file per signed-in account's UID, resolved only once
  an account is known and re-resolved whenever the signed-in account changes within the same app session (sign out,
  then a different account signs in, without the process restarting) — see design.md for how this interacts with
  the app's dependency graph, which today builds its repositories unconditionally at process start.
- A new sign-in screen and processor, following this project's existing MVI (Kide) pattern.
- **`androidApp` gains two product flavors, `production` and `fake`** (design.md decision 6), so the Maestro
  suite never needs a real Google account: the `fake` flavor's `AuthRepository` is a fake that starts already
  signed in, with zero Firebase/Credential Manager dependency at all. None of the 22 existing Maestro flows need to
  change — they just run against the `fakeDebug` build variant instead of today's plain `debug`.
- **The `fake` flavor also simulates the sign-in journey itself**, not just starting pre-signed-in (design.md
  decision 7): a small testing-only screen stands in for the real Google account picker, so a new `maestro/auth/`
  area can script the initial sign-in, cancelling it, a sign-in failure, and the account-switch isolation guarantee
  — all previously manual-only, since the real Credential Manager flow can't be scripted.
- The existing release-signing guard and `scripts/distribute.sh` need updating for flavor-qualified Gradle task
  names (`packageProductionRelease` etc. instead of `packageRelease`) — see design.md's Risks.

**Explicitly out of scope, filed or named as separate follow-ups:**
- The remote data store itself (Firestore), and syncing any local data to or from it — `add-firebase-sync`.
- Vehicle sharing/invites (needs the remote store to exist first).
- Any sign-in method besides Google (email/password, Apple, etc.).
- Preserving or migrating pre-existing local-only data past the single first-sign-in claim described above (e.g. no
  UI to let a user manually merge two accounts' local data).
- A full settings/account screen — `add-settings-screen` is already filed as its own stub; this proposal only adds
  the minimum "Sign out" affordance it needs, without building that screen.
