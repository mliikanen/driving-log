# Spec Delta

## MODIFIED Requirements

### Requirement: Application launches to the Home screen
The system SHALL start on a sign-in screen (`firebase-auth`, "A sign-in screen gates the rest of the app") when the
user is not signed in, and on the Home screen, without requiring network access or any other setup, once they are.

#### Scenario: Cold start on Android
- **WHEN** the user launches the app from the Android launcher, already signed in
- **THEN** the Home screen is displayed

#### Scenario: Launch without network
- **WHEN** the user launches the app, already signed in, while the device has no network connection
- **THEN** the Home screen is displayed and no error is shown
