# Proposal (stub)

> **Stub.** Filed as a reminder, not ready to build: the app icon SVG this depends on has not been provided yet.
> Short on purpose — it fixes the scope and lists the open questions, to be settled together with the developer once
> the artwork exists, before `/opsx:apply`. Nothing here is built yet.

## Why

The app has no custom launcher icon today (`AndroidManifest.xml` declares no `android:icon` at all) and no splash
screen (the launch window is only ever a solid background color, per `app-shell`'s "Material Design 3 theming"
requirement — no icon is ever shown during cold start). Both should show something that identifies Driving Log,
not a default/blank icon and a plain color flash.

## What Changes

- A custom launcher icon (adaptive icon: foreground + background layers) replaces the current absence of one, built
  from an SVG the developer will provide.
- A splash icon appears on the launch window during cold start (today it is background color only), built from an
  SVG the developer will provide — the same artwork as the launcher icon unless the open questions below settle on
  a simplified variant.
- iOS icon/launch screen work is out of scope until the Xcode project exists (see `app-shell`'s existing "iOS entry
  point" platform note for the established precedent of deferring iOS-specific wiring the same way).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `app-shell`: adds a launcher icon and a splash-screen icon; the shell's launch behavior today has neither.

## Impact

- `androidApp/src/main/res/`: new adaptive icon resources (vector drawables for foreground/background, and a
  monochrome layer if in scope — open question), `AndroidManifest.xml` gains `android:icon`/`android:roundIcon`.
- `androidApp/src/main/res/values/themes.xml` (and a `-v31`/API-33-appropriate variant if needed): a splash-screen
  theme, plus `MainActivity.kt` installing it (`androidx.core:core-splashscreen`'s `installSplashScreen()`).
- `gradle/libs.versions.toml`: the `core-splashscreen` dependency.
- Blocked on the developer supplying the SVG artwork; see design.md's open questions for what else needs settling
  once it exists.
