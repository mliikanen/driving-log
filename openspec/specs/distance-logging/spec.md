# distance-logging Specification

## Purpose
Lets users log driving as distance entries: from the vehicle details screen they record a distance travelled, either as
the trip meter distance or as the new odometer count, at a chosen date, time and time zone. The entries build the vehicle's log and
its current odometer.

## Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one distance entry.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing. The form's dismiss action, in the top-left of
its top app bar, SHALL be a close "X" (a Material full-screen dialog's dismiss icon), not a back arrow, since leaving
the form always discards whatever was entered.

#### Scenario: Open the form
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the log event form for that vehicle is displayed

#### Scenario: Save an entry
- **WHEN** the user enters a valid entry and saves
- **THEN** the vehicle's details screen is displayed and shows the new entry among the recent events

#### Scenario: Leave without saving
- **WHEN** the user leaves the form without saving
- **THEN** no entry is added and the vehicle's current odometer is unchanged

#### Scenario: The dismiss action is a close icon
- **WHEN** the user opens the log event form, from a vehicle's details screen or from the Home screen
- **THEN** the top-left action of the form's top app bar is a close "X", not a back arrow

### Requirement: A distance is logged as a trip distance or as a new odometer count
The system SHALL let the user choose, on the log event form, between "Trip distance" and "New odometer". "Trip distance"
SHALL be preselected. With "Trip distance" the number entered is the distance travelled. With "New odometer" the number
entered is the odometer count now, and the distance logged is that count minus the previous known odometer at the entry's
date and time (or, when no odometer is known at that time, an odometer anchor is saved instead). Each way SHALL keep its own typed number when the user switches between them.

#### Scenario: Log by trip distance
- **WHEN** a vehicle's current odometer is 45200 km and the user logs a trip distance of 30 km and saves
- **THEN** the log contains a distance entry of 30 km and the vehicle's current odometer is 45230 km

#### Scenario: Log by new odometer
- **WHEN** a vehicle's current odometer is 45200 km and the user chooses "New odometer", types 45250 and saves
- **THEN** the log contains a distance entry of 50 km and the vehicle's current odometer is 45250 km

#### Scenario: Preselected way
- **WHEN** the user opens the log event form
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
When a previous known odometer exists at the entry's date and time, the system SHALL refuse a new odometer count that is not
higher than it, showing an error on the odometer field that names the previous known odometer, and SHALL NOT save the entry.

#### Scenario: Lower count
- **WHEN** the previous known odometer is 45230 km and the user types 45100 as the new odometer and saves
- **THEN** the system shows an error naming 45,230 km, stays on the form and adds nothing

#### Scenario: Equal count
- **WHEN** the previous known odometer is 45230 km and the user types 45230 as the new odometer and saves
- **THEN** the system shows the same error and adds nothing

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field: while it is empty, the form's Save action SHALL be disabled
and SHALL NOT be tappable. A trip distance SHALL be more than zero: once the field is non-empty, the system SHALL
show an error on the field and SHALL NOT save the entry when a typed trip distance is zero.

#### Scenario: Empty field
- **WHEN** the active field has nothing typed
- **THEN** the form's Save action is disabled

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

### Requirement: The unit of an entry is selectable
The system SHALL let the user choose the unit of the entry on the form: kilometers or miles, and separately whether tenths are
included. The form SHALL preselect the unit family of the vehicle's odometer unit ("Kilometers" and "Kilometers with 100 m"
are kilometers, "Miles" and "Miles with tenths" are miles) and the tenths choice remembered for the vehicle, or, when none
was remembered yet, the tenths of the vehicle's odometer unit (the two tenths units include tenths).
Both fields of the form SHALL be entered in the chosen unit, using the same microwave-style number field as the initial
odometer, so whole-number entries and entries with one decimal are both possible whatever the vehicle's unit. Changing the
unit SHALL keep the digits typed, as it does when adding a vehicle. An entry logged in another unit than the vehicle's SHALL
be converted, and SHALL be shown in the vehicle's unit everywhere. The system SHALL remember the tenths choice per vehicle:
saving an entry SHALL keep the tenths choice used for that vehicle, and the next form for that vehicle SHALL preselect it.
The choice of kilometers or miles is not remembered and starts as the vehicle's.

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

#### Scenario: The tenths choice is remembered for the vehicle
- **WHEN** the user logs a distance for a vehicle with the unit "Kilometers" with tenths included and saves, and later opens the form for that vehicle again
- **THEN** tenths are included on the new form, although the vehicle's odometer unit has no tenths

