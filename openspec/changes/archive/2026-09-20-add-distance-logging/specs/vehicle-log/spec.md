# Spec Delta

## MODIFIED Requirements

### Requirement: Initial odometer event
The system SHALL record exactly one "Initial odometer" event in a vehicle's log when the vehicle is added, holding the
odometer reading entered in the vehicle's unit (a typed 0 is a valid reading), kept exactly as entered, and the date and
time the vehicle was added, to the minute (the seconds are dropped, so the time is on the same footing as a time chosen with a
picker), together with the device's time zone at that moment. Adding a vehicle SHALL either save the vehicle together with this event or save neither. The
initial odometer event is an odometer-setting event: it establishes an odometer reading. Other kinds of log event are
specified by the capabilities that create them.

#### Scenario: Event created with the vehicle
- **WHEN** the user adds a vehicle with the unit "Kilometers" and types 45200 in the odometer field
- **THEN** the vehicle's log contains one "Initial odometer" event with 45200 km and the time of adding

#### Scenario: The time of adding is to the minute
- **WHEN** the user adds a vehicle at 12:00:40
- **THEN** the "Initial odometer" event is dated 12:00, with no seconds

#### Scenario: An entry in the minute the vehicle was added counts
- **WHEN** the user adds a vehicle with an initial odometer of 45200 km at 12:00:40 and logs a trip distance of 30 km at 12:00, the time the form offers
- **THEN** the entry counts after the initial odometer event and the vehicle's current odometer is 45230 km

#### Scenario: Zero odometer is logged
- **WHEN** the user adds a vehicle with the unit "Miles" and types 0 in the odometer field
- **THEN** the vehicle's log contains one "Initial odometer" event with 0 mi

#### Scenario: Tenths reading is logged
- **WHEN** the user adds a vehicle with the unit "Kilometers with 100 m" and types 4, 5, 2, 0, 0, 3 in the odometer field
- **THEN** the log's "Initial odometer" event holds 45200.3 km and shows it as "45,200.3 km" with an English (United States) device locale

#### Scenario: Rejected input creates nothing
- **WHEN** saving a vehicle is rejected because of invalid input
- **THEN** neither a vehicle nor a log event is created

### Requirement: Current odometer is derived from the log
The system SHALL show a vehicle's current odometer, in the vehicle's unit, as the reading of its latest
odometer-setting event plus the distances of all distance entries logged after that event, and SHALL NOT keep a separately
edited total. Events are ordered by their date and time, and events at the same time by when they were added. A distance
entry dated before the latest odometer-setting event SHALL NOT count.

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

### Requirement: Recent events on the details screen
The system SHALL show at most the 5 most recent log events of the vehicle on its details screen, newest first by their date
and time, each with its type and its date and time, shown in the time zone the event was entered in and followed by that zone's
name when it is not the device's current time zone. The date is written year-month-day and the time follows the system's
12-hour or 24-hour setting (the examples below use a 24-hour setting). An "Initial odometer" event SHALL show its reading in the vehicle's
unit. An "Odometer reading" event (an odometer anchor) SHALL show its reading in the vehicle's unit, without a plus sign. A
"Distance" event SHALL show its distance in the vehicle's unit with a plus sign and, when it was logged as a new
odometer count, the count that was typed. When events share the same time, the one added last SHALL come first. The
system SHALL provide an action to open the full log.

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

### Requirement: Full log
The system SHALL show the vehicle's complete log on a separate screen opened from the details screen, listing every event
newest first by its date and time, each shown as it is in the recent events. Going back SHALL return to the details screen.

#### Scenario: Open the full log
- **WHEN** the user taps the full log action on a vehicle's details screen
- **THEN** the full log screen lists all of the vehicle's events, newest first

#### Scenario: Readings use the vehicle's unit and the device locale
- **WHEN** the user opens the full log of a vehicle whose unit is "Kilometers with 100 m" and whose initial odometer was 45200.3, with an English (United States) device locale
- **THEN** the "Initial odometer" event shows "45,200.3 km"

#### Scenario: Distance entries in the full log
- **WHEN** a vehicle whose unit is "Kilometers with 100 m" has an initial odometer and two distance entries of 12.3 km and 4.5 km, with an English (United States) device locale
- **THEN** the full log lists both entries as "+12.3 km" and "+4.5 km", newest first, with the initial odometer event last

#### Scenario: Back to the details
- **WHEN** the user navigates back from the full log screen
- **THEN** the vehicle's details screen is displayed

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name or license plate is edited. Existing log events SHALL
NOT be modified or removed by any action; the log is only ever added to.

#### Scenario: Editing does not add events
- **WHEN** the user edits the name and plate of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

#### Scenario: Logging a distance only adds an event
- **WHEN** the user logs a distance entry
- **THEN** the vehicle's log contains every event it had before, unchanged, and one new "Distance" event

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.

## ADDED Requirements

### Requirement: Event times keep the time zone they were entered in
The system SHALL store the date and time of every log event together with the time zone it was entered in, and SHALL show it in
that time zone, whatever the device's time zone is later, until the user chooses otherwise. The stored form SHALL NOT depend on
the device locale. The date and time of every event SHALL have minute precision: the seconds are dropped, whether the time was
chosen by the user or taken from the clock. Events are ordered by the instant they mean, not by the wall-clock time shown. An event written before time
zones were stored has no zone and SHALL be shown in the device's current time zone.

#### Scenario: The device zone changes later
- **WHEN** a distance entry was entered as 18:30 on 2026-09-20 in Europe/Helsinki, and later the device time zone is America/New_York
- **THEN** the entry is still shown as "2026-09-20 18:30 (Europe/Helsinki)"

#### Scenario: Order follows the instant
- **WHEN** the log has an entry entered as 15:00 in Europe/Helsinki (12:00 UTC) and another entered as 08:30 in America/New_York (12:30 UTC), on the same day
- **THEN** the full log lists the New York entry above the Helsinki entry, although its wall-clock time is earlier

#### Scenario: The initial odometer event records the device zone
- **WHEN** the user adds a vehicle while the device time zone is Europe/Helsinki
- **THEN** the "Initial odometer" event is stored and shown in Europe/Helsinki

#### Scenario: An event from before time zones were stored
- **WHEN** the log holds an event that was written without a time zone and the device time zone is America/New_York
- **THEN** the event is shown in America/New_York with no zone name

### Requirement: Odometer anchor events
The system SHALL support an odometer anchor event in a vehicle's log: an odometer-setting event, added after the vehicle,
that sets the odometer to a reading at a date and time. It SHALL be ordered like every event, and SHALL count as an odometer-setting
event wherever the log is derived from: the latest odometer-setting event at or before a time, whether the initial odometer event or an anchor, is
the baseline of the odometer known at that time.

#### Scenario: An anchor before the initial event does not change the current odometer
- **WHEN** a vehicle's log contains an initial odometer event of 45200 km at 12:00 and an odometer anchor of 44000 km the day before
- **THEN** the vehicle's current odometer is 45200 km, and the odometer known at a time between them is 44000 km

#### Scenario: A later anchor replaces the running total
- **WHEN** a vehicle's log contains an initial odometer event of 45200 km, a distance entry of 30 km after it, and then an odometer anchor of 45300 km
- **THEN** the vehicle's current odometer is 45300 km

#### Scenario: An anchor row in the log
- **WHEN** the log contains an odometer anchor of 44000 km for a vehicle whose unit is "Kilometers", with an English (United States) device locale
- **THEN** its row is labelled "Odometer reading" and shows "44,000 km"

