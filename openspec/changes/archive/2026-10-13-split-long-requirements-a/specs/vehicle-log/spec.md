## MODIFIED Requirements

### Requirement: Initial odometer event
When a vehicle is added, the system SHALL record exactly one "Initial odometer" event in its log: the odometer reading
in the vehicle's unit, exactly as entered (0 is valid), dated when the vehicle was added, to the minute (seconds
dropped, as for a picked time), in the device's time zone then. The vehicle and this event SHALL be saved together or
not at all. It is an odometer-setting event; other kinds of event are specified by the capabilities that create them.

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
The system SHALL show a vehicle's current odometer, in its unit, as the reading of its latest odometer-setting event
plus the distances of all "Distance" events after it, ordered as "Events at the same time are ordered by entry, then
by kind" describes, and SHALL NOT keep a separately edited total. A "Distance" event before it SHALL NOT count. A
"Refueling" event SHALL neither set nor add to the odometer; a refueling's mileage counts through its entry's own
mileage event.

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
- **WHEN** a vehicle's log contains an initial event of 45200 km and a later refueling logged with a trip-distance mileage of 30 km
- **THEN** the refueling's entry holds a "Distance" event of 30 km and the vehicle's current odometer is 45230 km

#### Scenario: A refueling with no mileage does not count
- **WHEN** a vehicle's log contains an initial event of 45200 km and a later "Refueling" event saved with no mileage
- **THEN** the vehicle's current odometer is still 45200 km

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
- **THEN** the recent events show a "Refueling" event with "42.3 L" and its date and time

#### Scenario: A refueling with mileage shows two rows
- **WHEN** the user logs a refueling of 42.3 L with a trip-distance mileage of 30 km for a vehicle with the unit "Kilometers"
- **THEN** the recent events show a "Refueling" row with "42.3 L" and, directly below it, a "Distance" row with "+30 km", both with the same date and time

### Requirement: The log is not changed by editing the vehicle
The system SHALL leave a vehicle's log unchanged when the vehicle's name, license plate or picture is edited.
Existing log events SHALL NOT be modified or removed by any action except one: changing or clearing a "Distance" or
"Odometer reading" event's note from its details screen (`event-details`, "A 'Distance' or 'Odometer reading'
event's note can be edited"), which SHALL change only that one event's note and nothing else. The log is otherwise
only ever added to.

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
The system SHALL store every log event's date and time with the time zone it was entered in, and SHALL show it in that
zone, whatever the device's zone is later, until the user chooses otherwise. The stored form SHALL NOT depend on the
device locale. Event times SHALL have minute precision, the seconds dropped, whether picked or taken from the clock.
Events are ordered by the instant they mean, not the wall-clock time. An event stored without a zone SHALL be shown in
the device's current zone.

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

### Requirement: Distance and odometer-anchor events can carry a note
The system SHALL let a "Distance" or an "Odometer reading" (anchor) event carry an optional note, entered on the log
event form (`distance-logging`, "A pending note is saved with the event") and kept exactly as entered. Once the event
is saved, its note SHALL change only through the details screen's "Edit" action (`event-details`, "A 'Distance' or
'Odometer reading' event's note can be edited"). The "Initial odometer" event SHALL NOT carry a note.

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

### Requirement: A refueling event can carry a note and photos
The system SHALL let a "Refueling" event carry an optional note and from 0 to 5 photos, as a "Distance" event or an
odometer anchor can, entered on the log event form when the refueling is logged (`refueling-logging`, "Attached fuel
data is saved with the event, or discarded with the rest of the form"). Once saved, they SHALL change only through
the details screen's "Edit" action (`event-details`); no other action changes them.

#### Scenario: A refueling with a note
- **WHEN** the user logs a refueling with the note "cheap gas today"
- **THEN** the log's new "Refueling" event holds that note

#### Scenario: A refueling with photos
- **WHEN** the user logs a refueling with 2 photos attached
- **THEN** the log's new "Refueling" event holds those 2 photos

#### Scenario: Both are optional
- **WHEN** the user logs a refueling without ever adding a note or a photo
- **THEN** the log's new event holds neither

### Requirement: A note's presence is shown as an icon on the event row
The system SHALL show a small icon cluster at the bottom-end corner of every row, in the recent events and the full
log, of an entry with a non-empty note, photos, or both: the note icon first (when there is a note), then the photo
icon (when there are photos). A row with neither SHALL show neither. The icons are presence-only: the row SHALL NOT
show the note's text, a photo, a thumbnail or any larger image; those are read on the details screen
(`event-details`).

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

#### Scenario: Every row of an entry shows its icons
- **WHEN** a refueling was logged with a trip-distance mileage and a note
- **THEN** both its "Refueling" row and its "Distance" row show the note icon

## ADDED Requirements

### Requirement: An event row shows when it happened
The system SHALL show an event row's date and time, in the recent events and the full log, in the time zone the
event was entered in, followed by that zone's name when it is not the device's current time zone. The date SHALL be
written year-month-day, and the time SHALL follow the system's 12-hour or 24-hour setting.

#### Scenario: A refueling from another time zone in the full log
- **WHEN** the system uses the 24-hour format, the device time zone is Europe/Helsinki, and the full log has a
  refueling entered as 08:30 on 2026-09-20 in America/New_York
- **THEN** its row shows "2026-09-20 08:30 (America/New_York)"

### Requirement: An event row shows its figure by kind
The system SHALL show an event row's figure, in the recent events and the full log: an "Initial odometer" event's
reading in the vehicle's unit; an "Odometer reading" event's reading in that unit, without a plus sign; a "Distance"
event's distance in that unit with a plus sign, and the typed count when it was logged as a new odometer count; a
"Refueling" event's fuel amount in the unit it was entered in (e.g. "42.3 L"), without a plus sign.

#### Scenario: An odometer reading's row has no plus sign
- **WHEN** the user logs a new odometer count of 44000 a week before the initial odometer of a vehicle with the unit
  "Kilometers", with an English (United States) device locale
- **THEN** its "Odometer reading" row shows "44,000 km", with no plus sign
