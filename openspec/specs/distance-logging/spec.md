# distance-logging Specification

## Purpose
Lets users log driving as distance entries: from the vehicle details screen they record a distance travelled, either as
the trip meter distance or as the new odometer count, at a chosen date, time and time zone. The entries build the vehicle's log and
its current odometer.

## Requirements

### Requirement: Log an event from the vehicle details screen
The system SHALL offer a "Log event" action on the vehicle details screen that opens a form for one distance entry.
Saving a valid entry SHALL return to the details screen, where the entry appears in the recent events and the current
odometer includes it. Leaving the form without saving SHALL add nothing. The form's dismiss action, top-left in its
top app bar, SHALL be a close "X" (a Material full-screen dialog's dismiss icon), not a back arrow.

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
The system SHALL let the user choose, on the log event form, between "Trip distance", preselected, and "New odometer".
With "Trip distance" the number entered is the distance travelled. With "New odometer" it is the odometer count now,
and the distance logged is that count minus the previous known odometer at the entry's date and time (or, with no
odometer known then, an odometer anchor is saved instead). Each way SHALL keep its own typed number when the user
switches between them.

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

#### Scenario: Accepting a scanned odometer reading switches the way
- **WHEN** the form is on "Trip distance" and the user scans a photo, and accepts a candidate classified as an odometer reading
- **THEN** the form switches to "New odometer" and the field shows the accepted value

#### Scenario: Accepting a scanned trip reading switches the way
- **WHEN** the form is on "New odometer" and the user scans a photo, and accepts a candidate classified as a trip reading
- **THEN** the form switches to "Trip distance" and the field shows the accepted value

### Requirement: The previous known odometer
The system SHALL define the previous known odometer at a date and time as the reading of the vehicle's latest
odometer-setting event at or before it, plus the distances of all distance entries after that event up to and
including that time; events at the same time SHALL count in the order they were added. With "New odometer" chosen, the
form SHALL show the previous known odometer for the chosen moment and the distance that would be logged, updated as
the number or the moment changes.

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
When a previous known odometer exists at the entry's date and time, the system SHALL refuse a new odometer count equal
to it, showing an error on the odometer field that names it, and SHALL NOT save the entry. A lower count is not
refused outright: it needs confirmation when the entry is for now or for any moment at or after the vehicle's latest
logged event, and is saved without it when backdated before that event (the two requirements on a lower new odometer
count).

#### Scenario: Lower count
- **WHEN** the vehicle's current odometer is 45230 km and the user logs a new odometer count of 45100 for now (or any moment at or after the vehicle's latest logged event) and saves
- **THEN** the system asks the user to confirm before saving, rather than refusing outright — see "A lower new odometer count that would replace the vehicle's current odometer is saved only after confirmation"

#### Scenario: Lower count added to history
- **WHEN** the user logs a new odometer count for a date and time before the vehicle's latest logged event, and that count is lower than the odometer known at that earlier moment, and saves
- **THEN** the system saves it outright, without confirmation — see "A lower new odometer count added to history is saved without confirmation"

#### Scenario: Equal count
- **WHEN** the odometer known at the entry's date and time is 45230 km and the user types 45230 as the new odometer and saves
- **THEN** the system shows an error naming 45,230 km, stays on the form and adds nothing

### Requirement: A lower new odometer count that would replace the vehicle's current odometer is saved only after confirmation
When the user logs a new odometer count for now, or for any moment at or after the vehicle's latest logged event, that
is strictly lower than the vehicle's current odometer (an equal count is refused), and saves, the system SHALL show a
confirmation dialog naming both the typed count and the current odometer it would replace, and SHALL NOT save anything
until the user confirms. Cancelling or dismissing it SHALL save nothing and leave the form exactly as it was, the
typed count still there.

#### Scenario: Confirming saves it as an anchor
- **WHEN** the vehicle's current odometer is 45230 km, the user logs a new odometer count of 44000 for now, saves, and confirms the dialog
- **THEN** the log contains an odometer anchor of 44,000 km at that time, and the vehicle's current odometer is now 44,000 km

#### Scenario: The dialog names both values
- **WHEN** the vehicle's current odometer is 45230 km and the user logs a new odometer count of 44000 for now and saves
- **THEN** the confirmation dialog names both 44,000 km and 45,230 km

#### Scenario: Cancelling saves nothing
- **WHEN** the confirmation dialog is shown and the user cancels or dismisses it
- **THEN** nothing is saved, the form is shown exactly as it was, and the typed count (44,000) is still in the field

