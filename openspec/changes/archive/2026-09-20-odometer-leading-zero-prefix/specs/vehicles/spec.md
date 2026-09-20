# Spec Delta

## MODIFIED Requirements

### Requirement: Odometer readings are entered in a microwave-style number field
The system SHALL let the user enter an odometer reading, wherever the app asks for one (today when adding a vehicle),
in a field that uses the device's number keyboard (digits only) and works like a microwave oven's timer: each digit typed enters at the right-hand end of the
displayed value and shifts the earlier digits one place to the left. For the units "Kilometers" and "Miles" the last
digit is a whole unit. For the units "Kilometers with 100 m" and "Miles with tenths" the last digit is a tenth, and
the decimal separator is added automatically. The field SHALL always show the current value in the vehicle's unit as
specified for odometer readings, so it SHALL show the decimal separator, and any thousands separator, of the device's
current locale (for example a comma in Finnish and a period in English (United States)), even though the user never
types a separator. The value SHALL be right-aligned in the field: new digits appear at the right edge of the field
and push the earlier digits to the left, with the unit abbreviation kept fixed at the end of the field so that the
right-hand end of the number does not move. The field starts empty: while no digit has been entered it SHALL NOT show
0 or 0.0 or any other number, only its label. Deleting a digit with the keyboard's backspace SHALL remove the last digit
entered, so that typing then deleting a digit restores the earlier value, and deleting the only digit SHALL make the
field empty again. A clear control in the field SHALL make it empty. A typed 0 SHALL be shown as 0 (0.0 for the tenths
units) and counts as an entered reading; in the tenths units that first 0 is the tenth. When more digits are typed after
a first 0, that 0 SHALL be kept as a prefix digit that is not drawn as an extra digit (the reading shows 0.5, never 00.5),
and backspace SHALL remove it only after the digits typed after it, so that backspace restores exactly the earlier
value and the field goes back through the typed zero to empty. Typing 0 while the entry is only the typed 0 SHALL leave
it unchanged, so an entry has at most one prefix zero. The prefix zero SHALL NOT count towards the digit limit.
Characters that are not digits, such as a comma, a period or a minus sign, whether typed or pasted, SHALL
be ignored. The value SHALL have at most 7 whole digits, and digits that would exceed that SHALL be ignored. Because
only digits can be entered, an invalid reading cannot be entered. Every field in the app where an odometer reading is
entered, on any screen, SHALL follow every rule of this requirement, so that entering an odometer works identically
everywhere; a screen MAY decide whether an empty field can be saved, but not how the field behaves.

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

#### Scenario: The field starts empty
- **WHEN** the user opens the add vehicle screen
- **THEN** the odometer field shows no number, neither 0 nor 0.0, whichever unit is preselected

#### Scenario: A typed zero is a reading
- **WHEN** the unit has tenths and the user types 0 in the empty field
- **THEN** the field shows 0.0 (and 0 for a whole-number unit) and saving is allowed

#### Scenario: Backspace on a typed zero
- **WHEN** the field shows the typed zero and the user presses backspace
- **THEN** the field is empty again

#### Scenario: A first zero is kept when more digits follow in a tenths unit
- **WHEN** the unit has tenths and the user types 0, then 5
- **THEN** the field shows 0.0, then 0.5
- **AND** pressing backspace shows 0.0 again, and pressing backspace once more makes the field empty

#### Scenario: A first zero is kept under several later digits
- **WHEN** the unit has tenths and the user types 0, 5, 3
- **THEN** the field shows 0.0, 0.5, 5.3
- **AND** three backspaces then show 0.5, then 0.0, then an empty field

#### Scenario: A first zero is kept in a whole-number unit
- **WHEN** the unit is "Kilometers" and the user types 0, then 5
- **THEN** the field shows 0 km, then 5 km
- **AND** pressing backspace shows 0 km again, and pressing backspace once more makes the field empty

#### Scenario: Further leading zeros are ignored
- **WHEN** the unit has tenths and the user types 0, 0, 0, then 5
- **THEN** the field shows 0.0 after each of the zeros, then 0.5
- **AND** pressing backspace shows 0.0, and one more makes the field empty

#### Scenario: A zero that is not typed first is an ordinary digit
- **WHEN** the unit has tenths and the user types 1, then 0
- **THEN** the field shows 0.1, then 1.0
- **AND** pressing backspace shows 0.1

#### Scenario: A pasted leading zero is kept like a typed one
- **WHEN** the unit has tenths, the field is empty and the user pastes "0123"
- **THEN** the field shows 12.3
- **AND** pressing backspace three times shows 1.2, then 0.1, then 0.0, and a fourth press makes the field empty

#### Scenario: The prefix zero does not count towards the digit limit
- **WHEN** the unit is "Kilometers" and the user types 0 followed by the seven digits 9999999
- **THEN** the field shows 9999999 km
- **AND** typing one more digit leaves it unchanged

#### Scenario: Every odometer field behaves the same
- **WHEN** the user enters an odometer reading on any screen of the app that has an odometer field
- **THEN** the field starts empty, fills from the right edge, keeps a first typed zero as a prefix, removes digits with backspace in the order they were typed, and draws the device locale's separators, exactly as in the other scenarios of this requirement

#### Scenario: Backspace restores the earlier value
- **WHEN** the unit has tenths, the field shows 1.2, the user types 3 and then presses backspace
- **THEN** the field shows 12.3 and then 1.2 again

#### Scenario: Typing, deleting to empty and typing again
- **WHEN** the unit has tenths and the user types 1, 2, presses backspace twice, then types 2, 3, 0
- **THEN** after each key the field shows 0.1, 1.2, 0.1, (empty), 0.2, 2.3, 23.0

#### Scenario: Backspace removes digits one at a time
- **WHEN** the unit has tenths, the field shows 12.3, and the user presses backspace three times
- **THEN** the field shows 1.2, then 0.1, then is empty

#### Scenario: Backspace on an empty field
- **WHEN** the field is empty and the user presses backspace
- **THEN** the field is still empty

#### Scenario: Backspace on a whole-number unit
- **WHEN** the unit is "Kilometers", the field shows 123 km, and the user presses backspace
- **THEN** the field shows 12 km

#### Scenario: Clear
- **WHEN** the field shows 12.3 and the user uses the clear control
- **THEN** the field is empty

#### Scenario: Zero at zero
- **WHEN** the user types 0 three times in the empty field of a whole-number unit
- **THEN** the field shows 0

#### Scenario: Maximum length
- **WHEN** the unit is "Kilometers" and the field already shows 9999999 km, and the user types 1
- **THEN** the field still shows 9999999 km

#### Scenario: Non-digits are ignored
- **WHEN** the field shows 12.3 and the user types or pastes a comma, a period or a minus sign
- **THEN** the field still shows 12.3

#### Scenario: Pasting digits
- **WHEN** the unit is "Kilometers with 100 m", the field is empty and the user pastes "123"
- **THEN** the field shows 12.3

#### Scenario: Changing the unit keeps the value
- **WHEN** the field shows 123 km and the user changes the unit to "Kilometers with 100 m"
- **THEN** the field shows 123.0 km
- **AND** changing the unit back to "Kilometers" shows 123 km

#### Scenario: Changing the unit of an empty field
- **WHEN** the field is empty and the user changes the unit
- **THEN** the field is still empty

#### Scenario: Changing to a whole-number unit rounds
- **WHEN** the unit has tenths, the field shows 12.6, and the user changes the unit to "Kilometers"
- **THEN** the field shows 13 km
