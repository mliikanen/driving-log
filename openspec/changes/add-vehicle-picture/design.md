# Design

## Context

- A vehicle is a row in `vehicle` (schema version 3: `id`, `name`, `license_plate`, `odometer_unit`, `created_at`, `updated_at`,
  `log_distance_tenths`). `addVehicle` inserts the row and the initial odometer event in one transaction; `updateVehicle`
  changes name and plate and bumps `updated_at`. The add and edit screens are Kide processors with serializable states
  (`AddVehicleState`, `EditVehicleState`) that survive rotation and process death by restoring the state; anything `@Transient`
  is rebuilt, so a picture the user has cropped but not saved must live somewhere the state can point to.
- The list (`VehicleListScreen`) and the details screen (`VehicleDetailsScreen`) draw from `Vehicle` / `VehicleDetails`.
- `shared` has commonMain UI (Compose Multiplatform 1.12, Material3), `expect`/`actual` only where the platform is needed
  (`DatabaseDriverFactory`, `IoDispatcher`), and platform objects are handed to the graph by the shells
  (`createAppGraph(driver, deviceLocale)`). minSdk is 33, so `Bitmap.CompressFormat.WEBP_LOSSY` (API 30) and the Photo Picker
  (`ActivityResultContracts.PickVisualMedia`, in `androidx.activity`, already used) are always available.
- The project rules: stored data is locale-agnostic and does not depend on the device; every state-changing action goes through a change
  in `openspec/changes/`; business logic lives in commonMain and is tested there; Android is primary, iOS opportunistic.

## Goals / Non-Goals

**Goals:**
- Add, change and remove a vehicle's picture from the add and edit screens, with the system photo picker and a mandatory square crop.
- Store a small and a large version in a size-efficient format in app-private files, referenced from the database by id only.
- Show the small version in lists and pickers and the large one on the details screen.
- Keep every save atomic and leave no stray files; keep the picture flow testable in commonMain (crop maths, sizes, processors, repository).

**Non-Goals:**
- Camera capture by the app, rotation, filters, free-form or non-square crops, several pictures, a zoomable viewer, backend sync,
  pictures elsewhere in the app, an iOS WebP encoder, and verifying iOS on a device.

## Decisions

### 1. Data: one nullable `picture_id` on the vehicle, files named by it

Migration `3.sqm`: `ALTER TABLE vehicle ADD COLUMN picture_id TEXT;` and schema version 4. `picture_id` is a UUID naming a set of files,
`null` for no picture. `Vehicle` gains `pictureId: String?`; `selectVehicles`, `selectVehicleDetails` and the insert and update queries carry it. A picture
is **replaced by a new id, never overwritten**: a changed picture is written under a new id, the row is pointed at it, and only then are the old files deleted. That
makes the swap atomic from the database's point of view, means a cached bitmap keyed by id can never be stale, and lets a failed
save be undone by deleting the new files. Editing the picture bumps `updated_at`, like a name or plate edit; it adds no log event.

The picture is an attribute of the vehicle, not a log record, so the additive-log principle (no running totals) is not affected.

### 2. Files: `VehiclePictureStore` over kotlinx-io, with a `pending` area

`VehiclePictureStore` (domain interface, `commonMain` implementation `FileVehiclePictureStore(root: Path)`) owns a directory `pictures/` under the
app's private files directory. The only platform piece is the root path (Android `context.filesDir`, iOS Application Support), handed to the graph
by the shell. The implementation uses **kotlinx-io** (`SystemFileSystem`, `Path`, sources and sinks, `atomicMove`) so file handling is plain common code and
is unit-tested with real files in a temporary directory. This is the one new dependency (`org.jetbrains.kotlinx:kotlinx-io-core`); the alternative, `expect`/`actual` file
code per platform, is more code to get wrong twice.

Layout: `pictures/{id}-small.{ext}`, `pictures/{id}-large.{ext}` for pictures in use, and `pictures/pending/{pendingId}-source`, `-small.{ext}`, `-large.{ext}` for
pictures the user is still working with. `ext` is the encoder's extension (`webp` or `png`, decision 4); `read` looks for either, so a file written by another platform or an
older build is still found. Operations: `putPendingSource(bytes)`, `readPendingSource`, `putPending(small, large)`, `promote(pendingId): pictureId` (an atomic move into `pictures/` under a new
id), `read(id, size): ByteArray?`, `delete(id)`, `discardPending(pendingId)`, and `sweep(referencedIds)`. The store also turns a picture into a **URI**: `uri(pictureId, size): String?` and `pendingUri(pendingId, size): String?`
(`file://` plus the percent-encoded absolute path: the iOS Application Support directory has a space in its name), `null` when the file is not there. The store is the one place that knows where files live and in which format, so the processors never build a path.

