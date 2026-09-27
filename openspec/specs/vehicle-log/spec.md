# vehicle-log Specification

## Purpose
Defines the per-vehicle event log: an append-only history of what happened to a vehicle. For now it holds only the
initial odometer reading, and it is the source of the vehicle's current odometer.

Odometer values in the scenarios are written with plain digits and a period; on screen they follow the unit and
locale rules of the `vehicles` capability.

## Requirements

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

### Requirement: An event row opens its details
The system SHALL make every event row tappable, in the recent events on the vehicle's details screen (`vehicle-log`,
"Recent events on the details screen") and in the full log (`vehicle-log`, "Full log"), opening that event's details
screen (`event-details`).

#### Scenario: Tap a row in recent events
- **WHEN** the user taps an event row in the recent events section
- **THEN** that event's details screen opens

#### Scenario: Tap a row in the full log
- **WHEN** the user taps an event row in the full log
- **THEN** that event's details screen opens

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name, license plate or picture is edited.
Existing log events SHALL NOT be modified or removed by any action, **except the one explicit edit action
`event-details`'s "A 'Distance' or 'Odometer reading' event's note can be edited" requirement adds**: changing or
clearing a "Distance" or "Odometer reading" event's note from its details screen. That action SHALL change only the
note of the one event being edited and SHALL NOT alter any other field of it or any other event. Every other action
still leaves the log exactly as it is; the log is otherwise only ever added to.

#### Scenario: Editing does not add events
- **WHEN** the user edits the name and plate of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

#### Scenario: Changing the picture does not add events
- **WHEN** the user changes or removes the picture of a vehicle and saves
- **THEN** the vehicle's log contains the same events as before

#### Scenario: Logging a distance only adds an event
- **WHEN** the user logs a distance entry
- **THEN** the vehicle's log contains every event it had before, unchanged, and one new "Distance" event

#### Scenario: Editing a note changes only that note
- **WHEN** the user edits a "Distance" event's note through its details screen and navigates back
- **THEN** every other event in the log, and every other field of the edited event, is exactly as it was before

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.

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

### Requirement: Distance and odometer-anchor events can carry a note
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry an optional note, entered on
the log event form when the event is logged (`distance-logging`, "A pending note is saved with the event"), kept
exactly as entered. Once an event is saved, its note SHALL only be changed through the details screen's "Edit"
action (`event-details`, "A 'Distance' or 'Odometer reading' event's note can be edited"); no other action changes
it. The "Initial odometer" event created when a vehicle is added SHALL NOT carry a note.

#### Scenario: A distance entry with a note
- **WHEN** the user logs a trip distance with the note "borrowed to Sam"
- **THEN** the log's new "Distance" event holds that note

#### Scenario: An odometer anchor with a note
- **WHEN** the user logs a new odometer count with a note, at a time when no odometer is known yet
- **THEN** the log's new "Odometer reading" event holds that note

#### Scenario: A note is optional
- **WHEN** the user logs a distance entry without ever adding a note
- **THEN** the log's new event holds no note

#### Scenario: A note can be corrected after saving
- **WHEN** the user changes a saved "Distance" event's note through its details screen's "Edit" action
- **THEN** the log holds that event with the changed note, and no other event or field is affected

### Requirement: Distance and odometer-anchor events can carry photos
The system SHALL let a "Distance" event or an "Odometer reading" (anchor) event carry from 0 to 5 photos, attached
on the log event form when the event is logged (`distance-logging`, "Attached photos are saved with the event"), or
added or removed later through the details screen's "Edit" action (`add-event-editing`, `event-pictures`'s own
delta of `event-details`). The "Initial odometer" event created when a vehicle is added SHALL NOT carry any photo.

#### Scenario: A distance entry with photos
- **WHEN** the user logs a trip distance with 2 photos attached
- **THEN** the log's new "Distance" event holds those 2 photos

#### Scenario: Photos are optional
- **WHEN** the user logs a distance entry without attaching any photo
- **THEN** the log's new event holds no photo

### Requirement: A note's presence is shown as an icon on the event row
The system SHALL show a small icon cluster, anchored at the bottom-end (bottom-trailing) corner of the row, in the
recent events on the vehicle's details screen (`vehicle-log`, "Recent events on the details screen") and in the full
log (`vehicle-log`, "Full log"), for any event that has a non-empty note, one or more photos, or both. The note icon
SHALL be shown first (when the event has a note), followed by the photo icon (when the event has at least one
photo); an event with neither SHALL show neither icon, and the row SHALL show no other, larger indicator (no
thumbnail, no "hero" image) — the icons are presence-only. The row SHALL NOT show the note's text or a photo's
content; reading either back is done from the event's details screen (`event-details`).

#### Scenario: A row with a note
- **WHEN** the recent events or the full log include a "Distance" event that has a note
- **THEN** that event's row shows the note icon, at the row's bottom-end corner

#### Scenario: A row without a note
- **WHEN** an event has no note and no photo
- **THEN** its row shows neither icon

#### Scenario: The note's text is not on the row
- **WHEN** an event with a note is shown in either list
- **THEN** the row shows the icon but not the note's text

#### Scenario: A row with photos only
- **WHEN** an event has one or more photos and no note
- **THEN** its row shows only the photo icon, at the row's bottom-end corner

#### Scenario: A row with both
- **WHEN** an event has both a note and one or more photos
- **THEN** its row shows both icons together at the bottom-end corner, the note icon first

#### Scenario: No thumbnail or hero image in the row
- **WHEN** an event with photos is shown in either list
- **THEN** the row shows the photo icon only, never a thumbnail or a larger image of any attached photo
