# Spec Delta

## Purpose
Records a trip: a span of time on one vehicle, started and ended by the user, that groups the entries logged while it is open. A stub: the requirements below are the proposed shape and are settled together with the open questions in the design.

## ADDED Requirements

### Requirement: A trip is started and ended from the Home screen
The system SHALL let the user start a trip for a vehicle and end it from the Home screen's trip action. The action SHALL read "Start trip" when no trip is open and "End trip" while one is. Starting a trip SHALL record its vehicle and its start moment, ending it SHALL record its end moment, and each moment SHALL be stored with its time zone like every other date and time in the application. Trips SHALL persist and work without a network.

#### Scenario: Start a trip
- **WHEN** the user has a vehicle and taps "Start trip" and chooses the vehicle
- **THEN** a trip is open for the vehicle, and the Home screen's trip action reads "End trip"

#### Scenario: End a trip
- **WHEN** a trip is open and the user taps "End trip"
- **THEN** the trip is closed with the end moment, and the action reads "Start trip" again

### Requirement: Entries logged during a trip belong to it
The system SHALL attach an entry saved while a trip of its vehicle is open to that trip, and SHALL show a trip's distance as the sum of its entries, derived when shown and never stored.

#### Scenario: An entry during a trip
- **WHEN** a trip is open for a vehicle and the user saves a distance entry for it
- **THEN** the entry belongs to the trip, and the trip's distance includes it

#### Scenario: An entry when no trip is open
- **WHEN** no trip is open for a vehicle and the user saves an entry for it
- **THEN** the entry belongs to no trip
