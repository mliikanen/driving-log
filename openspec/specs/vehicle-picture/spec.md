# vehicle-picture Specification

## Purpose
Lets users give a vehicle a picture: they choose or take a photo with the system's own image chooser, crop it to a square, and the app stores a
small and a large version of it in its own local file system. The small one shows the vehicle in lists and pickers, the large one
in full screen views such as the vehicle details.

## Requirements

### Requirement: A vehicle can have one optional picture
The system SHALL let a vehicle have at most one picture, which is optional. A vehicle without a picture SHALL be shown
with the icon of its type (as specified in the `vehicle-type` capability) as its placeholder wherever its picture would be shown, and the add vehicle screen shows the icon of the type currently chosen there (Car at first). The picture SHALL be offered on the add vehicle screen
and on the edit vehicle screen as the picture itself: the form SHALL show a preview of the picture it has (or the placeholder), and tapping the preview SHALL be the
action that starts choosing a picture, with no separate button for it. The preview SHALL be labelled "Add picture" when there is no picture and "Change picture" when there
is one, and SHALL show a small camera mark in its corner that tells a photo can be set by tapping it. The mark SHALL be the same camera in both cases, on top of the picture or the placeholder, and SHALL NOT be an edit (pen) mark. A "Remove picture" action SHALL be offered only when the vehicle (or the form) has a picture.

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

#### Scenario: The preview shows a camera mark
- **WHEN** the user opens the add vehicle screen, and again the edit screen of a vehicle that has a picture
- **THEN** the corner of the preview shows a camera mark in both cases, and no pen mark

#### Scenario: The mark is not a separate control
- **WHEN** the user taps the camera mark
- **THEN** the system chooser is shown, as when tapping anywhere else on the preview, and the screen reader announces the preview as "Add picture" or "Change picture" and not the mark separately

### Requirement: The photo comes from an app the user chooses through the system
The system SHALL let the user choose where the photo comes from with the system's own mechanism for providing an image, opened by "Add picture" or
"Change picture": on Android the system intent chooser, which offers the apps that can supply an image (including the camera app, so a new photo can be
taken); on iOS the system source sheet with Take Photo, Photo Library and Choose File. The system SHALL NOT draw its own picker or camera screen. It
SHALL accept any still image the chosen app returns and SHALL NOT change or remove a photo the user chose. Leaving the chooser or the app without
providing an image SHALL change nothing. When the image cannot be read as an image, the system SHALL show a message that the picture could not be opened
and SHALL keep the vehicle's picture as it was.

The system SHALL ask for a permission only when the user triggers an action that needs it, at that moment and not before, and SHALL NOT ask for
permissions the chosen way does not need. On Android the chooser and the camera app are used through intents, which need no storage, photo or camera
permission of the app. On iOS taking a photo needs camera access, which is asked for when the user chooses Take Photo the first time; when access is
refused, the system SHALL show a message that camera access is turned off and that it can be allowed in the device settings, and SHALL change nothing else.

**Platform note:** Android: `Intent.createChooser` over `ACTION_GET_CONTENT` for images, with the camera app's `ACTION_IMAGE_CAPTURE` as an initial
intent. iOS: `PHPicker` for the library, `UIImagePickerController` for the camera and the document picker for files, offered in a source sheet.

#### Scenario: The system asks which app provides the image
- **WHEN** the user taps "Add picture"
- **THEN** the system's chooser (on iOS the source sheet) is shown, listing the apps or sources that can provide an image, and the app draws no picker of its own

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
- **WHEN** on iOS the user chooses Take Photo for the first time
- **THEN** the system asks for camera access at that moment, and the camera opens when it is allowed

#### Scenario: Camera access refused
- **WHEN** on iOS the user has refused camera access and chooses Take Photo
- **THEN** the system shows a message that camera access is turned off and can be allowed in the settings, opens no camera and keeps the vehicle's picture as it was

#### Scenario: Leave the chooser
- **WHEN** the user opens the chooser and leaves it, or leaves the camera without taking a photo
- **THEN** the form is unchanged

