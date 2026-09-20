# iosApp

The Xcode project is not generated yet. Create it on a Mac (Xcode → new iOS App named `iosApp`),
then link the `Shared` framework produced by `:shared`
(`./gradlew :shared:embedAndSignAppleFrameworkForXcode`) and call `MainViewController()` from SwiftUI.
Before wiring this up, add a `MainViewController` (`ComposeUIViewController { App() }`) to `shared/src/iosMain`.