#### Scenario: Later entries build on the lower anchor
- **WHEN** a confirmed odometer anchor of 44000 km replaces a vehicle's current odometer of 45230 km, and the user later logs a trip distance of 30 km
- **THEN** the vehicle's current odometer is 44,030 km

### Requirement: A lower new odometer count added to history is saved without confirmation
When the user logs a new odometer count for a moment before the vehicle's latest logged event, lower than the odometer
known at that moment, the system SHALL save it outright as an odometer anchor at that moment (`vehicle-log`, "Odometer
anchor events"), without asking for confirmation: like any backdated anchor, it doesn't correct what the odometer
reads now. Later events SHALL be recalculated forward from it, as for a higher backdated anchor. An equal count is
still refused.

#### Scenario: A backdated lower count is saved without confirmation
- **WHEN** the vehicle's latest logged event is later than the moment the user is logging for, the odometer known at that earlier moment is 45230 km, and the user types 45100 and saves
- **THEN** the system saves an odometer anchor of 45,100 km at that moment without showing a confirmation dialog

### Requirement: A trip distance must be entered and above zero
The system SHALL require a number in the active field of a "Distance" entry: while it is empty, the form's Save action
SHALL be disabled (not tappable). A trip distance SHALL be more than zero: for a typed zero, the system SHALL show an
error on the field and SHALL NOT save the entry. This governs the "Distance" kind's own field only; a refueling's
optional mileage section does not gate Save (`refueling-logging`, "The fuel amount must be entered and above zero").

#### Scenario: Empty field
- **WHEN** the user has chosen "Distance" and the active field has nothing typed
- **THEN** the form's Save action is disabled

#### Scenario: Zero distance
- **WHEN** the user types 0 as the trip distance and saves
- **THEN** the system shows an error that the distance must be more than zero and adds nothing

#### Scenario: The error clears when the user types
- **WHEN** an error is shown on the field and the user types a digit
- **THEN** the error is no longer shown

#### Scenario: The required field is marked
- **WHEN** the user has chosen "Distance" and opens the log event form
- **THEN** the active field's label ("Trip distance" or "New odometer") carries a trailing "*", and a line near the Save action reads "* indicates a required field"

### Requirement: The unit of an entry is selectable
The system SHALL let the user choose the entry's unit on the form: kilometers or miles, and separately whether tenths
are included. It SHALL preselect the family of the vehicle's odometer unit ("Kilometers" and "Kilometers with 100 m"
are kilometers; "Miles" and "Miles with tenths" are miles). Both fields SHALL be entered in the chosen unit with the
initial odometer's microwave-style field, so whole and one-decimal entries are possible in any unit. Changing the unit
SHALL keep the digits typed.

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
The system SHALL show the entry's date, time and time zone on the form, each with a picker. The moment SHALL default
to when the form was opened, not when it is saved, and the zone to the device's current zone. The user SHALL be able
to choose any available time zone from a list searchable by name. The date and time picked are the wall-clock time in
the chosen zone: changing the zone keeps them and changes the instant they mean. The entry SHALL be stored with its
zone (`vehicle-log`).

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
When the user chooses "New odometer" and no odometer is known at the entry's date and time, the system SHALL save the
typed count as an odometer anchor event and SHALL NOT save a distance event. An anchor is an odometer-setting event:
it sets the odometer to the count at the entry's time and takes part in the previous known and the current odometer
like the initial odometer does (the latest odometer-setting event wins, and distances after it add to it).

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
The system SHALL make the Home screen's Log event action (an icon with no visible text, named "Log event" for a screen
reader; `app-shell`) open the log event form when the user has at least one vehicle. With no vehicle the action SHALL
stay a disabled Material 3 component, like the other not-yet-available Home screen actions, and SHALL NOT react to a
tap. Saving a valid entry SHALL return to the Home screen; leaving the form without saving SHALL add nothing.

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
When the log event form is opened from the Home screen, the system SHALL show a vehicle selector at its top: a
dropdown showing the chosen vehicle (its picture or icon, and its name) and listing every vehicle in the vehicle
list's order. Choosing a vehicle SHALL make the form about it: the previous known odometer, the checks of a new count
and the log the entry joins are that vehicle's. Opened from a vehicle's details screen, the form SHALL show no
selector and SHALL be about that vehicle.

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
The system SHALL remember which vehicle the user last saved an entry for, from the Home screen or a vehicle's details
screen, and SHALL start the selector on it. The memory SHALL be stored apart from the events (in the application's own
storage, written in the same transaction as the entry that sets it) and SHALL NOT be worked out from the events' data:
an entry dated before other vehicles' entries still makes its vehicle the remembered one.

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
The system SHALL show a "Kind" selector at the top of the log event form: a dropdown in the vehicle selector's style
listing the kinds the form can log, "Distance" and "Refueling" (`refueling-logging`), with "Distance" preselected. It
SHALL be an enabled Material 3 component whenever it lists more than one kind, and a disabled one (in Material's
disabled colors) that SHALL NOT react to a tap whenever it would list only one. Choosing a kind SHALL show that kind's
fields below it.

