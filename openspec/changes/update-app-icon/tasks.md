# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Get the launcher/splash icon SVG(s) from the developer, settle the open questions in design.md (splash vs.
      launcher artwork relationship, adaptive icon background color, whether a monochrome/themed variant is in
      scope), update the proposal, the spec delta and these tasks to match, then verify with `openspec validate
      update-app-icon --strict`.

## 2. To be planned after 1.1

- [ ] 2.1 Build adaptive icon resources (foreground/background vector drawables, monochrome layer if in scope) from
      the SVG and wire `android:icon`/`android:roundIcon` in `AndroidManifest.xml` (planned in detail when 1.1 is
      done). Remove the `tools:ignore="MissingApplicationIcon"` suppression (and its comment) from `<application>`,
      added by `clean-up-lint-baselines` until the icon exists; `./gradlew codeQuality` must still pass.
- [ ] 2.2 Add `androidx.core:core-splashscreen`, a splash theme layered on the existing `Theme.DrivingLog`, and
      install it in `MainActivity.kt` (planned in detail when 1.1 is done).
- [ ] 2.3 Verify on-device that the launcher shows the new icon and cold start shows the splash icon; final
      regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`).
      Note iOS icon/launch screen wiring stays deferred until the Xcode project exists.
