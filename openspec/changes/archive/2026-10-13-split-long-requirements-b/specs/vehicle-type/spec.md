## MODIFIED Requirements

### Requirement: A vehicle has a type from a fixed set
The system SHALL give every vehicle a type from this fixed set, each with a name and its own icon, shown in this
order: "Car", "SUV", "Van", "Truck", "Bus", "Motorcycle", "Scooter" and "Other". Every vehicle SHALL always have
exactly one type: the data model SHALL NOT allow a vehicle without one. The names are English for now, like the rest
of the app.

#### Scenario: The eight types are offered
- **WHEN** the user opens the type choice on the add vehicle screen
- **THEN** it offers "Car", "SUV", "Van", "Truck", "Bus", "Motorcycle", "Scooter" and "Other", in that order, each with an icon

#### Scenario: The type is stored as a code
- **WHEN** a vehicle of the type "Van" is saved
- **THEN** what is stored for the type is the code `VAN`, whatever the device language

#### Scenario: An unknown stored code
- **WHEN** a vehicle's stored type is a code the app does not know
- **THEN** the vehicle is shown with the type "Other" and nothing fails

### Requirement: The placeholder icon follows the type
Wherever the system shows a vehicle without a picture (the vehicle list, the details screen, the add and edit screens'
picture preview, any later vehicle picker), it SHALL show its type's icon instead, following the choice live on the
add screen (Car at first). A picture, when there is one, SHALL be shown instead of the icon. The icon SHALL carry the
accessibility label "Vehicle type: " and the type's name (e.g. "Vehicle type: Van"). The eight types' icons SHALL all
differ.

#### Scenario: The list shows the icon of the type
- **WHEN** the vehicle list contains a vehicle of the type "Motorcycle" without a picture
- **THEN** its item shows the motorcycle icon, labelled "Vehicle type: Motorcycle"

#### Scenario: The details screen shows the icon of the type
- **WHEN** the user opens the details screen of a vehicle of the type "Van" without a picture
- **THEN** the screen shows the van icon at the top

#### Scenario: The form preview follows the choice
- **WHEN** the user chooses the type "Truck" on the add screen and has added no picture
- **THEN** the picture preview shows the truck icon

#### Scenario: A picture wins
- **WHEN** a vehicle of the type "Truck" has a picture
- **THEN** the vehicle list and the details screen show the picture and not the truck icon

#### Scenario: The add screen starts with the car icon
- **WHEN** the user opens the add vehicle screen and has changed no type and added no picture
- **THEN** the picture preview shows the car icon, labelled "Add picture"

#### Scenario: Removing the picture brings the icon back
- **WHEN** the user removes the picture of a vehicle of the type "Bus" and saves
- **THEN** the vehicle is shown with the bus icon

## ADDED Requirements

### Requirement: A type is stored as a code that does not depend on the language
The system SHALL store a vehicle's type as a code that does not depend on the language or the device: `CAR`, `SUV`,
`VAN`, `TRUCK`, `BUS`, `MOTORCYCLE`, `SCOOTER` or `OTHER`. It SHALL treat a code it does not know, such as one written
by a newer version of the app, as the type "Other".

#### Scenario: Every type's code
- **WHEN** vehicles of each of the eight types are saved and read back
- **THEN** their stored codes are `CAR`, `SUV`, `VAN`, `TRUCK`, `BUS`, `MOTORCYCLE`, `SCOOTER` and `OTHER`, and each
  reads back as the type it was saved with