#### Scenario: The kind selector is shown on both routes, disabled
- **WHEN** the form offers only one kind of event (not the case today, since "Distance" and "Refueling" are both always offered)
- **THEN** the kind selector shows that one kind selected and disabled, on both routes the form can be opened from

#### Scenario: Distance is preselected
- **WHEN** the user opens the log event form
- **THEN** "Distance" is selected in the Kind selector and the Distance fields are shown

#### Scenario: Choosing Refueling switches the form
- **WHEN** the user opens the Kind selector and chooses "Refueling"
- **THEN** the form shows the refueling fields (`refueling-logging`, "Refueling can be logged from the log event form")
  in place of the Distance fields

#### Scenario: Sharing the row with the vehicle selector
- **WHEN** the form is opened from the Home screen
- **THEN** the Kind selector and the vehicle selector are shown side by side, each half the row's width and the same height, the Kind selector on the left

#### Scenario: Alone from the details screen
- **WHEN** the form is opened from a vehicle's details screen
- **THEN** the Kind selector is shown alone, the full width of the row, and no vehicle selector is shown

#### Scenario: Tapping the disabled selector does nothing
- **WHEN** the kind selector is disabled (because the form offers only one kind) and the user taps it
- **THEN** nothing happens: it does not open, and the form is unchanged

