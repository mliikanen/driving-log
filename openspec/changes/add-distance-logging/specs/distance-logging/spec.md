# Spec Delta

## Purpose

Lets users log driving as distance entries: from the vehicle details screen they record a distance travelled, either as
the trip meter distance or as the new odometer count, at a chosen date, time and time zone. The entries build the vehicle's log and
its current odometer.

## ADDED Requirements

### Requirement: Log a distance from the vehicle details screen
The system SHALL offer a "Log distance" action on the vehicle details screen that opens a form for one distance entry.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing.

#### Scenario: Open the form
- **WHEN** the user taps "Log distance" on a vehicle's details screen
- **THEN** the log distance form for that vehicle is displayed

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged

### Requirement: A distance is logged as a trip distance or as a new odometer count
The system SHALL let the user choose, on the log distance form, between "Trip distance" and "New odometer". "Trip distance"
SHALL be preselected. With "Trip distance" the number entered is the distance travelled. With "New odometer" the number
entered is the odometer count now, and the distance logged is that count minus the previous known odometer at the entry's
date and time. Each way SHALL keep its own typed number when the user switches between them.

#### Scenario: Log by trip distance
- **WHEN** a vehicle's current odometer is 45200 km and the user logs a trip distance of 30 km and saves
- **THEN** the log contains a distance entry of 30 km and the vehicle's current odometer is 45230 km

#### Scenario: Log by new odometer
- **WHEN** a vehicle's current odometer is 45200 km and the user chooses "New odometer", types 45250 and saves
- **THEN** the log contains a distance entry of 50 km and the vehicle's current odometer is 45250 km

#### Scenario: Preselected way
- **WHEN** the user opens the log distance form
- **THEN** "Trip distance" is selected

#### Scenario: Each way keeps its own number
- **WHEN** the user types 30 as a trip distance, switches to "New odometer", types 45250, and switches back
- **THEN** the trip distance still shows 30 and the odometer field still shows 45250

### Requirement: The previous known odometer
The system SHALL define the previous known odometer at a date and time as the reading of the latest odometer-setting
event of the vehicle at or before that time, plus the distances of all distance entries after that event up to and
including that time. Events at the same time SHALL count in the order they were added. When the user chooses "New
odometer", the form SHALL show the previous known odometer for the chosen date and time and the distance that would be
logged, and SHALL update them as the number or the date and time change.

#### Scenario: Known odometer includes earlier entries
- **WHEN** a vehicle has an initial odometer of 45200 km and a distance entry of 30 km logged after it, and the user chooses "New odometer" for a time after both
- **THEN** the form shows a previous known odometer of 45230 km

#### Scenario: Live distance
- **WHEN** the previous known odometer is 45230 km and the user types 45250 in the odometer field
- **THEN** the form shows that the distance logged will be 20 km

#### Scenario: Known odometer follows the chosen time
- **WHEN** a vehicle has an initial odometer of 45200 km and distance entries of 30 km on Monday and 20 km on Wednesday, and the user chooses the time Tuesday
- **THEN** the form shows a previous known odometer of 45230 km

### Requirement: The new odometer must be higher than the previous known odometer
The system SHALL refuse a new odometer count that is not higher than the previous known odometer at the entry's date and
time, showing an error on the odometer field that names the previous known odometer, and SHALL NOT save the entry.

#### Scenario: Lower count
- **WHEN** the previous known odometer is 45230 km and the user types 45100 as the new odometer and saves
- **THEN** the system shows an error naming 45,230 km, stays on the form and adds nothing

#### Scenario: Equal count
- **WHEN** the previous known odometer is 45230 km and the user types 45230 as the new odometer and saves
- **THEN** the system shows the same error and adds nothing

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field. A trip distance SHALL be more than zero. The system SHALL show an
error on the field and SHALL NOT save the entry when the field is empty, or when a trip distance is zero.

#### Scenario: Empty field
- **WHEN** the user tries to save with nothing typed in the field
- **THEN** the system shows an error on the field and adds nothing

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

### Requirement: The unit of an entry is selectable
The system SHALL let the user choose the unit of the entry on the form: kilometers or miles, and separately whether tenths are
included. The form SHALL preselect the unit family and the tenths choice of the vehicle's odometer unit ("Kilometers" and
"Kilometers with 100 m" are kilometers, "Miles" and "Miles with tenths" are miles; the two tenths units include tenths).
Both fields of the form SHALL be entered in the chosen unit, using the same microwave-style number field as the initial
odometer, so whole-number entries and entries with one decimal are both possible whatever the vehicle's unit. Changing the
unit SHALL keep the digits typed, as it does when adding a vehicle. An entry logged in another unit than the vehicle's SHALL
be converted, and SHALL be shown in the vehicle's unit everywhere.

#### Scenario: Defaults follow the vehicle
- **WHEN** the user opens the form for a vehicle with the unit "Miles with tenths"
- **THEN** miles is selected and tenths are included

