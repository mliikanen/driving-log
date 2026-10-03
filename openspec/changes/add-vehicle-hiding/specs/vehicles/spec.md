# Spec Delta

## MODIFIED Requirements

### Requirement: Vehicle list
The system SHALL show every vehicle the user has added and not hidden on the vehicle list screen, which the Home screen's "Vehicles" action opens and which is titled "Vehicles" with a back arrow that returns to the Home screen. The list is ordered by name without regard to letter
case, and SHALL show each vehicle's small picture (or the placeholder when it has none), its name and, when it has one, its license plate. When there are no vehicles shown the
system SHALL show an empty state that invites the user to add a vehicle. When any vehicle is hidden, the list SHALL end with a
"Hidden vehicles" entry that opens the hidden vehicles screen ("Restore a hidden vehicle"); with none hidden, the entry SHALL NOT
be shown. The vehicle list screen SHALL offer an action to
add a vehicle.

#### Scenario: Vehicles are listed alphabetically
- **WHEN** the user has vehicles named "van", "Bike" and "Family car"
- **THEN** the vehicle list shows them in the order "Bike", "Family car", "van"

#### Scenario: Vehicles show their picture
- **WHEN** the user has a vehicle with a picture and a vehicle without one
- **THEN** the list shows the small picture for the first and the placeholder for the second

#### Scenario: Empty vehicle list
- **WHEN** the vehicle list screen is displayed and the user has not added any vehicle
- **THEN** the screen shows an empty state and the action to add a vehicle

#### Scenario: Open the add screen
- **WHEN** the user taps the add vehicle action on the vehicle list screen
- **THEN** the add vehicle screen is displayed

#### Scenario: Back to the Home screen
- **WHEN** the user taps the back arrow of the vehicle list screen
- **THEN** the Home screen is displayed

#### Scenario: A hidden vehicle is not listed
- **WHEN** the user has vehicles "Bike" and "Family car" and has hidden "Bike"
- **THEN** the vehicle list shows "Family car" only, followed by the "Hidden vehicles" entry

#### Scenario: No hidden vehicles, no entry
- **WHEN** no vehicle is hidden
- **THEN** the vehicle list shows no "Hidden vehicles" entry

### Requirement: Vehicle details screen
The system SHALL show a details screen when the user selects a vehicle in the list. The details screen SHALL show the
vehicle's large picture at the top (or the placeholder when it has none), its name, its license plate when it has one, its current odometer in the vehicle's unit, its most recent log events as
specified in the `vehicle-log` capability, and actions to log an event, to open the full log and to edit the vehicle, and, in its
top app bar's overflow menu, an action to hide the vehicle ("Hide a vehicle").
Going back SHALL return to the vehicle list.

#### Scenario: Open a vehicle
- **WHEN** the user taps "Family car" in the vehicle list
- **THEN** the details screen shows the name "Family car", the plate, the current odometer and the recent log events

#### Scenario: The picture is shown
- **WHEN** the user opens the details screen of a vehicle with a picture
- **THEN** the screen shows the large picture above the name

#### Scenario: Open the log distance form
- **WHEN** the user taps "Log event" on the details screen
- **THEN** the log event form is displayed, as specified in the `distance-logging` capability

#### Scenario: Back to the list
- **WHEN** the user navigates back from the details screen
- **THEN** the vehicle list is displayed

#### Scenario: The hide action is in the overflow menu
- **WHEN** the user opens the overflow menu of a vehicle's details screen
- **THEN** it offers "Hide vehicle"

## ADDED Requirements

### Requirement: Hide a vehicle
The system SHALL let the user hide a vehicle from its details screen's "Hide vehicle" action, after a confirmation dialog that
names the vehicle, says that it will no longer be listed or offered when logging, that its log and everything else about it
are kept, and that it can be restored from "Hidden vehicles". Confirming SHALL hide the vehicle and return to the vehicle
list; cancelling or dismissing the dialog SHALL change nothing. Hiding SHALL NOT delete or change any of the vehicle's data
(its log, pictures, notes, scans or any other record); it only changes where the vehicle is shown. A hidden vehicle SHALL NOT be
shown or offered anywhere except the hidden vehicles screen, including as a candidate for any automatic vehicle detection.
Hiding SHALL be stored on the device, SHALL work without a network and SHALL survive closing the app.

#### Scenario: Hide after confirming
- **WHEN** the user taps "Hide vehicle" on the details screen of "Bike" and confirms
- **THEN** the vehicle list is shown without "Bike"

#### Scenario: Cancel hiding
- **WHEN** the user taps "Hide vehicle" and cancels the dialog
- **THEN** the details screen of the vehicle is shown and the vehicle is not hidden

#### Scenario: Hiding keeps the data
- **WHEN** the user hides a vehicle with a log, a picture and notes, and later restores it
- **THEN** its log, picture and notes are exactly as before

#### Scenario: Hidden after a restart
- **WHEN** the user hides a vehicle and closes the app completely
- **THEN** after reopening the vehicle is still hidden

### Requirement: Restore a hidden vehicle
The system SHALL show, from the vehicle list's "Hidden vehicles" entry, a hidden vehicles screen titled "Hidden vehicles" with a
back arrow to the vehicle list, listing every hidden vehicle as the vehicle list lists vehicles (small picture or placeholder,
name, plate), in the same order, each with a "Restore" action. Restoring SHALL show the vehicle again everywhere a visible vehicle
is shown, with all its data. When the last hidden vehicle is restored, the screen SHALL return to the vehicle list.

#### Scenario: Restore a vehicle
- **WHEN** the user opens "Hidden vehicles" and taps "Restore" on "Bike"
- **THEN** "Bike" is listed in the vehicle list again

#### Scenario: The last one restored
- **WHEN** the user restores the only hidden vehicle
- **THEN** the vehicle list is shown, without the "Hidden vehicles" entry
