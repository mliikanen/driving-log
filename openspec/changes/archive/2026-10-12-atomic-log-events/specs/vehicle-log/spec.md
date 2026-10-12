# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Current odometer is derived from the log
The system SHALL show a vehicle's current odometer, in the vehicle's unit, as the reading of its latest
odometer-setting event plus the distances of all "Distance" events after that event, and SHALL NOT keep a
separately edited total. Events are ordered as "Events at the same time are ordered by entry, then by kind"
describes. A "Distance" event dated before the latest odometer-setting event SHALL NOT count. A "Refueling" event
does not participate in this calculation at all — it neither sets nor adds to the odometer; a refueling logged with
mileage takes part only through its entry's own "Distance" or "Odometer reading" event.

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
The system SHALL show at most the 5 most recent log events of the vehicle on its details screen, newest first by their date
and time, each with its type and its date and time, shown in the time zone the event was entered in and followed by that zone's
name when it is not the device's current time zone. The date is written year-month-day and the time follows the system's
12-hour or 24-hour setting (the examples below use a 24-hour setting). An "Initial odometer" event SHALL show its reading in the vehicle's
unit. An "Odometer reading" event (an odometer anchor) SHALL show its reading in the vehicle's unit, without a plus sign. A
"Distance" event SHALL show its distance in the vehicle's unit with a plus sign and, when it was logged as a new
odometer count, the count that was typed. A "Refueling" event SHALL show its fuel amount, in the unit it was
entered in (e.g. "42.3 L"), as its figure; it carries no plus sign, since a refueling is not itself a distance.
Each event is its own row: a refueling logged with mileage shows a "Refueling" row and a row for its mileage event,
each counting toward the 5. Events that share the same time SHALL be shown in the order "Events at the same time are
ordered by entry, then by kind" gives, newest first. The system SHALL provide an action to open the full log.

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

### Requirement: A note's presence is shown as an icon on the event row
The system SHALL show a small icon cluster, anchored at the bottom-end (bottom-trailing) corner of the row, in the
recent events on the vehicle's details screen (`vehicle-log`, "Recent events on the details screen") and in the full
log (`vehicle-log`, "Full log"), for any event whose entry has a non-empty note, one or more photos, or both — on
every row of that entry. The note icon
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

#### Scenario: Every row of an entry shows its icons
- **WHEN** a refueling was logged with a trip-distance mileage and a note
- **THEN** both its "Refueling" row and its "Distance" row show the note icon
