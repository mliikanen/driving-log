# Proposal

## Why

Vehicles are told apart only by name and license plate, which is slow to scan in a list and gives the details screen no
face. A picture of the vehicle makes each vehicle recognizable at a glance, and later (picking a vehicle when logging,
matching a vehicle by photo) it is the natural thing to show. Users can add it now, while adding or editing a vehicle.

## What Changes

- The **add vehicle** and **edit vehicle** screens get an optional **picture**: a preview of it (or the placeholder) that **is itself the action**: tapping the
  picture starts choosing one, with no separate button; a "Remove picture" button appears once there is a picture.
- The photo comes from **the system's own way of choosing where an image comes from**, so the user picks the app that provides it. On
  Android that is the **intent chooser** (tapping the picture opens the system chooser, which lists the apps that can supply an image: the photo
  and file apps, the camera app, and any other app that offers images); on iOS it is the **system source sheet** (Take Photo, Photo
  Library, Choose File). This includes **taking a new photo with the camera**. The app does not build its own picker.
- **Permissions are asked for only when needed and only when the user triggers the action that needs them.** The Android chooser and the
  camera app are used through intents, so the app needs no storage, photo or camera permission; on iOS the first "Take Photo" asks for
  camera access at that moment (the usage description is added to the app's `Info.plist`), and a refusal is explained, never a crash.
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
- The view states of the screens carry the picture as a **URI** (today a `file://` URI of the locally stored file; later it can be an `https://` URI of an image behind HTTP, without changing any screen). The pictures are **loaded asynchronously** (off the main thread, with a placeholder while loading and when a file is missing) and cached in memory, so lists scroll smoothly.
- The **vehicle list** shows the small picture next to each vehicle (a generic car icon as the placeholder when there is none). The
  **vehicle details screen** shows the large picture at the top. Nothing else about either screen changes.
- Saving a vehicle with a picture is **atomic**: the vehicle, its initial odometer event and its picture are saved
  together or not at all; a failed save leaves no files behind. Editing the picture does not touch the vehicle's log.
- The placeholder is a **generic car icon** (the same for every vehicle for now, an MIT-licensed glyph from the Phosphor icon set, kept with its license in the repository). Choosing a vehicle type (car, van, motorcycle, ...) with an icon
  per type is a separate, later change (`add-vehicle-type`).
- Storage moves to **schema version 4** (migration `3.sqm`: one nullable `picture_id` column on the vehicle). Existing
  vehicles have no picture.
- Out of scope: a vehicle type and per-type icons (`add-vehicle-type`), building our own camera or gallery screen (the system ones are used), rotating, filters
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
- `app-shell`: two global rules: the application requests the minimum set of permissions it needs (this change is the first to touch permissions, and satisfies the rule by needing none on Android), and every screen respects the system bars and the keyboard and scrolls clear of them (the taller add, edit and details screens made this matter).

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
- Two new dependencies: `kotlinx-io-core`, so the picture files are handled in common code, and **Coil 3** (`coil-compose`), the coroutine-based Kotlin Multiplatform image loader, which loads and
  caches the pictures asynchronously wherever they are shown (it loads the URIs from the view states directly; no custom fetcher and no network module is added). No cropping library: the crop screen is a small Compose
  composable over a pure, tested model.
- `AppGraph` provides the picture store and codec; `maestro/` gets flows for adding, changing and removing a picture.
- `openspec/config.yaml`: the project context notes that pictures are local files referenced by id and their sizes and format.
