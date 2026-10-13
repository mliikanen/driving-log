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

#### Scenario: Every camera app is listed as itself on Android
- **WHEN** on Android the user taps "Add picture", whether or not camera access has been allowed
- **THEN** the chooser lists every installed camera app under its own name and icon

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

**Platform note:** Android: `Intent.createChooser` over `ACTION_GET_CONTENT` for images, with every installed camera app's own
`ACTION_IMAGE_CAPTURE` as an initial intent, so each is listed under its own name and icon. The chooser's refinement step
(`EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER`), which runs after the user picks an app and before its launch, asks for the camera permission
when a camera app was picked and the permission isn't held. Because the app declares the camera permission (for the live scanner), Android
refuses `ACTION_IMAGE_CAPTURE` from the app while the permission isn't held. iOS: `PHPicker` for the library, `UIImagePickerController` for the camera and the document picker for files,
offered in a source sheet.

### Requirement: Permissions are asked for only when needed
The system SHALL ask for a permission only when the user triggers an action that needs it, at that moment, and SHALL
NOT ask for permissions the chosen way does not need: choosing a photo, file or other image app needs none. Taking a
photo with the camera needs camera access on both platforms. It SHALL be asked for when the user chooses the camera
and the app does not hold it. If it is refused, the system SHALL show the missing-permission message (`app-shell`)
and change nothing else.

#### Scenario: Opening the chooser on Android asks for nothing
- **WHEN** on Android the user taps "Add picture" on the add vehicle screen
- **THEN** the system chooser opens directly, with no permission prompt before it

#### Scenario: Choosing a photo app on Android asks for nothing
- **WHEN** on Android the user chooses a photo or file app in the chooser, whether or not camera access has been allowed
- **THEN** that app opens with no permission prompt

#### Scenario: Choosing the camera app without camera access on Android
- **WHEN** on Android the user chooses a camera app in the chooser and the app has not been allowed camera access
- **THEN** the system asks for camera access before the camera app opens, and the chosen camera app opens when it is allowed

#### Scenario: Camera access refused on Android
- **WHEN** on Android the user refuses camera access after choosing a camera app
- **THEN** no camera app opens, and the screen the user came from is shown as it was, with the missing-permission message and its "Open settings" action

#### Scenario: Camera prompt dismissed on Android
- **WHEN** on Android the user chooses a camera app and leaves the camera-access prompt without allowing it
- **THEN** no camera app opens, and the screen the user came from shows the missing-permission message, as for a refusal

#### Scenario: A refusal Android remembers
- **WHEN** on Android the user has refused camera access so that the system no longer asks, and chooses a camera app
- **THEN** no prompt is shown and no camera app opens, and the screen shows the missing-permission message and its "Open settings" action

#### Scenario: Camera access already allowed on Android
- **WHEN** on Android camera access has been allowed (for example for the live scanner) and the user chooses a camera app
- **THEN** the camera app opens with no permission prompt

#### Scenario: Camera access allowed from the message
- **WHEN** the missing-permission message is shown, the user taps "Open settings", allows camera access there and comes back
- **THEN** the message is gone, and choosing a camera app in the chooser opens it with no permission prompt

#### Scenario: Camera access withdrawn on Android
- **WHEN** on Android the user allowed camera access, then turned it off in the device settings, and chooses a camera app again
- **THEN** the system asks for camera access again before the camera app opens