#### Scenario: Defaults for a whole-number kilometer vehicle
- **WHEN** the user opens the form for a vehicle with the unit "Kilometers"
- **THEN** kilometers is selected and tenths are not included

#### Scenario: Tenths for a whole-number vehicle
- **WHEN** the user opens the form for a vehicle with the unit "Kilometers", includes tenths, types 1, 2, 3 as a trip distance and saves
- **THEN** the log contains a distance entry of 12.3 km, shown as "12 km" in the vehicle's unit

#### Scenario: Another unit than the vehicle's
- **WHEN** the user logs a trip distance of 10 miles for a vehicle with the unit "Kilometers with 100 m"
- **THEN** the distance entry is shown as "+16.1 km" (with an English (United States) device locale)

#### Scenario: Changing the unit keeps the digits
- **WHEN** the user has typed 123 with kilometers selected and switches to miles
- **THEN** the field still shows the digits 123, now in miles

### Requirement: The date, time and time zone of an entry
The system SHALL show the date, time and time zone of the entry on the form, with pickers to change each. The moment SHALL
default to the moment the form was opened, not the moment the entry is saved, and the time zone to the device's current time
zone. The user SHALL be able to choose any time zone from the list of available time zones, which SHALL be searchable by name.
The date and time picked are the wall-clock time in the chosen zone: changing the zone keeps the date and time shown and changes
which instant they mean. The user SHALL be able to choose a moment in the past. The system SHALL refuse a moment later than the
time of saving, showing an error and adding nothing. The entry SHALL be stored with the zone it was entered in, as the
`vehicle-log` capability specifies.

#### Scenario: Default is when the form was opened, in the device's zone
- **WHEN** the device time zone is Europe/Helsinki and the user opens the form at 10:15 and saves at 10:20
- **THEN** the entry has the time 10:15 in Europe/Helsinki

#### Scenario: Choose an earlier day
- **WHEN** the user picks yesterday's date and a time and saves a valid entry
- **THEN** the entry appears in the log at that date and time, in its chronological place

#### Scenario: Choose another time zone
- **WHEN** the device time zone is Europe/Helsinki, the user selects America/New_York, keeps the time 08:30 and saves
- **THEN** the entry is stored as 08:30 in America/New_York, which is 15:30 in Europe/Helsinki, and it is shown as 08:30 with the zone America/New_York

#### Scenario: Changing the zone keeps the wall-clock time
- **WHEN** the form shows 18:30 in Europe/Helsinki and the user selects America/New_York
- **THEN** the form shows 18:30 in America/New_York

#### Scenario: Search the time zones
- **WHEN** the user opens the time zone list and types "new_y"
- **THEN** the list shows America/New_York and the other zones whose names contain the text

#### Scenario: Future moment
- **WHEN** the user picks a date and time later than now and saves
- **THEN** the system shows an error that the time cannot be in the future and adds nothing

#### Scenario: The future check compares instants, not wall-clock times
- **WHEN** it is 16:30 in Europe/Helsinki, which is 09:30 in America/New_York
- **THEN** entering 17:00 with the zone Europe/Helsinki shows the future error
- **AND** entering 09:00 with the zone America/New_York is accepted, because it is 16:00 in Helsinki and has already happened
- **AND** entering 10:00 with the zone America/New_York shows the future error

### Requirement: Entries before the initial odometer
The system SHALL accept a distance entry dated before the vehicle's initial odometer event when it is logged as a trip
distance. Such an entry SHALL appear in the log at its time and SHALL NOT change the vehicle's current odometer, nor any
previous known odometer at a later time. There is no known odometer before the initial odometer event, so for such a time
the form SHALL make "New odometer" unavailable, telling the user that no odometer is known at that time and that the trip
distance can be entered instead, and SHALL NOT save an entry logged that way.

#### Scenario: Trip distance before the initial odometer
- **WHEN** a vehicle was added with an initial odometer of 45200 km and the user logs a trip distance of 30 km dated a week before the initial odometer event
- **THEN** the log contains the entry at that earlier time and the vehicle's current odometer is still 45200 km

#### Scenario: New odometer before the initial odometer
- **WHEN** the user chooses "New odometer" and a time before the initial odometer event
- **THEN** the form says no odometer is known at that time, and saving is not possible until the time or the way is changed

### Requirement: Logging only adds to the log
The system SHALL record each saved distance entry as a new event and SHALL NOT change or remove any existing event, nor any
stored total. Saving SHALL either add the entry or add nothing.

#### Scenario: Earlier events are untouched
- **WHEN** the user logs a distance entry
- **THEN** every event that was in the log before is still there with the same values

### Requirement: Distance logging persists and works offline
The system SHALL keep distance entries on the device so they are still present after the app is closed and reopened, and
SHALL provide the whole form without a network connection.

#### Scenario: Survives a restart
- **WHEN** the user logs a distance entry, closes the app completely and opens it again
- **THEN** the entry is still in the vehicle's log and counted in its current odometer

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user logs a distance entry
- **THEN** the entry is saved and no network error is shown

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.
