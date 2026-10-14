# vehicle-log Specification

## Purpose
Defines the per-vehicle event log: an append-only history of what happened to a vehicle. For now it holds only the
initial odometer reading, and it is the source of the vehicle's current odometer.

Odometer values in the scenarios are written with plain digits and a period; on screen they follow the unit and
locale rules of the `vehicles` capability.

## Requirements

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
- **THEN** the recent events show a "Refueling" event with "42.30 L" and its date and time

#### Scenario: A refueling with mileage shows two rows
- **WHEN** the user logs a refueling of 42.3 L with a trip-distance mileage of 30 km for a vehicle with the unit "Kilometers"
- **THEN** the recent events show a "Refueling" row with "42.30 L" and, directly below it, a "Distance" row with "+30 km", both with the same date and time

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
"Recent events on the details screen") and in the full log (`vehicle-log`, "Full log"), opening the details screen
(`event-details`) of the entry the event belongs to. Every row of the same entry SHALL open the same details.

#### Scenario: Tap a row in recent events
- **WHEN** the user taps an event row in the recent events section
- **THEN** that event's details screen opens

#### Scenario: Tap a row in the full log
- **WHEN** the user taps an event row in the full log
- **THEN** that event's details screen opens

#### Scenario: Either row of a refueling opens its details
- **WHEN** a refueling was logged with a trip-distance mileage, and the user taps its "Distance" row
- **THEN** the same details screen opens as for its "Refueling" row, showing the refueling with its mileage

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

### Requirement: The log stores one fact per event, grouped into entries
The system SHALL store a vehicle's log as atomic events, each holding one fact: an odometer reading sets the odometer,
a "Distance" adds a distance, a "Refueling" adds fuel. Everything saved by one action
(adding a vehicle, saving the log event form) SHALL be one entry, holding one or more events, all at the entry's
date, time and time zone, saved together or not at all. No event SHALL hold a second fact.

#### Scenario: A distance entry is one event
- **WHEN** the user logs a trip distance of 30 km
- **THEN** the log gains one entry holding one "Distance" event of 30 km

#### Scenario: A refueling with mileage is two events of one entry
- **WHEN** the user logs a refueling of 42.3 L with a trip-distance mileage of 30 km
- **THEN** the log gains one entry holding a "Refueling" event of 42.3 L and a "Distance" event of 30 km, both at the
  date and time chosen on the form

#### Scenario: A refueling event holds no mileage
- **WHEN** the log holds a refueling logged with a new odometer count
- **THEN** the "Refueling" event holds only the fuel amount, unit, fuel type and fill-up state, and the count is held
  by the entry's own "Distance" or "Odometer reading" event

### Requirement: The initial odometer is the odometer reading saved when the vehicle is added
The system SHALL store the initial odometer and every later odometer reading as the same kind of event, and SHALL
record with each entry where it was saved from: adding the vehicle, or the log event form. The odometer reading of
the entry saved when the vehicle is added SHALL be the vehicle's "Initial odometer": labelled so, without a note,
photos or an "Edit" action. Every other odometer reading SHALL be an "Odometer reading." Both SHALL set the
odometer the same way.

#### Scenario: The initial odometer keeps its label
- **WHEN** the user adds a vehicle with an odometer of 45200 km
- **THEN** the log shows an "Initial odometer" event of 45200 km, and its details screen shows no note section and no "Edit" action

#### Scenario: A later reading is an odometer reading
- **WHEN** the user logs a new odometer count of 44000 km a week before the initial odometer
- **THEN** the log shows it as an "Odometer reading," with the note element, photos and "Edit" action an odometer reading offers

#### Scenario: Both set the odometer alike
- **WHEN** a vehicle's log holds an initial odometer of 45200 km and, later, an odometer reading of 45300 km
- **THEN** the vehicle's current odometer is 45300 km, and the odometer known between them is 45200 km

