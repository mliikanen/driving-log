# Spec Delta

## ADDED Requirements

### Requirement: A distance can be logged from the Home screen
The system SHALL make the Home screen's "Log event" action open the log distance form when the user has at least one vehicle. With no vehicle the action SHALL stay dimmed and SHALL NOT react to a tap, and its accessible name SHALL say that a vehicle must be added first. Saving a valid entry SHALL return to the Home screen; leaving the form without saving SHALL add nothing.

#### Scenario: Open the form from the Home screen
- **WHEN** the user has a vehicle and taps "Log event" on the Home screen
- **THEN** the log distance form is displayed with a vehicle selector at the top

#### Scenario: No vehicle
- **WHEN** the user has no vehicle and taps "Log event"
- **THEN** nothing happens, and the action is shown dimmed

#### Scenario: Save from the Home screen route
- **WHEN** the user opened the form from the Home screen, enters a valid entry and saves
- **THEN** the Home screen is displayed, and the entry is in the chosen vehicle's log

#### Scenario: Leave without saving
- **WHEN** the user opened the form from the Home screen and leaves it without saving
- **THEN** the Home screen is displayed and no entry is added

### Requirement: The vehicle is chosen with a selector when logging starts from the Home screen
When the log distance form is opened from the Home screen, the system SHALL show a vehicle selector at the top of the form: a dropdown that shows the chosen vehicle (its picture or icon and its name) and lists every vehicle the user has, in the order of the vehicle list. Choosing a vehicle SHALL make the form
about that vehicle: the previous known odometer, the checks of a new odometer count and the log the entry will join are that vehicle's, and the unit starts as that vehicle's (its unit family and its remembered choice of tenths). The digits already typed, the way (trip distance or new odometer), the date, the time and the time zone SHALL be kept when the vehicle is changed, and the typed digits are converted to the new unit as when the unit is changed by hand. The chosen vehicle SHALL survive a rotation of the device and the restart of the process. When the form is opened from a vehicle's details screen the selector SHALL NOT be shown and the vehicle SHALL be that vehicle.

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
- **WHEN** the user taps "Log distance" on a vehicle's details screen
- **THEN** the form has no vehicle selector and is about that vehicle

#### Scenario: The choice survives a rotation
- **WHEN** the user has chosen a vehicle in the selector and rotates the device
- **THEN** the form shows the same vehicle and what was typed

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