#### Scenario: A file that is not an image
- **WHEN** the chosen app returns a file that cannot be decoded as an image
- **THEN** the system shows that the picture could not be opened, does not open the crop screen and keeps the vehicle's picture as it was

#### Scenario: The photo's orientation is respected
- **WHEN** the user chooses or takes a photo that is stored rotated and marked with an orientation
- **THEN** the crop screen shows it the right way up

### Requirement: The photo must be cropped to a square
After a photo is chosen the system SHALL open a crop screen showing the photo under a fixed square frame, and SHALL NOT use the
photo before the user has confirmed a crop. The user SHALL be able to move the photo under the frame and to zoom it, and the
system SHALL keep the frame inside the photo at all times: the smallest zoom makes the shorter side of the photo fill the frame,
and the photo cannot be moved so that an edge of the frame leaves it. The crop SHALL start at the smallest zoom with the photo centered. The
user SHALL confirm with "Use photo" or cancel; cancelling SHALL discard the chosen photo and leave the form as it was. There SHALL be no
way to keep a photo without cropping it.

#### Scenario: The crop starts centered
- **WHEN** the crop screen opens with a landscape photo
- **THEN** the frame covers the largest centered square of the photo

#### Scenario: The frame stays inside the photo
- **WHEN** the user drags the photo so that far more than its width would pass the frame
- **THEN** the photo stops at the frame's edge and no part of the frame is empty

#### Scenario: Zoom in and out
- **WHEN** the user pinches to zoom in and then pinches to zoom out past the start
- **THEN** the zoom never goes below the smallest zoom and the frame stays inside the photo

#### Scenario: Confirm
- **WHEN** the user moves and zooms the photo and taps "Use photo"
- **THEN** the form shows the part of the photo inside the frame as its picture

#### Scenario: Cancel the crop
- **WHEN** the user taps cancel on the crop screen
- **THEN** the form's picture is what it was before and nothing is stored

#### Scenario: The crop cannot be skipped
- **WHEN** the crop screen is displayed
- **THEN** it offers only "Use photo" and cancel

### Requirement: A small and a large version are stored
The system SHALL store two versions of the cropped picture: a small version of at most 256 x 256 pixels and a large version of at
most 1024 x 1024 pixels, both square and showing the same crop. A version is scaled down to its target size, and SHALL NOT be
enlarged: when the crop's side is shorter than the target, the version has the size of the crop. Both versions SHALL be stored in a size-efficient image format, lossy WebP at quality 80
with transparency kept, so that the large version of a typical photo is well under 200 kilobytes.

**Platform note:** iOS can read WebP but its image APIs cannot write it, so on iOS the versions are stored as PNG until a WebP encoder is added.
Every platform reads either format whatever platform wrote it.

#### Scenario: Sizes of a large photo
- **WHEN** the user crops a 4000 x 3000 photo to a 3000 x 3000 square and confirms
- **THEN** the stored small version is 256 x 256 pixels and the stored large version is 1024 x 1024 pixels

#### Scenario: A small photo is not enlarged
- **WHEN** the user crops a 600 x 400 photo to a 400 x 400 square and confirms
- **THEN** the stored large version is 400 x 400 pixels and the small version is 256 x 256 pixels

#### Scenario: A tiny crop is not enlarged at all
- **WHEN** the user crops a photo to a 200 x 200 square and confirms
- **THEN** both stored versions are 200 x 200 pixels

#### Scenario: Both versions show the same crop
- **WHEN** the user confirms a crop
- **THEN** the small and the large version show the same part of the photo

#### Scenario: Efficient format
- **WHEN** a picture is stored on Android
- **THEN** both files are WebP images and the large one is smaller than the same picture would be as a PNG

### Requirement: Pictures are stored in the application's local file system
The system SHALL store the picture files in the application's private storage on the device, SHALL NOT store them in the
device's photo library or another shared location, and SHALL keep only an identifier of the picture with the vehicle's data,
never a file path, so that stored data does not depend on where the files live. The system SHALL keep the pictures after the app is
closed and reopened, and SHALL show and change them without a network connection. Replacing or removing a vehicle's picture SHALL delete its
old files, and files that no vehicle refers to (left by an interrupted save, say) SHALL be deleted when the app starts.