### Requirement: Events at the same time are ordered by entry, then by kind
The system SHALL order events by their date and time. Events at the same date and time SHALL be ordered by the
entry they belong to, in the order the entries were added, and the events of one entry SHALL follow a fixed order
by kind: odometer reading (initial or not), "Distance," "Refueling." Lists that show the newest first SHALL
show this order reversed. The order SHALL be the same every time the log is shown and SHALL be the order in which
the log is derived from.

#### Scenario: A refueling's row sits above its own mileage row
- **WHEN** the user logs a refueling with a trip-distance mileage of 30 km
- **THEN** the recent events and the full log show its "Refueling" row directly above its "Distance" row

#### Scenario: Separate entries at the same time keep the order they were added in
- **WHEN** the user logs a refueling with no mileage at 12:00 and then a distance entry, also dated 12:00
- **THEN** the "Distance" row is shown above the "Refueling" row, since its entry was added last

#### Scenario: The order is stable
- **WHEN** the user opens the full log twice, or closes and reopens the app in between
- **THEN** events with the same date and time are shown in the same order each time

### Requirement: Events of one entry share one note and one set of photos
The system SHALL store an entry's note and photos once, with the entry, not with any one of its events and not
copied onto each of them. Every event of the entry SHALL show that note and those photos wherever an event's note or
photos are shown, and a change to them SHALL be seen from every event of the entry. The entry saved when the
vehicle is added SHALL carry neither.

#### Scenario: Both events of a refueling show its note
- **WHEN** the user logs a refueling with a trip-distance mileage and the note "cheap gas today"
- **THEN** both its "Refueling" row and its "Distance" row show the note icon, and the details screen opened from
  either shows "cheap gas today"

#### Scenario: A photo is stored once
- **WHEN** the user logs a refueling with a mileage and 2 photos attached
- **THEN** the entry holds 2 photos, not 4, and the details screen opened from either row shows the same 2 thumbnails

### Requirement: Existing logs are carried over when the app is updated
When the app is updated from a version that stored a refueling's mileage on the refueling event, the system SHALL
keep every logged event, and SHALL store each refueling that carried mileage as one entry holding a "Refueling"
event and the "Distance" or "Odometer reading" event the mileage stood for, at the refueling's date, time and time
zone. Every vehicle's current odometer, every note, every photo and every kept scan SHALL be unchanged.

#### Scenario: A refueling with a trip distance is split
- **WHEN** the app is updated with a log holding a refueling of 42.3 L that carried a trip distance of 30 km
- **THEN** the log holds one entry with a "Refueling" event of 42.3 L and a "Distance" event of 30 km at that
  refueling's date and time, and the vehicle's current odometer is what it was before the update

#### Scenario: A refueling with an odometer reading is split
- **WHEN** the app is updated with a log holding a refueling that set the odometer to 45300 km
- **THEN** the log holds one entry with a "Refueling" event and an "Odometer reading" event of 45300 km

#### Scenario: Notes and photos are kept
- **WHEN** the app is updated with a log holding a refueling with a mileage, a note and a photo
- **THEN** the note and the photo are shown from both of its rows after the update

#### Scenario: The initial odometer stays the initial odometer
- **WHEN** the app is updated with a log holding a vehicle's initial odometer of 45200 km and an odometer reading of 44000 km
- **THEN** the first is still shown as "Initial odometer" with no "Edit" action, and the second as "Odometer reading," with the same values

#### Scenario: Other events are unchanged
- **WHEN** the app is updated with a log holding distance entries, odometer readings and refuelings with no mileage
- **THEN** each is still one event of its own entry, with the same values, note and photos

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
"Refueling" event's fuel amount in the unit it was entered in (e.g. "42.30 L"), without a plus sign.

#### Scenario: An odometer reading's row has no plus sign
- **WHEN** the user logs a new odometer count of 44000 a week before the initial odometer of a vehicle with the unit
  "Kilometers", with an English (United States) device locale
- **THEN** its "Odometer reading" row shows "44,000 km", with no plus sign
