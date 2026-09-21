# iosApp

The Xcode project is not generated yet. Create it on a Mac (Xcode → new iOS App named `iosApp`),
then link the `Shared` framework produced by `:shared`
(`./gradlew :shared:embedAndSignAppleFrameworkForXcode`) and call `MainViewController()` (defined in `shared/src/iosMain`) from SwiftUI.

## What the Xcode project must contain for vehicle pictures

The picture source sheet (Take Photo, Photo Library, Choose File) uses the system's own screens. Only taking a photo needs a permission, and the app
asks for it at the moment the user chooses Take Photo, not before. Add this key to the app's `Info.plist`, or iOS ends the app at that moment:

| Key | Text shown when camera access is asked for |
| --- | --- |
| `NSCameraUsageDescription` | "Driving Log uses the camera to take a picture of your vehicle." |

The photo library (`PHPicker`) and the file picker need no permission and no `Info.plist` key. Pictures are stored in Application Support, which is
private to the app.

## Status bar over the dark header

Every screen's app bar is a dark header (Petroleum Deep) in both light and dark mode. The status bar above it must therefore show **light content**
(white icons and text) in both modes: return `UIStatusBarStyleLightContent` from the hosting view controller's preferred status bar style (or set
`UIStatusBarStyle` to `UIStatusBarStyleLightContent` in `Info.plist` with `UIViewControllerBasedStatusBarAppearance` off). Without it, the light-mode status bar
shows dark icons on the dark header and is unreadable. On Android the app configures this itself.
