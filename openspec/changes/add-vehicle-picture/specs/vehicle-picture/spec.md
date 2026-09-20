# Spec Delta

## Purpose

Lets users give a vehicle a picture: they choose a photo with the system photo picker, crop it to a square, and the app stores a
small and a large version of it in its own local file system. The small one shows the vehicle in lists and pickers, the large one
in full screen views such as the vehicle details.

## ADDED Requirements

### Requirement: A vehicle can have one optional picture
The system SHALL let a vehicle have at most one picture, which is optional. A vehicle without a picture SHALL be shown
with a generic car icon as its placeholder wherever its picture would be shown; the icon is the same for every vehicle until vehicles have a type. The picture SHALL be offered on the add vehicle screen
and on the edit vehicle screen as an "Add picture" action, which becomes "Change picture" together with a "Remove picture"
action when the vehicle (or the form) has a picture, and the form SHALL show a preview of the picture it has.

#### Scenario: A vehicle without a picture
- **WHEN** the user opens the add vehicle screen
- **THEN** the screen offers "Add picture", shows the generic car icon as the preview and offers no "Remove picture"

#### Scenario: The placeholder is a generic car icon
- **WHEN** the vehicle list contains a vehicle without a picture
- **THEN** its item shows the generic car icon in the place of the picture

#### Scenario: A vehicle with a picture
- **WHEN** the user opens the edit screen of a vehicle that has a picture
- **THEN** the screen shows that picture as the preview and offers "Change picture" and "Remove picture"

### Requirement: The photo is chosen with the system photo picker
The system SHALL let the user choose the source photo with the system's own photo picker, opened by "Add picture" or "Change picture",
and SHALL NOT ask for permission to read the device's photos or files. The system SHALL accept any still image the
picker returns and SHALL NOT change or remove the chosen photo. Leaving the picker without choosing SHALL change nothing.
When the chosen file cannot be read as an image, the system SHALL show a message that the picture could not be opened and SHALL
keep the vehicle's picture as it was.

**Platform note:** on Android this is the Android Photo Picker; on iOS it is the system photo picker (`PHPicker`).

#### Scenario: Choose a photo
- **WHEN** the user taps "Add picture" and chooses a photo in the system photo picker
- **THEN** the crop screen opens with that photo

#### Scenario: No permission is requested
- **WHEN** the user chooses a photo for the first time after installing the app
- **THEN** no storage or photo library permission prompt is shown

#### Scenario: Leave the picker
- **WHEN** the user opens the photo picker and leaves it without choosing a photo
- **THEN** the form is unchanged

#### Scenario: A file that is not an image
- **WHEN** the picker returns a file that cannot be decoded as an image
- **THEN** the system shows that the picture could not be opened, does not open the crop screen and keeps the vehicle's picture as it was

#### Scenario: The photo's orientation is respected
- **WHEN** the user chooses a photo that is stored rotated and marked with an orientation
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
