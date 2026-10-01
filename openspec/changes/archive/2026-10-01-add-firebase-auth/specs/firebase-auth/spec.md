# Spec Delta

## Purpose

Gates the application behind a Google account: the user signs in once, the session persists across restarts, and
every other capability keeps working exactly as it does today, entirely offline, once signed in.

## ADDED Requirements

### Requirement: A sign-in screen gates the rest of the app
The system SHALL show a sign-in screen instead of the Home screen (or any other screen) whenever the user is not
signed in, and SHALL NOT let them reach any other functionality until they are. The sign-in screen SHALL offer one
action: "Sign in with Google."

#### Scenario: Not signed in shows the sign-in screen
- **WHEN** the user opens the app and is not signed in
- **THEN** the sign-in screen is shown, offering "Sign in with Google," and no other screen is reachable

#### Scenario: Signed in skips the sign-in screen
- **WHEN** the user opens the app and is already signed in
- **THEN** the Home screen is shown directly; the sign-in screen is not shown

### Requirement: Signing in uses Google, through the system's own account picker
The system SHALL sign the user in with their Google account using Android's Credential Manager API (not a
browser-based or embedded web view flow), which shows the device's own account picker. A successful sign-in SHALL
proceed to the Home screen. The system SHALL require a network connection to sign in. Dismissing the account picker
without choosing an account SHALL leave the user on the sign-in screen with no error; any other sign-in failure
SHALL show an error, without crashing, leaving the user on the sign-in screen.

#### Scenario: Successful sign-in reaches the Home screen
- **WHEN** the user taps "Sign in with Google" and completes it by choosing an account
- **THEN** the Home screen is shown

#### Scenario: Cancelling the account picker leaves the user on the sign-in screen
- **WHEN** the user taps "Sign in with Google" and dismisses the account picker without choosing one
- **THEN** the sign-in screen is shown again, with no error

#### Scenario: A sign-in failure shows an error, not a crash
- **WHEN** signing in fails (for example, no network connection is available)
- **THEN** the sign-in screen shows an error and remains usable; the app does not crash

### Requirement: The signed-in session persists across restarts
Once signed in, the system SHALL remember the session so the user is not asked to sign in again after closing and
reopening the app, until they sign out. The Home screen and every other screen SHALL be reachable without a network
connection once signed in, the same as before this capability existed.

#### Scenario: The session survives a restart
- **WHEN** the user has signed in, closes the app completely and opens it again
- **THEN** the Home screen is shown directly, with no sign-in prompt

#### Scenario: Offline use after sign-in
- **WHEN** the user is signed in and the device has no network connection
- **THEN** the app is fully usable, the same as it was before this capability existed

### Requirement: On-device data is isolated per signed-in account
The system SHALL keep on-device data (vehicles, events, photos) separate per signed-in Google account, so that a
different account signed in on the same device never sees, overwrites, or loses another account's data. Switching
to a previously-used account SHALL restore exactly the data it had, unaffected by any other account's use in
between. The first account to ever sign in on a device SHALL claim whatever local data already exists there,
including data created before this capability existed.

#### Scenario: The first account to sign in claims existing local data
- **WHEN** the first Google account ever signs in on a device that already has local data (e.g. an upgrade from
  before this capability existed)
- **THEN** that data remains visible, now belonging to the signed-in account

#### Scenario: A second account starts with none of the first account's data
- **WHEN** a different Google account signs in on a device that already has data belonging to another account
- **THEN** the new account sees no vehicles, events, or photos — none of the other account's data

#### Scenario: Switching back to a previous account restores its data
- **WHEN** the user signs out, a different account signs in and is later signed out, and the original account signs
  back in
- **THEN** all of the original account's data is exactly as it was left, untouched by the other account's use

### Requirement: The signed-in account and sign-out are reachable from the Home screen
The system SHALL show an account action on the Home screen's top app bar, showing the signed-in account (its name,
email, or picture). Tapping it SHALL show the account's email and a "Sign out" action. Signing out SHALL end the
session immediately, with no confirmation dialog, and SHALL return to the sign-in screen.

#### Scenario: The account action is shown
- **WHEN** the user is signed in and views the Home screen
- **THEN** the top app bar shows an account action for the signed-in account

#### Scenario: Signing out returns to the sign-in screen
- **WHEN** the user taps the account action, then "Sign out"
- **THEN** the session ends immediately and the sign-in screen is shown

#### Scenario: Signing back in after signing out
- **WHEN** the user has signed out and signs in again with the same Google account
- **THEN** the Home screen is shown, the same as any other successful sign-in
