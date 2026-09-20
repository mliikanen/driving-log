# Spec Delta

## Purpose

Lets users define the vehicles they log against: add them, choosing how each vehicle's odometer counts, see them in
a list, view a vehicle's details and change its name and license plate. Vehicles are kept on the device so they
persist across restarts and work offline.

Odometer values in the scenarios are written with plain digits and a period; on screen they use the device
locale's separators, as specified in the requirement on odometer display. Odometer readings are entered with the
odometer field, so "entering the odometer 45200" means typing the digits 4, 5, 2, 0 and 0.

## ADDED Requirements

### Requirement: Add a vehicle
The system SHALL allow the user to add a vehicle by entering a name (required), a license plate (optional), choosing
the vehicle's odometer unit and entering its current odometer reading in the odometer field (optional, defaulting
to 0). After a successful
save the system SHALL return to the vehicle list showing the new vehicle. The system SHALL allow several vehicles to
share the same name or license plate.

#### Scenario: Add a vehicle with all fields
- **WHEN** the user enters the name "Family car" and the plate "ABC-123", chooses the unit "Kilometers", types the odometer 45200 and saves
- **THEN** the vehicle "Family car" with plate "ABC-123" appears in the vehicle list
- **AND** its details screen shows a current odometer of 45200 km

#### Scenario: Add a vehicle with only a name
- **WHEN** the user enters only the name "Van", leaves the preselected unit unchanged and saves
- **THEN** the vehicle "Van" appears in the vehicle list without a license plate
- **AND** its details screen shows a current odometer of 0 in the preselected unit

#### Scenario: Name is required
- **WHEN** the user tries to save a vehicle with an empty name
- **THEN** the system shows an error on the name field, stays on the add screen and does not save anything

#### Scenario: Duplicate names are allowed
- **WHEN** the user adds a vehicle named "Van" while another vehicle named "Van" already exists
- **THEN** both vehicles appear in the vehicle list

#### Scenario: Cancel adding
- **WHEN** the user leaves the add screen without saving
- **THEN** no vehicle is added

### Requirement: Odometer unit is a per-vehicle setting with a default from the device region
The system SHALL let the user choose one of four odometer units for each vehicle when adding it: "Kilometers" (a
kilometer odometer shown in whole kilometers), "Kilometers with 100 m" (a kilometer odometer that also shows tenths
of a kilometer, that is, hundreds of meters), "Miles" (shown in whole miles) or "Miles with tenths" (a mile odometer
that also shows tenths of a mile). The add screen SHALL preselect a default unit derived from the device's region:
"Miles" when the region is the United States, the United Kingdom, Liberia or Myanmar, and "Kilometers" for every other
or unknown region. The user SHALL be able to change the selection before saving. The unit SHALL determine how the
vehicle's odometer readings are shown everywhere in the app, and SHALL NOT be changed after the vehicle has been added
in this change.

#### Scenario: Default unit in the United States
- **WHEN** the device region is the United States and the user opens the add vehicle screen
- **THEN** the unit "Miles" is preselected

#### Scenario: Default unit in Finland
- **WHEN** the device region is Finland and the user opens the add vehicle screen
- **THEN** the unit "Kilometers" is preselected

#### Scenario: Unknown region
- **WHEN** the device region cannot be determined and the user opens the add vehicle screen
- **THEN** the unit "Kilometers" is preselected

#### Scenario: Changing the unit
- **WHEN** the user selects "Kilometers with 100 m" instead of the preselected unit and saves a vehicle
- **THEN** the vehicle is saved with the unit "Kilometers with 100 m" and its readings are shown in that unit

#### Scenario: Four units are offered
- **WHEN** the user opens the add vehicle screen
- **THEN** the units "Kilometers", "Kilometers with 100 m", "Miles" and "Miles with tenths" are offered

### Requirement: Whitespace is trimmed from name and license plate
The system SHALL remove leading and trailing whitespace from the vehicle name and the license plate when a vehicle
is added or edited, before validating and saving. Whitespace inside the text SHALL be kept. A name that is empty
after trimming SHALL be rejected as a missing name. A license plate that is empty after trimming SHALL be treated as
no license plate.

