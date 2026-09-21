# Spec Delta

## MODIFIED Requirements

### Requirement: A vehicle can have one optional picture
The system SHALL let a vehicle have at most one picture, which is optional. A vehicle without a picture SHALL be shown
with the icon of its type (as specified in the `vehicle-type` capability) as its placeholder wherever its picture would be shown, and the add vehicle screen shows the icon of the type currently chosen there (Car at first). The picture SHALL be offered on the add vehicle screen
and on the edit vehicle screen as the picture itself: the form SHALL show a preview of the picture it has (or the placeholder), and tapping the preview SHALL be the
action that starts choosing a picture, with no separate button for it. The preview SHALL be labelled "Add picture" when there is no picture and "Change picture" when there
is one, and SHALL show a small edit mark that tells it can be tapped. A "Remove picture" action SHALL be offered only when the vehicle (or the form) has a picture.

#### Scenario: A vehicle without a picture
- **WHEN** the user opens the add vehicle screen
- **THEN** the screen shows the placeholder as the preview (the icon of the type currently chosen, which is the car icon at first), labelled "Add picture", and offers no "Remove picture"

#### Scenario: The placeholder is a generic car icon
- **WHEN** the user opens the add vehicle screen and has not changed the preselected type
- **THEN** the picture preview shows the car icon in the place of the picture

#### Scenario: The placeholder follows the vehicle's type
- **WHEN** the vehicle list contains a vehicle of the type "Van" without a picture
- **THEN** its item shows the van icon in the place of the picture

#### Scenario: A vehicle with a picture
- **WHEN** the user opens the edit screen of a vehicle that has a picture
- **THEN** the screen shows that picture as the preview, labelled "Change picture", and offers "Remove picture"

#### Scenario: Tapping the picture starts choosing
- **WHEN** the user taps the preview on the add or edit screen
- **THEN** the system chooser of where the photo comes from is shown, and there is no other button that does so
