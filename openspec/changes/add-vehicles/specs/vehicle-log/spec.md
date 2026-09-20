# Spec Delta

## Purpose

Defines the per-vehicle event log: an append-only history of what happened to a vehicle. For now it holds only the
initial odometer reading, and it is the source of the vehicle's current odometer.

Odometer values in the scenarios are written with plain digits and a period; on screen they follow the unit and
locale rules of the `vehicles` capability.

## ADDED Requirements

### Requirement: Initial odometer event
The system SHALL record exactly one "Initial odometer" event in a vehicle's log when the vehicle is added, holding the
odometer reading entered in the vehicle's unit (0 when no digit was entered), kept exactly as entered and the date and time the vehicle was added. Adding a vehicle SHALL either
save the vehicle together with this event or save neither. No other event type SHALL exist in this change.

#### Scenario: Event created with the vehicle
- **WHEN** the user adds a vehicle with the unit "Kilometers" and types 45200 in the odometer field
- **THEN** the vehicle's log contains one "Initial odometer" event with 45200 km and the time of adding

#### Scenario: Default odometer is logged
- **WHEN** the user adds a vehicle with the unit "Miles" without entering an odometer
- **THEN** the vehicle's log contains one "Initial odometer" event with 0 mi

#### Scenario: Tenths reading is logged
- **WHEN** the user adds a vehicle with the unit "Kilometers with 100 m" and types 4, 5, 2, 0, 0, 3 in the odometer field
- **THEN** the log's "Initial odometer" event holds 45200.3 km and shows it as "45,200.3 km" with an English (United States) device locale

#### Scenario: Rejected input creates nothing
- **WHEN** saving a vehicle is rejected because of invalid input
- **THEN** neither a vehicle nor a log event is created

### Requirement: Current odometer is derived from the log
The system SHALL show a vehicle's current odometer, in the vehicle's unit, as the odometer reading of its most recent
log event that has one, and SHALL NOT keep a separately edited total.

#### Scenario: Only the initial event
- **WHEN** a vehicle's log contains only an initial odometer event of 45200 km
- **THEN** the vehicle's current odometer is 45200 km

#### Scenario: A newer event exists
- **WHEN** a vehicle's log contains an initial event of 45200 km and a newer event with an odometer of 45900 km
- **THEN** the vehicle's current odometer is 45900 km

### Requirement: Recent events on the details screen
The system SHALL show at most the 5 most recent log events of the vehicle on its details screen, newest first, each
with its type, its odometer reading in the vehicle's unit and its date and time. When events share the same time, the one added last SHALL
come first. The system SHALL provide an action to open the full log.

#### Scenario: One event
- **WHEN** the user opens the details of a newly added vehicle
- **THEN** the recent events section shows the single "Initial odometer" event

#### Scenario: More than five events
- **WHEN** a vehicle's log contains 7 events
- **THEN** the details screen shows the 5 newest events, newest first, and not the 2 oldest

**Note:** only the initial event can be created through the app in this change, so the multi-event scenarios are
verified with prepared data in automated tests.

### Requirement: Full log
The system SHALL show the vehicle's complete log on a separate screen opened from the details screen, listing every
event newest first with its type, its odometer reading in the vehicle's unit and its date and time. Going back SHALL return to the details screen.

#### Scenario: Open the full log
- **WHEN** the user taps the full log action on a vehicle's details screen
- **THEN** the full log screen lists all of the vehicle's events, newest first

#### Scenario: Readings use the vehicle's unit and the device locale
- **WHEN** the user opens the full log of a vehicle whose unit is "Kilometers with 100 m" and whose initial odometer was 45200.3, with an English (United States) device locale
- **THEN** the "Initial odometer" event shows "45,200.3 km"

#### Scenario: Back to the details
- **WHEN** the user navigates back from the full log screen
- **THEN** the vehicle's details screen is displayed

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name or license plate is edited. Existing log
events SHALL NOT be modified or removed by any action in this change.

#### Scenario: Editing does not add events
- **WHEN** the user edits the name and plate of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.
