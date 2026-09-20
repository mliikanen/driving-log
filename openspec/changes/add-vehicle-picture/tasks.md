# Tasks

## 1. Data and files

- [x] 1.1 Add migration `3.sqm` (`vehicle.picture_id` TEXT, nullable), schema version 4, `Vehicle.pictureId`, and `picture_id` in the vehicle insert, update, list and details queries (a picture change bumps `updated_at`), and verify the JVM migration test for 3 to 4, 1 to 4, 2 to 4 and a fresh version-4 schema (data intact, `picture_id` null)
- [x] 1.2 Add the `kotlinx-io-core` dependency, the `VehiclePictureStore` interface and `FileVehiclePictureStore` over a root path (`pictures/`, `pictures/pending/`; put pending source, put pending versions, promote to a new id, read a size, delete, discard pending, sweep), and verify tests with real files in a temporary directory: put and read both sizes, promote moves the files under a new id, a missing file reads as null, delete removes both sizes, sweep deletes unreferenced pictures and pending files older than 24 hours and keeps the rest
- [ ] 1.3 Add `PendingPicture` and `PictureChange` (`Keep`, `Remove`, `Replace`), and make `addVehicle` and `updateVehicle` promote the pending picture, write `picture_id` in the transaction, delete the promoted files if the transaction fails and the old files after it succeeds; update the fake repository, and verify repository tests on real SQL: add with a picture, add without, replace (old files deleted, new id used), remove, keep, a failed transaction leaves no new files and the old picture in use, the log is unchanged by a picture edit
- [ ] 1.4 Run the sweep once when the app starts (the referenced ids come from the vehicles), and verify a test that files of no vehicle are deleted and the pictures of vehicles are kept

## 2. Picture logic

- [ ] 2.1 Add the pure `CropState` and `CropRect` (start centred at the largest square, `panBy` and `zoomBy` clamped so the square stays inside the image, the zoom limits, the integer rectangle) and verify unit tests for landscape, portrait, square and very small images, every edge clamp, zoom out past the start, the maximum zoom, and pan then zoom
- [ ] 2.2 Add `pictureSides` (small at most 256, large at most 1024, never enlarged), the `ImageCodec` interface (`decode` with orientation applied and downsampled to 3072 px, `encodeSquare` with the extension of what it wrote), `DecodedImage` and `EncodedImage`, and a fake codec for tests, and verify unit tests for the sizes (large crop, crop of 400, crop of 200, exactly 256 and 1024)
- [ ] 2.3 Add `PictureDraft` (`None`, `Unchanged`, `Removed`, `Pending`) and `PictureDraftEditor` (pick a photo into a pending source, an unreadable photo, confirm a crop into pending versions, cancel a crop, remove, discard on leaving), and verify unit tests with the fake store and codec including that the state holds only ids
- [ ] 2.4 Use the draft in the add and edit processors and states (serializable, `cropSourceId`, `pictureError`, new intents, save passes the picture to the repository, leaving without saving discards the pending files), and verify processor tests with kide-test: add with a picture, cancel adding, edit change, edit remove then save, remove then leave, unreadable photo, a state restored with a pending id and with a crop source id, a failed save keeps the draft

## 3. Platform code

- [ ] 3.1 Add the Android `ImageCodec` (`ImageDecoder` with orientation and downsampling, crop and scale, `Bitmap.compress(WEBP_LOSSY, 80)`) and the Android picture root (`filesDir`), and verify it compiles and that a debug check on the emulator writes WebP files of the expected sizes (see 5.2)
- [ ] 3.2 Add the photo picker (`expect`/`actual`: Android `PickVisualMedia` image only, reading the result into bytes with a size limit, no permission; iOS `PHPickerViewController`), and verify it compiles for Android and iOS
- [ ] 3.3 Add the iOS `ImageCodec` (ImageIO decode with orientation, crop and scale, PNG encode) and the iOS picture root (Application Support), and verify it compiles for iOS (it cannot be run until the Xcode project exists)
- [ ] 3.4 Provide the store and the codec from `AppGraph` (handed in by the shells like the driver and the device locale) and update the graph creation in the Android application, the iOS view controller and the tests

## 4. Screens

- [ ] 4.1 Add Coil 3 (`coil-compose` 3.6.3, no network module) and verify it compiles for Android and iOS with this project's Kotlin and Compose versions, then add the `PictureModel` with its Coil fetcher and keyer over the picture store, the app's `ImageLoader` (bounded memory cache, set as the singleton, created from the graph) and the `VehiclePicture` composable (`AsyncImage`, the placeholder with the first letter of the name while loading, for no picture and for missing or unreadable files), use it in the vehicle list rows and at the top of the details screen with the test tags from the design, and verify tests for the fetcher (saved and pending pictures, a missing file fails, keys differ by id and size) and that it compiles for Android and iOS
- [ ] 4.2 Add the picture row (96 dp preview, "Add picture", "Change picture", "Remove picture", the unreadable-photo message) to the add and edit screens, and the full-screen `CropScreen` (image under a square frame, drag and pinch, "Use photo", "Cancel", back cancels), and verify it compiles for Android and iOS
- [ ] 4.3 Check the picture flow by hand on the emulator (list, details, add, edit, crop) in light and dark mode, in landscape and with the keyboard open, and fix what looks wrong

## 5. Maestro flows

- [ ] 5.1 Add flows for adding a vehicle with a picture (`addMedia`, the Photo Picker, drag the crop, "Use photo", the picture in the list and on the details screen), changing and removing it, cancelling the crop, leaving the add screen without saving, keeping the picture after a restart, working offline, and a rotation with a confirmed and an unsaved picture and with the crop screen open, and verify they pass
- [ ] 5.2 Add `maestro/picture/run.sh` that runs a picture flow and then checks the app's files with `adb shell run-as`: two files for a picture, WebP magic bytes, the dimensions read from the header (256 and at most 1024), the large file under the size bound, no files of a removed or replaced picture, and none left after cancelling, and verify it passes

## 6. Project context and final verification

- [ ] 6.1 Add to the project context in `openspec/config.yaml` that pictures are local files in app-private storage referenced by an id (never a path), stored small and large in a size-efficient format, and verify `openspec validate --all --strict` passes
- [ ] 6.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug`, the whole Maestro suite, `maestro/picture/run.sh` and `openspec validate --all --strict`, and verify all pass
