## MODIFIED Requirements

### Requirement: Signing in uses Google, through the system's own account picker
The system SHALL sign the user in with their Google account through Android's Credential Manager API (not a
browser-based or embedded web view flow), which shows the device's own account picker. A successful sign-in SHALL
proceed to the Home screen. Signing in SHALL require a network connection. Dismissing the picker without choosing an
account SHALL leave the user on the sign-in screen with no error; any other failure SHALL show an error, without
crashing, on the sign-in screen.

#### Scenario: Successful sign-in reaches the Home screen
- **WHEN** the user taps "Sign in with Google" and completes it by choosing an account
- **THEN** the Home screen is shown

#### Scenario: Cancelling the account picker leaves the user on the sign-in screen
- **WHEN** the user taps "Sign in with Google" and dismisses the account picker without choosing one
- **THEN** the sign-in screen is shown again, with no error

#### Scenario: A sign-in failure shows an error, not a crash
- **WHEN** signing in fails (for example, no network connection is available)
- **THEN** the sign-in screen shows an error and remains usable; the app does not crash