#### Scenario: Survives a restart
- **WHEN** the user saves a vehicle with a picture, closes the app completely and opens it again
- **THEN** the vehicle's picture is still shown, in the list and on the details screen

#### Scenario: Offline use
- **WHEN** the device has no network connection and the user adds a picture to a vehicle
- **THEN** every step succeeds and no network error is shown

#### Scenario: Not in the photo library
- **WHEN** the user saves a vehicle with a picture
- **THEN** no new photo appears in the device's photo library

#### Scenario: Replaced pictures are deleted
- **WHEN** the user changes a vehicle's picture and saves
- **THEN** the files of the earlier picture no longer exist

#### Scenario: Unused files are cleaned up
- **WHEN** the app starts and the picture storage holds files that no vehicle refers to
- **THEN** those files are deleted and the pictures of vehicles are kept

### Requirement: A picture is saved with the vehicle or not at all
The system SHALL save a new vehicle's picture together with the vehicle and its initial odometer event, or save none of them,
and SHALL apply a changed or removed picture together with the other changes of an edit, or none of them. A picture the user has
confirmed but not yet saved SHALL be kept when the screen is rotated or recreated, and SHALL be discarded, with its files,
when the user leaves the add or edit screen without saving. A failed save SHALL leave no picture files that the vehicle does not use and SHALL
leave the vehicle's saved picture as it was.

#### Scenario: Add a vehicle with a picture
- **WHEN** the user adds a vehicle, adds and crops a picture and saves
- **THEN** the vehicle appears in the list with that picture and its details screen shows it

#### Scenario: Cancel adding
- **WHEN** the user adds and crops a picture on the add screen and leaves the screen without saving
- **THEN** no vehicle is added and no picture files are left

#### Scenario: Rotate with an unsaved picture
- **WHEN** the user has confirmed a crop on the add or edit screen and rotates the device
- **THEN** the form still shows that picture

#### Scenario: A failed save
- **WHEN** saving a vehicle with a new picture fails
- **THEN** no vehicle is added or changed, the vehicle's earlier picture is still in use and no unused picture files are left

### Requirement: Change and remove a picture
The system SHALL let the user change or remove the picture of an existing vehicle from its edit screen. Changing SHALL go through
the photo picker and the crop like adding does. Removing SHALL make the vehicle have no picture once the edit is saved. Leaving the edit screen without saving
SHALL keep the vehicle's saved picture. Changing the picture SHALL NOT change the vehicle's odometer, unit or log.

#### Scenario: Change the picture
- **WHEN** the user edits a vehicle that has a picture, chooses and crops another photo and saves
- **THEN** the vehicle's list entry and details screen show the new picture

#### Scenario: Remove the picture
- **WHEN** the user taps "Remove picture" on the edit screen of a vehicle with a picture and saves
- **THEN** the vehicle has no picture: the list and the details screen show the placeholder

#### Scenario: Remove without saving
- **WHEN** the user taps "Remove picture" and leaves the edit screen without saving
- **THEN** the vehicle keeps its picture

#### Scenario: Only the picture changes
- **WHEN** the user changes only the picture of a vehicle and saves
- **THEN** the vehicle's name, plate, odometer, unit and log are unchanged

### Requirement: The small version shows the vehicle in lists and pickers, the large version in full screen views
The system SHALL show a vehicle's small picture wherever the vehicle is shown as an item among others (the vehicle list, and any
picker of vehicles), and SHALL show its large picture on views that show the vehicle full screen (the vehicle details screen). The
system SHALL NOT use the large version in a list or a picker, and SHALL show the placeholder when the vehicle has no picture or
its files cannot be read.

#### Scenario: The list shows the small picture
- **WHEN** the vehicle list contains a vehicle with a picture
- **THEN** its item shows the small version of the picture

#### Scenario: The details screen shows the large picture
- **WHEN** the user opens the details screen of a vehicle with a picture
- **THEN** the screen shows the large version of the picture

#### Scenario: Missing files
- **WHEN** a vehicle refers to a picture whose files cannot be read
- **THEN** the placeholder is shown in place of the picture and everything else works
