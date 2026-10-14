## MODIFIED Requirements

### Requirement: Recent events on the details screen
The system SHALL show at most the 5 most recent log events of the vehicle on its details screen, newest first by date
and time, each as its own row with its type, date and time, and figure (the scenarios use a 24-hour setting). A
refueling logged with mileage SHALL show as two rows, each counting toward the 5. Events at the same time SHALL be in
the order "Events at the same time are ordered by entry, then by kind" gives, newest first. The system SHALL provide
an action to open the full log.

#### Scenario: One event
- **WHEN** the user opens the details of a newly added vehicle
- **THEN** the recent events section shows the single "Initial odometer" event

#### Scenario: More than five events
- **WHEN** a vehicle's log contains 7 events
- **THEN** the details screen shows the 5 newest events, newest first, and not the 2 oldest

#### Scenario: A distance entry
- **WHEN** the user logs a trip distance of 30 km for a vehicle with the unit "Kilometers"
- **THEN** the recent events show a "Distance" event with "+30 km" and its date and time, above the "Initial odometer" event

#### Scenario: An entry logged by odometer shows the typed count
- **WHEN** the user logs a new odometer count of 45250 for a vehicle with the unit "Kilometers" whose previous known odometer was 45200 km
- **THEN** the "Distance" event shows "+50 km" and the count 45,250 km

#### Scenario: An entry from another time zone
- **WHEN** the device time zone is Europe/Helsinki and the log has a distance entry entered as 08:30 on 2026-09-20 in America/New_York
- **THEN** its row shows "2026-09-20 08:30 (America/New_York)"

#### Scenario: A 12-hour system setting
- **WHEN** the system uses the 12-hour format, the device time zone is Europe/Helsinki and the log has a distance entry entered as 15:30 on 2026-09-20 in Europe/Helsinki
- **THEN** its row shows "2026-09-20 3:30 PM" and no zone name, and an entry entered as 08:30 in America/New_York shows "2026-09-20 8:30 AM (America/New_York)"

#### Scenario: An entry from the device's zone shows no zone name
- **WHEN** the device time zone is Europe/Helsinki and the log has a distance entry entered as 15:30 on 2026-09-20 in Europe/Helsinki
- **THEN** its row shows "2026-09-20 15:30" and no zone name

#### Scenario: A backdated entry sits in its chronological place
- **WHEN** the user logs a distance entry dated yesterday after having logged one dated today
- **THEN** the recent events list the entry dated today above the one dated yesterday, whichever was added first

#### Scenario: A refueling's row
- **WHEN** the user logs a refueling of 42.3 L for a vehicle
- **THEN** the recent events show a "Refueling" event with "42.30 L" and its date and time

#### Scenario: A refueling with mileage shows two rows
- **WHEN** the user logs a refueling of 42.3 L with a trip-distance mileage of 30 km for a vehicle with the unit "Kilometers"
- **THEN** the recent events show a "Refueling" row with "42.30 L" and, directly below it, a "Distance" row with "+30 km", both with the same date and time

### Requirement: An event row shows its figure by kind
The system SHALL show an event row's figure, in the recent events and the full log: an "Initial odometer" event's
reading in the vehicle's unit; an "Odometer reading" event's reading in that unit, without a plus sign; a "Distance"
event's distance in that unit with a plus sign, and the typed count when it was logged as a new odometer count; a
"Refueling" event's fuel amount in the unit it was entered in (e.g. "42.30 L"), without a plus sign.

#### Scenario: An odometer reading's row has no plus sign
- **WHEN** the user logs a new odometer count of 44000 a week before the initial odometer of a vehicle with the unit
  "Kilometers", with an English (United States) device locale
- **THEN** its "Odometer reading" row shows "44,000 km", with no plus sign