#### Scenario: Turning tenths off is remembered too
- **WHEN** the user logs a distance for a vehicle with the unit "Miles with tenths" with tenths not included and saves, and later opens the form for that vehicle again
- **THEN** tenths are not included on the new form

#### Scenario: Other vehicles are not affected
- **WHEN** the tenths choice was remembered for one vehicle and the user opens the form for another vehicle
- **THEN** the form for the other vehicle starts with the tenths of that vehicle's own odometer unit, unless one was remembered for it

#### Scenario: A choice that is not saved is not remembered
- **WHEN** the user turns tenths on, leaves the form without saving, and opens the form for the same vehicle again
- **THEN** the form starts with the choice that was remembered before, or the tenths of the vehicle's odometer unit if there was none

#### Scenario: The remembered choice survives a restart
- **WHEN** a tenths choice was remembered for a vehicle, the app is closed completely and opened again
- **THEN** the form for that vehicle still starts with that choice

#### Scenario: The unit family is not remembered
- **WHEN** the user logs a distance in miles for a vehicle with the unit "Kilometers" and saves, and later opens the form for it again
- **THEN** kilometers is selected

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
`vehicle-log` capability specifies. The date SHALL be shown with the day of the week of the selected date, named in the device's
language, and the time SHALL be shown and picked in 12-hour form (with the device's AM and PM markers) or 24-hour form according
to the system setting.

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

#### Scenario: The day of the week is shown
- **WHEN** the form shows the date 2026-09-20 and the device language is English
- **THEN** the date shows "Sunday" as well, and after the date is changed to 2026-09-21 it shows "Monday"

#### Scenario: The day of the week follows the device language
- **WHEN** the form shows the date 2026-09-20 and the device language is Finnish
- **THEN** the date shows "sunnuntai"

#### Scenario: A 24-hour system setting
- **WHEN** the system uses the 24-hour format and the form shows the time 16:30
- **THEN** the time is shown as "16:30" and the time picker is a 24-hour picker

#### Scenario: A 12-hour system setting
- **WHEN** the system uses the 12-hour format and the form shows the time 16:30
- **THEN** the time is shown as "4:30 PM" (with the device's PM marker) and the time picker is a 12-hour picker with AM and PM

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
previous known odometer at a later time, unless an odometer anchor precedes it (see below).

#### Scenario: Trip distance before the initial odometer
- **WHEN** a vehicle was added with an initial odometer of 45200 km and the user logs a trip distance of 30 km dated a week before the initial odometer event
- **THEN** the log contains the entry at that earlier time and the vehicle's current odometer is still 45200 km

### Requirement: A new odometer count without a known odometer is saved as an odometer anchor
When the user chooses "New odometer" and no odometer is known at the entry's date and time, the system SHALL save the typed
count as an odometer anchor event, and SHALL NOT save a distance event. An odometer anchor is an odometer-setting event: it
sets the odometer to the typed count at the entry's time, and it takes part in the previous known odometer and the current
odometer like the initial odometer event does (the latest odometer-setting event wins, and distances after it add to it). The
form SHALL say that no odometer is known at that time and that the count will be saved as a new odometer starting point, SHALL
show no distance, and SHALL allow saving once a count is typed. The count SHALL be entered and unit-converted like any other,
the future check SHALL apply, and a count of zero SHALL be accepted. While the field is empty, the form's Save action SHALL be
disabled, as it is for a trip distance. The tenths choice used SHALL be remembered like for a distance entry.

#### Scenario: New odometer before the initial odometer
- **WHEN** a vehicle was added with an initial odometer of 45200 km, and the user chooses "New odometer", a time a week before the initial odometer event, types 44000 and saves
- **THEN** the log contains an odometer anchor of 44,000 km at that time and no distance event, and the vehicle's current odometer is still 45200 km

#### Scenario: The form explains it
- **WHEN** the user chooses "New odometer" and a time before the initial odometer event
- **THEN** the form says that no odometer is known at that time and that the count will be saved as a new odometer starting point, shows no distance, and saving is possible once a count is typed

#### Scenario: Later entries build on the anchor
- **WHEN** an odometer anchor of 44000 km exists a week before the initial odometer event, and the user chooses "New odometer" for a time three days before the initial odometer event
- **THEN** the form shows a previous known odometer of 44,000 km

#### Scenario: Trip distances after the anchor count up to the initial odometer
- **WHEN** an odometer anchor of 44000 km exists a week before the initial odometer event, a trip distance of 30 km is logged three days before it, and the user chooses "New odometer" for two days before it
- **THEN** the form shows a previous known odometer of 44,030 km

#### Scenario: An anchor at a time where an odometer is known is not created
- **WHEN** an odometer is known at the entry's time and the user chooses "New odometer" and types a higher count
- **THEN** a distance event is saved, as usual

#### Scenario: Nothing typed
- **WHEN** the user chooses "New odometer" with no odometer known at the time and nothing is typed
- **THEN** the form's Save action is disabled

### Requirement: Logging only adds to the log
The system SHALL record each saved distance entry as a new event and SHALL NOT change or remove any existing event
or any stored total, **except the one explicit edit action `event-details`'s "A 'Distance' or 'Odometer reading'
event's note can be edited" requirement adds** (changing or clearing an already-saved event's note from its details
screen). Saving a new entry SHALL either add it or add nothing.

#### Scenario: Earlier events are untouched
- **WHEN** the user logs a distance entry
- **THEN** every event that was in the log before is still there with the same values

#### Scenario: Editing a note is the one exception
- **WHEN** the user edits a previously logged event's note through its details screen
- **THEN** the log's other events, and the edited event's other fields, are unchanged

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

### Requirement: A distance can be logged from the Home screen
The system SHALL make the Home screen's Log event action (an icon with no visible text, named "Log event" for a screen reader; `app-shell`, "Home screen offers the main actions") open the log event form when the user has at least one vehicle. With no vehicle the action SHALL stay a disabled Material 3 component, exactly as the other not-yet-available Home screen actions are (`app-shell`, "Home screen offers the main actions"), and SHALL NOT react to a tap. Saving a valid entry SHALL return to the Home screen; leaving the form without saving SHALL add nothing.

#### Scenario: Open the form from the Home screen
- **WHEN** the user has a vehicle and taps the Home screen's Log event action
- **THEN** the log event form is displayed with a vehicle selector at the top

#### Scenario: No vehicle
- **WHEN** the user has no vehicle and taps the Home screen's Log event action
- **THEN** nothing happens, and the action is shown as disabled

#### Scenario: Save from the Home screen route
- **WHEN** the user opened the form from the Home screen, enters a valid entry and saves
- **THEN** the Home screen is displayed, and the entry is in the chosen vehicle's log

#### Scenario: Leave without saving
- **WHEN** the user opened the form from the Home screen and leaves it without saving
- **THEN** the Home screen is displayed and no entry is added

### Requirement: The vehicle is chosen with a selector when logging starts from the Home screen
When the log event form is opened from the Home screen, the system SHALL show a vehicle selector at the top of the form: a dropdown that shows the chosen vehicle (its picture or icon and its name) and lists every vehicle the user has, in the order of the vehicle list. Choosing a vehicle SHALL make the form
about that vehicle: the previous known odometer, the checks of a new odometer count and the log the entry will join are that vehicle's, and the unit starts as that vehicle's (its unit family and its remembered choice of tenths). The digits already typed, the way (trip distance or new odometer), the date, the time, the time zone and any pending note SHALL be kept when the vehicle is changed, and the typed digits are converted to the new unit as when the unit is changed by hand. The chosen vehicle SHALL survive a rotation of the device and the restart of the process. When the form is opened from a vehicle's details screen the selector SHALL NOT be shown and the vehicle SHALL be that vehicle.

#### Scenario: The selector lists the vehicles
- **WHEN** the user has vehicles named "van", "Bike" and "Family car" and opens the form from the Home screen and opens the selector
- **THEN** it lists "Bike", "Family car" and "van", in that order

#### Scenario: Choose another vehicle
- **WHEN** the user has typed a trip distance for "Family car" and chooses "Bike" in the selector
- **THEN** the form is about "Bike": its previous known odometer is shown, the typed digits are still there, and saving adds the entry to "Bike"

#### Scenario: The unit follows the chosen vehicle
- **WHEN** the user chooses a vehicle whose odometer is in miles after one whose odometer is in kilometers
- **THEN** the unit starts as miles (with that vehicle's remembered choice of tenths) and the typed digits are kept

#### Scenario: From the details screen there is no selector
- **WHEN** the user taps "Log event" on a vehicle's details screen
- **THEN** the form has no vehicle selector and is about that vehicle

#### Scenario: The choice survives a rotation
- **WHEN** the user has chosen a vehicle in the selector and rotates the device
- **THEN** the form shows the same vehicle and what was typed

#### Scenario: A pending note survives a vehicle change
- **WHEN** the user has typed a note, then chooses another vehicle in the selector
- **THEN** the note is still pending for the entry, now about the newly chosen vehicle

### Requirement: The vehicle last logged for is remembered apart from the log
The system SHALL remember which vehicle the user last saved an entry for, and SHALL start the selector on it. The memory SHALL be stored separately from the events (in the application's own storage, written in the same transaction as the entry that causes it) and SHALL NOT be worked out from the events' data: an entry
dated earlier than the entries of other vehicles still makes its vehicle the remembered one, because it was the last one logged. An entry saved from a vehicle's details screen also sets it. When nothing is remembered (a new install, an update from a version without the memory) or the remembered vehicle does not exist, the selector SHALL start on the first vehicle in the order of the vehicle list. The
memory SHALL survive closing the app, work without a network, and SHALL NOT be filled from the log when it is first created.

#### Scenario: The selector starts on the last vehicle logged for
- **WHEN** the user saves an entry for "Bike" and later opens the form from the Home screen
- **THEN** the selector shows "Bike"

#### Scenario: A backdated entry counts as the last
- **WHEN** the user saves an entry for "Family car" dated today and then saves an entry for "Bike" dated last month, and opens the form from the Home screen
- **THEN** the selector shows "Bike", because it is the vehicle last logged for

#### Scenario: Logging from the details screen counts
- **WHEN** the user saves an entry from the details screen of "van" and later opens the form from the Home screen
- **THEN** the selector shows "van"

#### Scenario: Nothing remembered
- **WHEN** the user has vehicles "van" and "Bike" and has never saved an entry with the memory in place
- **THEN** the selector starts on "Bike", the first by name

#### Scenario: The remembered vehicle is gone
- **WHEN** the remembered vehicle is not among the vehicles
- **THEN** the selector starts on the first vehicle by name

#### Scenario: The memory survives a restart
- **WHEN** the user saves an entry for "Bike", closes the app completely and opens the form from the Home screen
- **THEN** the selector shows "Bike"

#### Scenario: A failed save does not change the memory
- **WHEN** saving an entry fails
- **THEN** the remembered vehicle is what it was

### Requirement: The kind of event is chosen
The system SHALL show a "Kind" selector at the top of the log event form: a dropdown, in the same style as the vehicle selector, listing the kinds of event the form can log — today only "Distance", which SHALL be selected. Since there is only one kind today, the selector SHALL be a disabled Material 3 component (shown in Material's disabled colors, exactly as the Home screen's not-yet-available actions are) and SHALL NOT react to a tap; it becomes usable once a later change adds a second kind. When the form is opened from the Home screen the Kind selector SHALL share one row with the vehicle selector, the two of equal width and height, the Kind selector first (on the left). When the form is opened from a vehicle's details screen (where there is no vehicle selector) the Kind selector SHALL take the row alone.

#### Scenario: The kind selector is shown on both routes, disabled
- **WHEN** the user opens the log event form, from the Home screen or from a vehicle's details screen
- **THEN** a "Kind" selector is shown at the top of the form, showing "Distance" selected and disabled

#### Scenario: Sharing the row with the vehicle selector
- **WHEN** the form is opened from the Home screen
- **THEN** the Kind selector and the vehicle selector are shown side by side, each half the row's width and the same height, the Kind selector on the left

#### Scenario: Alone from the details screen
- **WHEN** the form is opened from a vehicle's details screen
- **THEN** the Kind selector is shown alone, the full width of the row, and no vehicle selector is shown

#### Scenario: Tapping the disabled selector does nothing
- **WHEN** the user taps the Kind selector
- **THEN** nothing happens: it does not open, and the form is unchanged

### Requirement: A note can be added to the log event form
The system SHALL show a note element on the log event form, below the trip distance or new odometer field and above
the Save action, styled as a Material 3 outlined text field — matching the look of the form's other tap-to-choose
fields (the Kind and Vehicle selectors) — with a "Note" label. Before a note is pending for the entry, the element
SHALL show the field styled as empty, with "Add a note..." shown where the field's content goes. Once a note is
pending, the element SHALL show its text there instead, at most two rendered lines (whether the break is a wrap or a
line feed the user typed), end-ellipsized. Tapping the element SHALL open a full-screen note editor. The note element
SHALL NOT become an editable text field on tap: no text cursor, no software keyboard, and no in-place editing SHALL
appear, and it SHALL NOT show the "Discard" action either; every edit goes through the full-screen editor.

#### Scenario: No note yet
- **WHEN** the user opens the log event form and has typed nothing in the note editor
- **THEN** the note element shows "Add a note..."

#### Scenario: A short note
- **WHEN** a one-line note is pending for the entry
- **THEN** the note element shows that line in place of the prompt

#### Scenario: A long note is truncated to two lines
- **WHEN** a pending note is longer than two rendered lines, whether from wrapping or from line breaks the user typed
- **THEN** the note element shows only its first two rendered lines, the second ending with an ellipsis

#### Scenario: Opening the editor
- **WHEN** the user taps the note element
- **THEN** the full-screen note editor opens

#### Scenario: The element looks like the form's other fields
- **WHEN** the user opens the log event form
- **THEN** the note element is shown as an outlined field with a "Note" label, in the same visual style as the Kind and Vehicle selectors

#### Scenario: Tapping does not turn it into an editable field
- **WHEN** the user taps the note element
- **THEN** no text cursor or software keyboard appears on the log event form, and the full-screen note editor opens instead

### Requirement: The full-screen note editor
The system SHALL open a full-screen editor with a single multi-line text field, seeded with whatever note is
currently pending for the entry (empty when none is pending). Back navigation — the editor's own back action, the
system back gesture or button — SHALL attach the field's current text to the entry as its pending note and return to
the log event form; text that is empty or only whitespace SHALL attach as no note (clearing any note that was
pending before the editor opened). The editor SHALL also offer an explicit "Discard" action that returns to the log
event form without attaching anything, leaving the note that was pending before the editor opened exactly as it was.

#### Scenario: Editor starts with the pending note
- **WHEN** the user opens the editor while a note is already pending for the entry
- **THEN** the text field starts with that note's text, ready to continue editing

#### Scenario: Back attaches what was typed
- **WHEN** the user types a note in the editor and navigates back (the toolbar action, the system gesture or the system back button)
- **THEN** the log event form is shown again with that text pending for the entry

#### Scenario: Attaching blank text clears the note
- **WHEN** the user clears the text in the editor (or leaves it as only whitespace) and navigates back
- **THEN** the log event form is shown again with no note pending for the entry

#### Scenario: Discard drops the session's edits
- **WHEN** the user changes the text in the editor and taps "Discard"
- **THEN** the log event form is shown again with the note that was pending before the editor opened, unchanged

### Requirement: A pending note can be removed
While a note is pending for the entry, the system SHALL show a trash-can action on the note element, alongside the
tap-to-edit action. Tapping it SHALL show a confirmation dialog before removing the note. Confirming SHALL clear the
pending note, after which the note element reverts to prompting "Add a note...". Dismissing or cancelling the dialog
SHALL leave the pending note unchanged. The trash-can action SHALL NOT be shown while no note is pending.

#### Scenario: The action appears once a note is pending
- **WHEN** a note is pending for the entry
- **THEN** the note element shows a trash-can action

#### Scenario: No action without a note
- **WHEN** no note is pending for the entry
- **THEN** the note element shows no trash-can action

#### Scenario: Confirming removes the note
- **WHEN** the user taps the trash-can action and confirms the dialog
- **THEN** the pending note is cleared and the note element prompts "Add a note..." again

#### Scenario: Cancelling keeps the note
- **WHEN** the user taps the trash-can action and cancels or dismisses the dialog
- **THEN** the pending note is unchanged and still shown on the note element

### Requirement: A pending note is saved with the event
Saving a valid log event form SHALL store the pending note, if any, with the new event (whichever the form saves: a
distance entry or an odometer anchor). Leaving the form without saving SHALL discard the pending note along with the
rest of the unsaved entry, as it already does for every other field on the form.

#### Scenario: The note is saved with the entry
- **WHEN** the user types a note, enters a valid distance and saves
- **THEN** the log contains the new entry together with that note

#### Scenario: No note means nothing extra is stored
- **WHEN** the user saves a valid entry without ever adding a note
- **THEN** the log contains the new entry with no note

#### Scenario: Leaving the form drops the note too
- **WHEN** the user types a note and leaves the form without saving
- **THEN** no entry is added and the note is not stored anywhere
