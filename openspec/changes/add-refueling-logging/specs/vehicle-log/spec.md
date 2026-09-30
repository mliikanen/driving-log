# Spec Delta

## ADDED Requirements

### Requirement: A refueling event can carry a note and photos
The system SHALL let a "Refueling" event carry an optional note and from 0 to 5 photos, the same as a "Distance"
event or an odometer anchor (`vehicle-log`, "Distance and odometer-anchor events can carry a note," "Distance and
odometer-anchor events can carry photos"), entered on the log event form when the refueling is logged
(`refueling-logging`, "Attached fuel data is saved with the event, or discarded with the rest of the form"). Once
saved, a refueling's note and photos can only be changed through the details screen's "Edit" action
(`event-details`, "A 'Distance,' 'Odometer reading' or 'Refueling' event's note can be edited"); no other action
changes them.

#### Scenario: A refueling with a note
- **WHEN** the user logs a refueling with the note "cheap gas today"
- **THEN** the log's new "Refueling" event holds that note

#### Scenario: A refueling with photos
- **WHEN** the user logs a refueling with 2 photos attached
- **THEN** the log's new "Refueling" event holds those 2 photos

#### Scenario: Both are optional
- **WHEN** the user logs a refueling without ever adding a note or a photo
- **THEN** the log's new event holds neither

## MODIFIED Requirements

### Requirement: Current odometer is derived from the log
The system SHALL show a vehicle's current odometer, in the vehicle's unit, as the reading of its latest
odometer-setting event plus the distances of all distance entries and refueling events carrying a trip-distance
mileage (`refueling-logging`, "Mileage is optional for a refueling") logged after that event, and SHALL NOT keep a
separately edited total. Events are ordered by their date and time, and events at the same time by when they were
added. A distance entry or a refueling's trip-distance mileage dated before the latest odometer-setting event SHALL
NOT count. A refueling saved with no mileage does not participate in this calculation at all — it neither sets nor
adds to the odometer.

#### Scenario: Only the initial event
- **WHEN** a vehicle's log contains only an initial odometer event of 45200 km
- **THEN** the vehicle's current odometer is 45200 km

#### Scenario: A newer event exists
- **WHEN** a vehicle's log contains an initial event of 45200 km and a later distance entry of 30 km
- **THEN** the vehicle's current odometer is 45230 km

#### Scenario: Several entries add up
- **WHEN** a vehicle's log contains an initial event of 45200 km and later distance entries of 30 km, 20 km and 0.5 km
- **THEN** the vehicle's current odometer is 45250.5 km, shown as "45,251 km" for a vehicle with the unit "Kilometers"

#### Scenario: Entries before the initial event do not count
- **WHEN** a vehicle's log contains an initial event of 45200 km at 12:00 and a distance entry of 30 km at 09:00 the same day
- **THEN** the vehicle's current odometer is 45200 km

#### Scenario: Entries at the same time count in the order they were added
- **WHEN** a distance entry is added at exactly the time of the initial event
- **THEN** it counts after that event and the current odometer includes it

#### Scenario: A refueling's mileage counts like a distance entry
- **WHEN** a vehicle's log contains an initial event of 45200 km and a later "Refueling" event logged with a trip-distance mileage of 30 km
- **THEN** the vehicle's current odometer is 45230 km

#### Scenario: A refueling with no mileage does not count
- **WHEN** a vehicle's log contains an initial event of 45200 km and a later "Refueling" event saved with no mileage
- **THEN** the vehicle's current odometer is still 45200 km

### Requirement: Recent events on the details screen
The system SHALL show at most the 5 most recent log events of the vehicle on its details screen, newest first by their date
and time, each with its type and its date and time, shown in the time zone the event was entered in and followed by that zone's
name when it is not the device's current time zone. The date is written year-month-day and the time follows the system's
12-hour or 24-hour setting (the examples below use a 24-hour setting). An "Initial odometer" event SHALL show its reading in the vehicle's
unit. An "Odometer reading" event (an odometer anchor) SHALL show its reading in the vehicle's unit, without a plus sign. A
"Distance" event SHALL show its distance in the vehicle's unit with a plus sign and, when it was logged as a new
odometer count, the count that was typed. A "Refueling" event SHALL show its fuel amount, in the unit it was
entered in (e.g. "42.3 L"), as its figure; it carries no plus sign, since a refueling is not itself a distance.
When events share the same time, the one added last SHALL come first. The system SHALL provide an action to open the full log.

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
- **THEN** the recent events show a "Refueling" event with "42.3 L" and its date and time