### Requirement: A note can be added to the log event form
The system SHALL show a note element on the log event form, below the trip distance or new odometer field and above
Save, as a Material 3 outlined text field labelled "Note", in the style of the form's Kind and Vehicle selectors. With
no note pending it SHALL look empty, with "Add a note..." as its content; with a note pending, its text, at most two
rendered lines (wrapped or typed breaks), end-ellipsized. Tapping it SHALL open a full-screen note editor.

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
The system SHALL open a full-screen editor with one multi-line text field, seeded with the note pending for the entry
(empty when none). Back navigation (the editor's back action, or the system back gesture or button) SHALL attach the
field's text to the entry as its pending note and return to the form; empty or whitespace-only text SHALL attach as no
note, clearing any pending one. An explicit "Discard" action SHALL return without attaching anything, leaving the
earlier pending note as it was.

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

### Requirement: Photos can be added to the log event form
The system SHALL show a photo element on the log event form, below the note element, for a "Distance" or "Odometer
reading" (new odometer count) entry; the "Initial odometer" event never offers one, as it never offers a note. It
SHALL show thumbnails of the photos attached (0 to 5) and, while fewer than 5 are attached, an "Add photo" action that
shows the system's chooser of image sources (`vehicle-picture`). With 5 attached, "Add photo" SHALL NOT be shown until
one is removed.

#### Scenario: No photos yet
- **WHEN** the user opens the log event form and has not attached any photo
- **THEN** the photo element shows only "Add photo"

#### Scenario: Add a photo
- **WHEN** the user taps "Add photo" and chooses a photo from the system chooser
- **THEN** the photo is added to the strip, with no crop screen shown

#### Scenario: The limit is reached
- **WHEN** the user has attached 5 photos to the entry being logged
- **THEN** the strip shows the 5 thumbnails and no "Add photo" action

#### Scenario: Removing one allows another
- **WHEN** the user has attached 5 photos and removes one
- **THEN** the strip shows "Add photo" again

#### Scenario: No photo element for the initial odometer
- **WHEN** a vehicle is added
- **THEN** its "Initial odometer" event offers no photo element

### Requirement: An attached photo can be removed while composing the event
The system SHALL show a remove action on each thumbnail in the photo strip. Tapping it SHALL show a confirmation
dialog before removing that photo. Confirming SHALL remove it from the strip; dismissing or cancelling SHALL leave
it attached.

#### Scenario: Remove a photo
- **WHEN** the user taps a thumbnail's remove action and confirms
- **THEN** that photo is no longer in the strip

#### Scenario: Cancel keeps the photo
- **WHEN** the user taps a thumbnail's remove action and cancels or dismisses the dialog
- **THEN** that photo is still in the strip

### Requirement: Attached photos are saved with the event
Saving a valid log event form SHALL store the attached photos (0 to 5), if any, with the new event. Leaving the form
without saving SHALL discard the attached photos along with the rest of the unsaved entry, as it already does for
every other field on the form. Attached photos SHALL be kept across a rotation or a process restart while the form
is open.

#### Scenario: Photos are saved with the entry
- **WHEN** the user attaches photos, enters a valid distance and saves
- **THEN** the log contains the new entry together with those photos

#### Scenario: No photos means nothing extra is stored
- **WHEN** the user saves a valid entry without ever attaching a photo
- **THEN** the log contains the new entry with no photo

#### Scenario: Leaving the form drops attached photos too
- **WHEN** the user attaches a photo and leaves the form without saving
- **THEN** no entry is added and the photo is not stored anywhere

#### Scenario: Photos survive a rotation
- **WHEN** the user has attached photos and rotates the device before saving
- **THEN** the form still shows those photos attached

### Requirement: Accepting a scanned reading switches the way
Accepting a candidate from the log event form's "Scan a reading" action (`odometer-ocr-capture`, "Accepting a candidate
sets the field and the way") SHALL set the active field's number and SHALL switch the way to match the candidate's
classification, the same as if the user had switched by hand and typed it.

#### Scenario: A scanned reading replaces the typed number
- **WHEN** the form is on "New odometer" with 45250 typed, and the user accepts a scanned candidate of 45260
  classified as an odometer reading
- **THEN** the form stays on "New odometer" and the field shows 45260

### Requirement: A confirmed lower count is saved as an odometer anchor
Confirming a lower new odometer count SHALL save it as an odometer anchor (`vehicle-log`, "Odometer anchor events"),
the same way a count is saved when no odometer was known before: it becomes the vehicle's current odometer going
forward, like any other anchor, with no constraint that it be higher than what it replaces.

#### Scenario: A confirmed lower count adds no distance
- **WHEN** the vehicle's current odometer is 45230 km, and the user logs a new odometer count of 44000 for now, saves
  and confirms
- **THEN** the log gains an odometer anchor of 44,000 km and no "Distance" event

### Requirement: The required field of a distance entry is marked
On a "Distance" entry, the active field's label ("Trip distance" or "New odometer") SHALL carry a trailing "*", and the
form SHALL show a line near its Save action explaining the convention: "* indicates a required field". A refueling's
optional mileage section's labels SHALL NOT carry it; the fuel amount's label does (`refueling-logging`).

#### Scenario: The mark follows the active field
- **WHEN** the user switches a "Distance" entry from "Trip distance" to "New odometer"
- **THEN** the "New odometer" label carries the trailing "*"

### Requirement: The tenths choice is remembered per vehicle
The log event form SHALL preselect the tenths choice remembered for the vehicle or, when none was remembered yet, the
tenths of the vehicle's odometer unit (the two tenths units include tenths). Saving an entry SHALL remember the tenths
choice used for that vehicle. The choice of kilometers or miles is not remembered and starts as the vehicle's.

#### Scenario: Saving in miles remembers only the tenths
- **WHEN** the user logs a distance in miles with tenths included for a vehicle with the unit "Kilometers", saves, and
  later opens the form for that vehicle again
- **THEN** kilometers is selected and tenths are included

### Requirement: An entry in another unit is converted
An entry logged in another unit than the vehicle's SHALL be converted, and SHALL be shown in the vehicle's unit
everywhere.

#### Scenario: Miles for a whole-kilometer vehicle
- **WHEN** the user logs a trip distance of 10 miles for a vehicle with the unit "Kilometers"
- **THEN** the entry is shown as "+16 km"

### Requirement: An entry's moment can be in the past but not in the future
The user SHALL be able to choose a moment in the past for an entry. The system SHALL refuse a moment later than the
time of saving, showing an error and adding nothing.

#### Scenario: A moment last year
- **WHEN** the user picks a date a year ago and saves a valid entry
- **THEN** the entry is saved at that date

### Requirement: The entry's date shows its weekday and its time follows the system's clock
The date of the entry SHALL be shown with the day of the week of the selected date, named in the device's language,
and its time SHALL be shown and picked in 12-hour form (with the device's AM and PM markers) or in 24-hour form,
following the system setting.

#### Scenario: Just after midnight in 12-hour form
- **WHEN** the system uses the 12-hour format and the form shows the time 00:15
- **THEN** the time is shown as "12:15 AM"

### Requirement: The form explains a new odometer starting point
When "New odometer" is chosen and no odometer is known at the entry's time, the form SHALL say that no odometer is
known at that time and that the count will be saved as a new odometer starting point, SHALL show no distance, and SHALL
allow saving once a count is typed. While the field is empty, the form's Save action SHALL be disabled, as for a trip
distance.

#### Scenario: The explanation goes away at a known time
- **WHEN** the form explains a new odometer starting point, and the user changes the time to one after the initial
  odometer
- **THEN** the form shows the previous known odometer and the distance instead

### Requirement: An odometer starting point is entered like any count
The count of a new odometer starting point SHALL be entered and unit-converted like any other count, the future check
SHALL apply to it, a count of zero SHALL be accepted, and the tenths choice used SHALL be remembered as for a distance
entry.

#### Scenario: A starting point of zero
- **WHEN** no odometer is known at the entry's time, and the user chooses "New odometer", types 0 and saves
- **THEN** the log contains an odometer anchor of 0 at that time

### Requirement: Changing the vehicle keeps what was entered
Changing the vehicle in the log event form's selector SHALL start the unit as the new vehicle's (its unit family and
its remembered choice of tenths), and SHALL keep the typed digits, converted to the new unit as when the unit is
changed by hand, the way, the date, the time, the time zone and any pending note. The chosen vehicle SHALL survive a
rotation of the device and a restart of the process.

#### Scenario: The date and time survive a vehicle change
- **WHEN** the user has picked yesterday at 08:30 and then chooses another vehicle in the selector
- **THEN** the form still shows yesterday at 08:30

### Requirement: The selector starts on the first vehicle when none is remembered
When no vehicle is remembered (a new install, an update from a version without the memory) or the remembered vehicle
does not exist, the selector SHALL start on the first vehicle in the vehicle list's order. The memory SHALL survive
closing the app, SHALL work without a network, and SHALL NOT be filled from the log when it is first created.

#### Scenario: An update does not fill the memory from the log
- **WHEN** the app is updated from a version without the memory, with vehicles "Bike" and "van" and the latest entry
  logged for "van"
- **THEN** the selector starts on "Bike", the first vehicle in the list's order

### Requirement: The Kind selector shares a row with the vehicle selector
When the log event form is opened from the Home screen, the Kind selector and the vehicle selector SHALL share one row,
of equal width and height, the Kind selector first (on the left). Opened from a vehicle's details screen, where there
is no vehicle selector, the Kind selector SHALL take the row alone.

#### Scenario: The row survives a rotation
- **WHEN** the form is opened from the Home screen and the device is rotated
- **THEN** the Kind and vehicle selectors are still side by side, each half the row's width and the same height

### Requirement: The note element is never edited in place
The note element SHALL NOT become an editable text field when tapped: no text cursor, software keyboard or in-place
editing SHALL appear on the log event form, and the element SHALL NOT show the "Discard" action; every edit SHALL go
through the full-screen note editor.

#### Scenario: Tapping with a note pending
- **WHEN** a note is pending and the user taps the note element
- **THEN** the full-screen note editor opens with that note, and no keyboard appears on the log event form

### Requirement: An event photo is added without a crop
A photo chosen through the log event form's "Add photo" SHALL be added to the strip without a crop step: unlike a
vehicle's picture, an event photo is looked at for its content, and cropping it to a square risks cutting off what it
was meant to capture.

#### Scenario: A photo is attached uncropped
- **WHEN** the user adds a portrait (3:4) photo through "Add photo"
- **THEN** no crop screen is shown, and the photo is attached as it is, 3:4, not cut to a square

### Requirement: A refused camera is shown on the photo element
When the user chooses the camera from "Add photo" and camera access is refused, now or before, the system SHALL open
no camera, keep the attached photos as they were, and show the missing-permission message (`app-shell`) on the photo
element. Leaving the chooser, the camera or a photo app without providing a photo SHALL change nothing and SHALL NOT
show that message. The same holds on the event edit screen, which uses this photo element.

#### Scenario: Camera access refused when adding a photo
- **WHEN** the user taps "Add photo", chooses the camera, and camera access is refused (now or before)
- **THEN** no camera opens, the attached photos stay as they were, and the photo element shows the missing-permission message with its "Open settings" action

#### Scenario: Allowed from the message
- **WHEN** the photo element shows the missing-permission message, and the user allows camera access in the device settings and comes back
- **THEN** the message is gone, and choosing the camera from "Add photo" opens it

#### Scenario: Leaving the chooser without a photo
- **WHEN** the user taps "Add photo" and leaves the chooser, the camera or a photo app without providing a photo
- **THEN** the attached photos stay as they were, and no missing-permission message is shown
