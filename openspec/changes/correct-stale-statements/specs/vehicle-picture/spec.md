## ADDED Requirements

### Requirement: A vehicle has at most one picture
The system SHALL let a vehicle have at most one, optional, picture. A vehicle without one SHALL be shown with its
type's icon as a placeholder wherever its picture would be (`vehicle-type`); the add vehicle screen shows the icon of
the type chosen there (Car at first). The add and edit vehicle screens SHALL show a preview of the picture, or the
placeholder, labelled "Add picture" without a picture and "Change picture" with one, and SHALL offer "Remove picture"
only when there is a picture.

#### Scenario: A vehicle without a picture
- **WHEN** the user opens the add vehicle screen
- **THEN** the screen shows the placeholder as the preview (the icon of the type currently chosen, which is the car icon at first), labelled "Add picture", and offers no "Remove picture"

#### Scenario: A new vehicle's placeholder is the car icon
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

#### Scenario: The preview shows a camera mark
- **WHEN** the user opens the add vehicle screen, and again the edit screen of a vehicle that has a picture
- **THEN** the corner of the preview shows a camera mark in both cases, and no pen mark

#### Scenario: The mark is not a separate control
- **WHEN** the user taps the camera mark
- **THEN** the system chooser is shown, as when tapping anywhere else on the preview, and the screen reader announces the preview as "Add picture" or "Change picture" and not the mark separately

## MODIFIED Requirements

### Requirement: The photo comes from an app the user chooses through the system
The system SHALL let the user choose where the photo comes from with the system's own mechanism for providing an
image, opened by "Add picture" or "Change picture": on Android the intent chooser of the apps that can supply an image
(the camera app included); on iOS the source sheet with Take Photo, Photo Library and Choose File. It SHALL NOT draw
its own picker or camera screen, SHALL accept any still image the chosen app returns, and SHALL NOT change or remove a
photo the user chose.

#### Scenario: The system asks which app provides the image
- **WHEN** the user taps "Add picture"
- **THEN** the system's chooser (on iOS the source sheet) is shown, listing the apps or sources that can provide an image, and the app draws no picker of its own

#### Scenario: The camera apps are listed as themselves on Android
- **WHEN** on Android the user taps "Add picture", whether or not camera access has been allowed
- **THEN** the chooser lists the installed camera apps, up to two, each under its own name and icon, next to every app that can provide an image

#### Scenario: Choose a photo
- **WHEN** the user chooses a photo in the app they picked
- **THEN** the crop screen opens with that photo

#### Scenario: Take a new photo
- **WHEN** the user chooses the camera app in the chooser and takes a photo
- **THEN** the crop screen opens with the new photo

#### Scenario: No permission is asked before it is needed
- **WHEN** the user opens the add vehicle screen, and again when the user chooses a photo from the library
- **THEN** no permission prompt is shown

#### Scenario: Camera access is asked for when taking a photo
- **WHEN** the user chooses the camera (Take Photo on iOS, a camera app in the chooser on Android) and the app has not been allowed camera access
- **THEN** the system asks for camera access at that moment, and the camera opens when it is allowed

#### Scenario: Camera access refused
- **WHEN** the user refuses camera access when asked, or has refused it before and chooses the camera again
- **THEN** the system opens no camera, keeps the vehicle's picture as it was, and shows below the picture the missing-permission message (`app-shell`): a photo can't be taken because camera access is turned off, with "Open settings"

#### Scenario: Leave the chooser
- **WHEN** the user opens the chooser and leaves it, or leaves the camera without taking a photo
- **THEN** the form is unchanged, and no missing-permission message is shown

#### Scenario: A file that is not an image
- **WHEN** the chosen app returns a file that cannot be decoded as an image
- **THEN** the system shows that the picture could not be opened, does not open the crop screen and keeps the vehicle's picture as it was

#### Scenario: The photo's orientation is respected
- **WHEN** the user chooses or takes a photo that is stored rotated and marked with an orientation
- **THEN** the crop screen shows it the right way up

**Platform note:** on Android the system chooser lists every app that can provide an image, and the installed camera apps,
up to two (the most the chooser shows next to its own list), each under its own name and icon; choosing a camera app
while the app lacks camera access asks for it first, because the app declares the camera permission (for the live
scanner). On iOS a source sheet offers the photo library, the camera and files.

## REMOVED Requirements

### Requirement: A vehicle can have one optional picture
**Reason**: Its scenario "The placeholder is a generic car icon" is about the add screen's preselected Car since `vehicle-type`, and OpenSpec cannot rename a scenario through MODIFIED.
**Migration**: Replaced by "A vehicle has at most one picture", the same requirement with that scenario named "A new vehicle's placeholder is the car icon".