**Sweep** runs once when the app starts (from `App`, on the IO dispatcher): it deletes every picture in `pictures/` whose id no vehicle refers to, and pending files
older than 24 hours. Pending files younger than that are kept because a restored form (process death) may still point at them.

### 3. The pending picture: files, not state

While the user works on the form, the picture is a **draft** in the serializable state:

`PictureDraft = Unchanged | Removed | Pending(pendingId)` (`Unchanged` only on the edit screen; the add screen starts with "no picture", which is `Removed`-equivalent `None`) plus `cropSourceId: String?` while
the crop screen is open, and, for the view, `previewUri: String?` (`@Transient`, rebuilt from the draft by the processor whenever the draft changes and after a restore). Picking a photo writes its bytes to `pending/{id}-source` and sets `cropSourceId`; the crop screen decodes from that file. Confirming the crop encodes both versions,
writes them as `pending/{pendingId}-...`, deletes the source and sets `Pending(pendingId)`. So a rotation or process death at any point restores the crop screen or the
picture, because the state only holds ids and the bytes are on disk. Cancelling the crop deletes the source. Leaving the add or edit screen without saving discards
the pending files (the processor's clean-up on leaving; the sweep is the backstop).

The logic shared by both screens (pick, crop, confirm, remove, error) is one small class, `PictureDraftEditor`, that takes the store and the codec and returns new draft values; the add and
edit processors call it and never re-implement it, as with `OdometerEntry`.

### 4. The image codec: platform decode and encode, common maths

`ImageCodec` (interface; Android and iOS implementations, handed to the graph by the shells):
- `decode(bytes: ByteArray): DecodedImage?` decodes with the photo's EXIF orientation applied and downsampled so the longer side is at most **3072 px**, and returns
  the size and an `ImageBitmap` for the crop screen; `null` when the bytes are not an image. Android: `ImageDecoder` (it applies orientation and can downsample). iOS: `UIImage`/ImageIO with a thumbnail size limit.
- `encodeSquare(bytes, crop: CropRect, targetSides: List<Int>): List<EncodedImage>` decodes again, crops the rectangle (expressed in the decoded image's pixels), scales it to each target side, and encodes.
  Android: `Bitmap.compress(WEBP_LOSSY, 80)`; alpha is kept. iOS: PNG, because ImageIO cannot write WebP (a libwebp binding is the follow-up); `EncodedImage` carries its extension.

The target sides come from the pure function `pictureSides(cropSide)`: `small = min(256, cropSide)`, `large = min(1024, cropSide)`. Never enlarging keeps small crops honest and the files small.
A 3072 px decode is enough for a 1024 px large version even when the user zooms in to a third of the photo; the decode is bounded (about 28 MB as a bitmap).

**Why lossy WebP at 80:** the pictures are photographs of a vehicle shown at most at phone-screen size. Lossy WebP at that quality is roughly a quarter to a third of the size of a JPEG of similar quality and
much smaller than PNG (which is lossless and stores photos in megabytes), keeps alpha, and decodes everywhere the app runs. PNG stays the iOS fallback because it is the only size-acceptable format ImageIO
can write; storage is a per-file extension, so a WebP encoder on iOS later changes nothing else.

### 5. The crop: a pure model and a small composable

`CropState` (pure, commonMain) holds the decoded image's width and height, a `zoom` (at least 1, at most `maxZoom`) and the crop square's centre in image pixels.
The square's side is `min(width, height) / zoom`, so zoom 1 is the largest square and the frame always fits the photo. `panBy(dxImagePx, dyImagePx)` and `zoomBy(factor, focus)` return new states with the
centre clamped so the square stays inside the image; `maxZoom` keeps the side at or above `min(128, min(width, height))` pixels. `cropRect()` returns the integer `CropRect(x, y, side)` in image pixels.
It starts at zoom 1 centred, so the first frame is the largest centred square.

`CropScreen` (composable, full screen over the add or edit screen) shows the image under a square frame with the outside dimmed, converts drag and pinch gestures
(`detectTransformGestures`) from screen pixels to image pixels using the frame's on-screen side, and offers "Use photo" and "Cancel" (and a back gesture that cancels). It is a composable inside the screen, not a navigation
destination, because its input is the draft in the state; that keeps the flow local and restorable. No third-party cropper is used: the maths is small and fully unit-tested, and a library would bring its own UI.

### 6. The photo picker

`rememberPhotoPicker(onResult: (ByteArray?) -> Unit): PhotoPicker` is an `expect`/`actual` composable. Android: `rememberLauncherForActivityResult(PickVisualMedia())` with `ImageOnly`, and the result Uri
is read into bytes immediately (the picker's grant is short-lived) on the IO dispatcher. iOS: `PHPickerViewController` (single selection, images) with its item provider loaded as data. Neither asks for a permission.
`null` means the user left the picker. Bytes larger than a limit (say 40 MB) are refused as unreadable, which keeps the in-memory copy bounded.

### 7. Repository and atomic saves

`VehicleRepository.addVehicle(name, plate, unit, odometer, picture: PendingPicture? = null)` and `updateVehicle(id, name, plate, picture: PictureChange = PictureChange.Keep)` where
`PictureChange` is `Keep | Remove | Replace(pending)`. The repository takes the `VehiclePictureStore` and, for a pending picture, does in order:
1. `promote(pendingId)` (atomic move into `pictures/`, new id);
2. the database transaction (vehicle and initial event, or the update), writing `picture_id`;
3. on failure of the transaction: `delete(newId)` and rethrow; on success: delete the old picture's files (best effort, the sweep catches a failure).

A crash between 1 and 2 leaves only unreferenced files, which the sweep removes. `Remove` sets `picture_id` to `NULL` then deletes the files. The repository never reads the image; it only moves files and writes an id.

### 8. Showing the picture: a URI in the view state, loaded by Coil 3

**The processors' view states carry a picture URI, not an id and not bytes.** Each state that shows a picture has a `pictureUri: String?` (a plain string, so it serializes and compares) for the size that screen shows: the list items and the pickers the small
version's URI, the details state the large version's, the add and edit forms the preview's (the small version of the pending or the saved picture). Today the URI points at the locally stored file (`file:///.../pictures/{id}-small.webp`); later the same field can hold an `https://` URI of an image
behind HTTP, and no screen changes. The database still stores only the picture id (decision 1); the processors turn ids into URIs through the store (decision 2), and `null` means no picture (or a file that is gone).

Pictures are then shown with **Coil 3** (`io.coil-kt.coil3:coil-compose`, 3.6.3, published for Android and iOS arm64 and simulator): coroutine-based, Kotlin Multiplatform, Compose Multiplatform `AsyncImage`. Glide was considered and rejected: it is Android-only (no iOS, nothing in commonMain). Kamel and Landscapist are
alternatives of the same shape; Coil is the most widely used. Because the model is just a URI, **no custom fetcher, model or keyer is needed**: Coil loads `file:` URIs natively (its decoders read WebP and PNG, downsampling to the size the composable is laid out at) and, once a network module is added
(`coil-network-ktor3`, not now), `http(s)` URIs too. Coil's default cache key for a string model is the URI itself; a picture's id never changes what its files hold (a changed picture has a new id), so the **memory cache is always correct** and no disk cache is configured.

- **Composable.** `VehiclePicture(uri: String?, modifier)` wraps `AsyncImage`: while loading, on error (an unreadable file) and for a `null` URI it draws the placeholder (a `null` URI makes no request), decision 12. It is used by the list rows
  (56 dp square), the details screen (full width at a 1:1 aspect ratio, content scale `Crop`) and the form preview (96 dp). Test tags: `vehicle_picture` (list rows), `vehicle_picture_large` (details), `picture_preview`, `add_picture`, `remove_picture`, `crop_frame`, `crop_confirm`, `crop_cancel`.
- **One `ImageLoader`** is created by the app and set as Coil's singleton (`setSingletonImageLoaderFactory`) with a bounded memory cache.
- **What Coil does not do.** It does not crop or encode, and the crop screen needs the decoded pixels and the photo's dimensions, so the crop screen decodes through our `ImageCodec` (decision 4); Coil is only the display path.

### 12. The placeholder: a generic car icon

Until vehicles have a type, every vehicle without a picture shows the same **generic car icon**: a rounded square in `surfaceVariant` with the car glyph tinted `onSurfaceVariant`, scaled to the picture's size. The glyph is `car-fill` from the **Phosphor** icon set
(github.com/phosphor-icons/core), a single filled path on a 256 x 256 view box, **MIT licensed**: the license text is kept beside the icon (`docs/icons/phosphor/LICENSE`, with the original `car-fill.svg`) and named in `THIRD_PARTY_NOTICES.md` at the repository root, which is what the MIT license asks for. Phosphor was chosen over CC0 collections
(FreeSVG, SVG Silh: mixed styles and unclear per-file provenance) because it is one consistent, maintained set that also has the vehicle icons the later type change needs (van, truck, motorcycle, scooter, jeep, bus, tractor), also MIT, so that change adds icons without changing the style or the licensing.

The SVG is drawn in the app as an `ImageVector` built from the SVG's path data (`PathParser`, plain common Compose code), not loaded as a file at run time: Compose Multiplatform resources render SVG on iOS and desktop but not on Android, and an SVG decoder module would be a
dependency for one glyph. Building the vector from path data is tintable, has no loading step, and needs nothing per platform. The original `.svg` stays in `docs/icons/` as the source. A single `VehicleIcons.Car` is used everywhere; the vehicle type change replaces it by a lookup on the type.

### 9. The form

Add and edit screens get a row with the preview (96 dp) and the actions: "Add picture" when there is no picture, otherwise "Change picture" and "Remove picture". "Remove picture" only changes the draft; the saved vehicle changes on Save. An unreadable photo
shows a short message under the preview (`pictureError` in the state, cleared on the next action). Save is disabled while a crop is in progress (the crop screen covers the form anyway).

### 10. Migration and its test

`3.sqm` adds one nullable column; a JVM test extends `VehicleMigrationJvmTest` with 3 to 4 (data intact, `picture_id` null), 1 to 4, 2 to 4, and a fresh version-4 schema.

### 11. Testing

- Pure, in commonTest: `CropState` (start centred, the clamp on every side, zoom limits, pan then zoom, the rectangle for landscape, portrait and square images, a very small image), `pictureSides`, the draft editor.
- Store: real files in a temporary directory with kotlinx-io (put, promote, read, delete, sweep including the 24-hour rule, a missing file reads as `null`).
- Repository on real SQL with a fake image-free store or the real store: add with a picture, replace (old files deleted, new id), remove, a failed transaction deleting the promoted files, edit without picture change keeps the id, the row and the files agree.
- Processors with kide-test and a fake codec and store: pick, unreadable photo, crop confirm, cancel, remove, rotate (restore state holding a pending id and a crop source id), leaving without saving discards files, save calls the repository with the right change.
- The platform codec and picker cannot run in host tests (no Android bitmap classes), so they are verified on the emulator: Maestro flows that add a photo with `addMedia`, choose it in the Photo Picker, drag the crop, confirm and see the picture in the list and on the details
  screen, change and remove it, and a script (`maestro/picture/run.sh`) that runs a flow and then inspects the app's files with `adb shell run-as`: two files per picture, WebP magic bytes (`RIFF....WEBP`), the dimensions read from the WebP header, and the large file under the size bound.

## Risks / Trade-offs

- **The photo picker in Maestro.** The Android Photo Picker is a system UI; its selectors (the content description of a photo) can differ by image version. The flows use `addMedia` and select the first item; if that proves flaky the fallback is to drive it with `adb` input events from the script.
- **Memory of big photos.** The picked bytes (bounded at 40 MB) and one decode (bounded by 3072 px) are in memory at once. Acceptable for a single picture; not for batch use.
- **Crop precision.** Cropping from a 3072 px decode loses some detail on zoomed-in crops of very large photos, so the large version can be below 1024 px for a tight crop. Accepted; decoding the crop region from the original file is the follow-up if it matters.
- **iOS stores PNG.** Larger files on iOS until an encoder is added, and the "well under 200 kilobytes" figure applies to Android. The spec says so.
- **Lossy re-encoding.** A photo is compressed once (WebP quality 80); the small version is made from the decoded original crop, not from the large one, so it is not compressed twice.
- **Files and rows can disagree** (a file deleted by the user through a file manager, a crash between steps). The rows are the truth: a missing file shows the placeholder, and unreferenced files are swept.
- **Two new dependencies.** kotlinx-io is small and maintained with kotlinx-datetime and serialization (only `core`). Coil 3 is larger; it is used only for display, without its network modules, and its compatibility with Kotlin 2.4.20 and Compose Multiplatform 1.12.0 is confirmed by compiling first (task 4.1).
