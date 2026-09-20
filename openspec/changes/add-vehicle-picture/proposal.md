# Proposal

## Why

Vehicles are told apart only by name and license plate, which is slow to scan in a list and gives the details screen no
face. A picture of the vehicle makes each vehicle recognizable at a glance, and later (picking a vehicle when logging,
matching a vehicle by photo) it is the natural thing to show. Users can add it now, while adding or editing a vehicle.

## What Changes

- The **add vehicle** and **edit vehicle** screens get an optional **picture**: an "Add picture" action (which becomes
  "Change picture" and "Remove picture" once there is one) and a preview of the picture.
- The user **selects the source photo with the system photo picker** (the Android Photo Picker, which needs no storage
  permission; the iOS photo picker on iOS). The app never asks for access to the photo library.
- After selecting, the user **must crop the photo to a square**: a crop screen shows the photo under a fixed square frame,
  the user pans and pinches to position and zoom it, and confirms with "Use photo" or cancels (cancel discards the selection and
  leaves the vehicle's picture as it was). The crop cannot be skipped and the frame cannot leave the photo, so the result
  is always a full square of the photo.
- The confirmed crop is stored as **two square versions of the same picture**: a **small** one (up to 256 x 256 px) for lists and
  pickers, and a **large** one (up to 1024 x 1024 px) for full screen views; neither is ever enlarged beyond the crop. Both are stored in a
  **size-efficient format: lossy WebP** (quality 80, transparency kept), which is a few tens of kilobytes for the large one where a
  JPEG or PNG of the same photo is several times that.
- The pictures are **stored in the application's private local file system**, not in the database and not in the
  device's photo library. The database only remembers which picture belongs to the vehicle (an id), so nothing that is
  stored depends on a file path. Replacing or removing the picture deletes the old files; files that no vehicle uses are
  cleaned up.
- The **vehicle list** shows the small picture next to each vehicle (a neutral placeholder when there is none). The
  **vehicle details screen** shows the large picture at the top. Nothing else about either screen changes.
- Saving a vehicle with a picture is **atomic**: the vehicle, its initial odometer event and its picture are saved
  together or not at all; a failed save leaves no files behind. Editing the picture does not touch the vehicle's log.
- Storage moves to **schema version 4** (migration `3.sqm`: one nullable `picture_id` column on the vehicle). Existing
  vehicles have no picture.
- Out of scope: taking a photo with the camera (the system picker may offer it, the app adds nothing), rotating, filters
  or free-form cropping, several pictures per vehicle, a zoomable full screen viewer, pictures in the log or in
  distance entries, syncing pictures to a backend, and verifying the iOS side on a device (there is no Xcode project yet).

## Capabilities

### New Capabilities
- `vehicle-picture`: choosing a vehicle's picture with the system photo picker, cropping it to a square, the two stored
  sizes and format, local file storage and clean-up, and where each size is shown.

### Modified Capabilities
- `vehicles`: adding and editing a vehicle offer the picture, the vehicle list shows the small picture, and the details
  screen shows the large picture.
- `vehicle-log`: editing a vehicle's picture, like its name and plate, leaves the log unchanged.

## Impact

- `shared/` commonMain: the `Vehicle` model gains `pictureId`; a `VehiclePictureStore` interface (files) and a pure crop model
  (`CropState`: scale, offset, the source rectangle a crop means) with its unit tests; picture state and intents in the add and edit
  processors (a pending picture kept as temporary files so it survives rotation and process death); the crop screen; picture
  composables in the list, details, add and edit screens; `SqlDelightVehicleRepository` writes, replaces and removes pictures;
  migration `3.sqm` and schema version 4.
- Platform code behind `expect`/`actual` or interfaces: a photo picker launcher, an image codec (decode with the photo's
  orientation applied and downsampled, crop and scale, encode WebP) and the file store location. Android: the Photo Picker
  contract from `androidx.activity` (already a dependency), `ImageDecoder` and `Bitmap.compress(WEBP_LOSSY)` (API 30+, the minimum
  is 33), `filesDir`. iOS: `PHPickerViewController`, `UIImage`/ImageIO, Application Support. **Platform difference:** ImageIO can read WebP but not write it, so
  on iOS the pictures are written as PNG until a WebP encoder (libwebp) is added; both platforms read both formats.
- One new dependency, `kotlinx-io-core`, so the picture files are handled in common code. No image-loading or cropping library: the crop screen is a small Compose composable over a pure, tested model.
- `AppGraph` provides the picture store and codec; `maestro/` gets flows for adding, changing and removing a picture.
- `openspec/config.yaml`: the project context notes that pictures are local files referenced by id and their sizes and format.