#### Scenario: Name and plate are trimmed on add
- **WHEN** the user saves a new vehicle with a chosen unit, the name "  Family car " and the plate " ABC-123  "
- **THEN** the vehicle is shown and stored with the name "Family car" and the plate "ABC-123"

#### Scenario: Whitespace-only name is rejected
- **WHEN** the user chooses a unit and tries to save a vehicle whose name consists only of spaces
- **THEN** the system treats the name as missing, shows the name error and does not save anything

#### Scenario: Whitespace-only plate means no plate
- **WHEN** the user saves a vehicle with a chosen unit, the name "Van" and a plate consisting only of spaces
- **THEN** the vehicle is saved without a license plate

#### Scenario: Inner whitespace is kept
- **WHEN** the user saves a vehicle with a chosen unit and the name " My  old car "
- **THEN** the vehicle name is "My  old car", with both inner spaces intact

### Requirement: Odometer readings are entered in a microwave-style number field
The system SHALL let the user enter the odometer reading of a new vehicle in a field that uses the device's number
keyboard (digits only) and works like a microwave oven's timer: each digit typed enters at the right-hand end of the
displayed value and shifts the earlier digits one place to the left. For the units "Kilometers" and "Miles" the last
digit is a whole unit. For the units "Kilometers with 100 m" and "Miles with tenths" the last digit is a tenth, and
the decimal separator is added automatically. The field SHALL always show the current value in the vehicle's unit as
specified for odometer readings, so it SHALL show the decimal separator, and any thousands separator, of the device's
current locale (for example a comma in Finnish and a period in English (United States)), even though the user never
types a separator. The value SHALL be right-aligned in the field: new digits appear at the right edge of the field
and push the earlier digits to the left, with the unit abbreviation kept fixed at the end of the field so that the
right-hand end of the number does not move. The field starts at 0 (or 0.0 for the tenths units). Deleting a digit
with the keyboard's backspace SHALL remove the last digit entered, so that typing then deleting a digit restores the
earlier value, and a clear control in the field SHALL set the value back to 0. Typing 0 while the value is 0 SHALL
leave it at 0. Characters that are not digits, such as a comma, a period or a minus sign, whether typed or pasted, SHALL
be ignored. The value SHALL have at most 7 whole digits, and digits that would exceed that SHALL be ignored. Because
only digits can be entered, an invalid reading cannot be entered.

#### Scenario: Whole-number unit
- **WHEN** the unit is "Kilometers" and the user types 1, then 2, then 3
- **THEN** the field shows 1 km, then 12 km, then 123 km

