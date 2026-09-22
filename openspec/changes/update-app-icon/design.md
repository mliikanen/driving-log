# Design (stub)

## Context

`AndroidManifest.xml` declares no `android:icon`; `androidApp/src/main/res/` has no `mipmap`/`drawable` icon
resources at all. The launch window today is only `android:windowBackground = @color/window_background` on
`Theme.DrivingLog` (`DeviceDefault.DayNight` parent) — no splash-screen library is used, no icon is ever shown
before the first frame. `minSdk` is API 33 (Android 13), well above adaptive icons' API 26 floor, so there is no
need for legacy non-adaptive PNG icon resources for the app itself to run correctly (a PNG export would only matter
for a future Play Store listing, which is explicitly not happening yet per the project context).

## Decisions (proposed, to be confirmed)

1. **Adaptive icon as vector drawables**, not PNG mipmaps: `minSdk` 33 makes the legacy fallback unnecessary, and
   the project already keeps other iconography (Phosphor glyphs) as vector assets.
2. **Splash screen via `androidx.core:core-splashscreen`**: the standard, minimal way to show an icon during cold
   start on the API levels this app targets, and it composes with the existing solid-background theme rather than
   replacing it (the background-color requirement is unchanged; the icon is added on top of it).

## Open questions

1. **The SVG itself** — not provided yet. Everything below depends on what it actually looks like.
2. Is the splash icon the same artwork as the launcher icon, or a simplified variant? Android's splash-screen
   guidance generally wants a simpler mark than a full adaptive icon (tighter safe-zone, no background layer of its
   own beyond the theme's existing color).
3. Adaptive icon background layer: a solid brand color (Petroleum Deep `#0F2027`, matching the app bar, is the
   obvious candidate) or something derived from the SVG's own colors?
4. Is an Android 13+ themed/monochrome icon layer in scope for this change, or deferred? It needs a single-color
   silhouette version of the artwork, which may or may not be practical depending on the SVG's actual content.
5. iOS: deferred until the Xcode project exists, per `app-shell`'s existing "iOS entry point" precedent for
   platform-specific wiring that cannot be built or verified without a Mac.

## Risks

- **Blocked on an asset the developer has not supplied yet** — this stub exists specifically so the intent is not
  lost before that happens; nothing here should be built from a guessed-at icon.
