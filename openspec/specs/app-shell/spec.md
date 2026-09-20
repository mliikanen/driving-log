# app-shell Specification

## Purpose

Defines the baseline behavior of the Driving Log application shell: how it launches, how it looks, and what the
user sees before any feature exists. Later features add screens and behavior on top of this baseline.

## Requirements

### Requirement: Application launches to the Home screen
The system SHALL start on a Home screen when the application is opened, without requiring login, network
access or any other setup.

#### Scenario: Cold start on Android
- **WHEN** the user launches the app from the Android launcher
- **THEN** the Home screen is displayed

#### Scenario: Launch without network
- **WHEN** the user launches the app while the device has no network connection
- **THEN** the Home screen is displayed and no error is shown

### Requirement: Home screen lists vehicles
The system SHALL show the application name "Driving Log" in a top app bar on the Home screen, and SHALL show the
user's vehicles in the content area as specified in the `vehicles` capability. When the user has no vehicles, the
content area SHALL show an empty state that invites the user to add a vehicle. The Home screen SHALL offer an action
to add a vehicle.

#### Scenario: Home screen with vehicles
- **WHEN** the Home screen is displayed and the user has added vehicles
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area lists the vehicles

#### Scenario: Empty Home screen content
- **WHEN** the Home screen is displayed and the user has not added any vehicle
- **THEN** the top app bar shows "Driving Log"
- **AND** the content area shows the empty state and the action to add a vehicle

### Requirement: Material Design 3 theming
The system SHALL render all screens using Material Design 3, and SHALL use the light or dark color scheme
according to the device's system setting.

#### Scenario: Device in light mode
- **WHEN** the app is opened while the device is set to light mode
- **THEN** the app is displayed with the light color scheme

#### Scenario: Device in dark mode
- **WHEN** the app is opened while the device is set to dark mode
- **THEN** the app is displayed with the dark color scheme

#### Scenario: System setting changes while running
- **WHEN** the device switches between light and dark mode while the app is open
- **THEN** the app switches to the matching color scheme without restarting

### Requirement: Home screen state survives configuration changes
The system SHALL keep the Home screen showing the same content across configuration changes such as device
rotation, without returning to a loading or initial state.

#### Scenario: Rotation on Android
- **WHEN** the user rotates the device while the Home screen is displayed
- **THEN** the Home screen is displayed again with the same content

### Requirement: iOS entry point
The shared code SHALL expose an entry point that an iOS host app can embed to display the same Home screen.
Behavior on iOS SHALL match Android unless a difference is recorded in a feature spec.

#### Scenario: iOS host embeds the entry point
- **WHEN** an iOS host app displays the shared entry point
- **THEN** the Home screen is displayed with the same content as on Android

**Platform note:** iOS is an opportunistic target. This requirement is verified on a Mac once the Xcode project exists,
not by Android builds or Linux CI.