#### Scenario: Tenths unit
- **WHEN** the unit is "Kilometers with 100 m" or "Miles with tenths" and the user types 1, then 2, then 3
- **THEN** the field shows 0.1, then 1.2, then 12.3 (with the unit's abbreviation, for an English (United States) device locale)

#### Scenario: Digits fill from the right edge
- **WHEN** the user types 1, then 2, then 3 with the unit "Kilometers with 100 m"
- **THEN** the number is right-aligned against the unit abbreviation at the right edge of the field each time, and each new digit appears at the right-hand end while the earlier digits move left

#### Scenario: Decimal separator follows the device locale
- **WHEN** the unit is "Miles with tenths", the device locale is Finnish and the user types 1, then 2, then 3
- **THEN** the field shows 0,1 mi, then 1,2 mi, then 12,3 mi

#### Scenario: Number keyboard
- **WHEN** the user focuses the odometer field
- **THEN** the device's number keyboard is shown, not the full text keyboard

#### Scenario: Initial value
- **WHEN** the user opens the add vehicle screen
- **THEN** the field shows 0 in the preselected unit, or 0.0 when that unit has tenths, and saving without typing anything saves a reading of 0

#### Scenario: Backspace restores the earlier value
- **WHEN** the unit has tenths, the field shows 1.2, the user types 3 and then presses backspace
- **THEN** the field shows 12.3 and then 1.2 again

#### Scenario: Typing, deleting to zero and typing again
- **WHEN** the unit has tenths and the user types 1, 2, presses backspace twice, then types 2, 3, 0
- **THEN** after each key the field shows 0.1, 1.2, 0.1, 0.0, 0.2, 2.3, 23.0

#### Scenario: Backspace removes digits one at a time
- **WHEN** the unit has tenths, the field shows 12.3, and the user presses backspace three times
- **THEN** the field shows 1.2, then 0.1, then 0.0

#### Scenario: Backspace at zero
- **WHEN** the field shows 0.0 (or 0) and the user presses backspace
- **THEN** the field still shows 0.0 (or 0)

#### Scenario: Backspace on a whole-number unit
- **WHEN** the unit is "Kilometers", the field shows 123 km, and the user presses backspace
- **THEN** the field shows 12 km

#### Scenario: Clear
- **WHEN** the field shows 12.3 and the user uses the clear control
- **THEN** the field shows 0.0

#### Scenario: Zero at zero
- **WHEN** the value is 0 and the user types 0 three times
- **THEN** the field still shows 0

#### Scenario: Maximum length
- **WHEN** the unit is "Kilometers" and the field already shows 9999999 km, and the user types 1
- **THEN** the field still shows 9999999 km

#### Scenario: Non-digits are ignored
- **WHEN** the field shows 12.3 and the user types or pastes a comma, a period or a minus sign
- **THEN** the field still shows 12.3

#### Scenario: Pasting digits
- **WHEN** the unit is "Kilometers with 100 m", the field shows 0.0 and the user pastes "123"
- **THEN** the field shows 12.3

#### Scenario: Changing the unit keeps the value
- **WHEN** the field shows 123 km and the user changes the unit to "Kilometers with 100 m"
- **THEN** the field shows 123.0 km
- **AND** changing the unit back to "Kilometers" shows 123 km

#### Scenario: Changing to a whole-number unit rounds
- **WHEN** the unit has tenths, the field shows 12.6, and the user changes the unit to "Kilometers"
- **THEN** the field shows 13 km

### Requirement: Odometer readings are shown in the vehicle's unit using the device locale
The system SHALL show every odometer reading in the vehicle's unit, followed by the unit abbreviation "km" or "mi",
using the decimal separator and the thousands separator of the device's current locale. "Kilometers" and "Miles"
readings SHALL be rounded to the nearest whole number, halves rounding up, and SHALL have no decimal digits.
"Kilometers with 100 m" and "Miles with tenths" readings SHALL be rounded to the nearest tenth of the unit and always
show exactly one decimal digit.
Readings SHALL be shown again with the new locale's separators when the device locale changes.

#### Scenario: English (US) locale
- **WHEN** a vehicle with the unit "Kilometers with 100 m" has a reading of 45200.3 and the device locale is English (United States)
- **THEN** the reading is shown as "45,200.3 km"

#### Scenario: Finnish locale
- **WHEN** a vehicle with the unit "Kilometers with 100 m" has a reading of 45200.3 and the device locale is Finnish
- **THEN** the reading is shown as "45 200,3 km", with a no-break space between the thousands and the hundreds

#### Scenario: Whole-number units round
- **WHEN** a vehicle with the unit "Kilometers" has a prepared reading of 123 500 meters
- **THEN** its reading is shown as "124 km"

#### Scenario: Tenths are always shown
- **WHEN** a vehicle with the unit "Kilometers with 100 m" has a reading of 0
- **THEN** the reading is shown as "0.0 km" with an English (United States) locale and "0,0 km" with a Finnish locale

#### Scenario: Miles
- **WHEN** a vehicle with the unit "Miles" has a reading of 45200 and the device locale is English (United States)
- **THEN** the reading is shown as "45,200 mi"

#### Scenario: Miles with tenths
- **WHEN** a vehicle with the unit "Miles with tenths" has a reading of 45200.3 and the device locale is English (United States)
- **THEN** the reading is shown as "45,200.3 mi"

#### Scenario: Miles with tenths under a Finnish locale
- **WHEN** a vehicle with the unit "Miles with tenths" has a reading of 0 and the device locale is Finnish
- **THEN** the reading is shown as "0,0 mi"

#### Scenario: Device locale changes
- **WHEN** the device locale changes from English (United States) to Finnish while a vehicle's details are shown
- **THEN** the reading is shown again with the Finnish separators

### Requirement: Stored data does not depend on the device locale
The system SHALL store vehicles, odometer readings and units in a form that does not depend on the device locale, so
that data added under one locale is shown correctly under any other locale, and the same input text always gives the
same stored value.

#### Scenario: Same data under two locales
- **WHEN** the user adds a vehicle with the unit "Kilometers with 100 m" by typing 1, 2, 3, 5 while the device locale is Finnish, and later views it while the device locale is English (United States)
- **THEN** the reading is shown as "123.5 km" with the English (United States) separators, the same value that was shown as "123,5 km"

### Requirement: Vehicle list
The system SHALL show every vehicle the user has added on the Home screen, ordered by name without regard to letter
case, and SHALL show each vehicle's name and, when it has one, its license plate. When there are no vehicles the
system SHALL show an empty state that invites the user to add a vehicle. The Home screen SHALL offer an action to
add a vehicle.

#### Scenario: Vehicles are listed alphabetically
- **WHEN** the user has vehicles named "van", "Bike" and "Family car"
- **THEN** the vehicle list shows them in the order "Bike", "Family car", "van"

#### Scenario: Empty vehicle list
- **WHEN** the user has not added any vehicle
- **THEN** the Home screen shows an empty state and the action to add a vehicle

#### Scenario: Open the add screen
- **WHEN** the user taps the add vehicle action on the Home screen
- **THEN** the add vehicle screen is displayed

### Requirement: Vehicle details screen
The system SHALL show a details screen when the user selects a vehicle in the list. The details screen SHALL show the
vehicle's name, its license plate when it has one, its current odometer in the vehicle's unit, its most recent log events as
specified in the `vehicle-log` capability, and actions to open the full log and to edit the vehicle. Going back
SHALL return to the vehicle list.

#### Scenario: Open a vehicle
- **WHEN** the user taps "Family car" in the vehicle list
- **THEN** the details screen shows the name "Family car", the plate, the current odometer and the recent log events

#### Scenario: Back to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the vehicle list is displayed

### Requirement: Edit a vehicle
The system SHALL allow the user to change a vehicle's name and license plate from its details screen. The edit screen
SHALL start with the current values, SHALL apply the same trimming and name validation as adding a vehicle, and SHALL
NOT allow changing the odometer reading or the odometer unit. After a successful save the system SHALL return to the details screen showing
the new values, and the vehicle list SHALL show them too.

#### Scenario: Change name and plate
- **WHEN** the user edits "Family car" to the name " Estate car " and the plate "XYZ-789 " and saves
- **THEN** the details screen shows the name "Estate car" and the plate "XYZ-789"
- **AND** the vehicle list shows "Estate car" with the plate "XYZ-789"

#### Scenario: Remove the plate
- **WHEN** the user clears the license plate of a vehicle and saves
- **THEN** the vehicle no longer shows a license plate

#### Scenario: Name cannot be emptied
- **WHEN** the user clears the name, or replaces it with only spaces, and saves
- **THEN** the system shows the name error, stays on the edit screen and keeps the previously saved values

#### Scenario: Odometer and unit are not editable
- **WHEN** the user opens the edit screen of a vehicle
- **THEN** the screen offers only the name and the license plate for editing

#### Scenario: Cancel editing
- **WHEN** the user changes the fields and then leaves the edit screen without saving
- **THEN** the vehicle keeps its previously saved name and plate

### Requirement: Vehicles persist and work offline
The system SHALL keep vehicles on the device so that they are still present after the app is closed and reopened,
and SHALL provide all vehicle functionality without a network connection.

#### Scenario: Vehicles survive a restart
- **WHEN** the user adds a vehicle, closes the app completely and opens it again
- **THEN** the vehicle is still shown in the vehicle list with the same details

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds, views and edits a vehicle
- **THEN** every action succeeds and no network error is shown

**Platform note:** the behavior is the same on iOS; it is verified on Android now and on iOS once the Xcode project exists.
