# Design

## Context

The baselines as written by `add-lint-quality-gates`:

| Tool | Entries | Breakdown |
|---|---|---|
| detekt (`config/detekt/baseline.xml`) | 157 | MagicNumber 79, ReturnCount 20, TooGenericExceptionCaught 9, TooManyFunctions 9, CyclomaticComplexMethod 7, LongMethod 7, SwallowedException 7, LargeClass 4 (all test classes), DestructuringDeclarationWithTooManyEntries 4, ComplexCondition 2, EmptyFunctionBlock 2, MatchingDeclarationName 2, NestedBlockDepth 2, UnusedParameter 2, LoopWithTooManyJumpStatements 1 |
| Android lint, app (`androidApp/lint-baseline.xml`) | 5 | OldTargetApi, ChromeOsAbiSupport (both `build.gradle.kts`), MissingApplicationIcon (manifest), UseKtx (`SharedPreferencesClaimedAccountStore`), CredentialManagerMisuse (`FirebaseAuthRepository`) |
| Android lint, shared (`shared/lint-baseline.xml`) | 5 | UseKtx ×4 (`AndroidImageCodec`, `MlKitTextRecognizer`), RememberReturnType (`App.kt`) |

The MagicNumber entries cluster in OCR (`PpOcr`, `Geometry`, `RgbImage`), color math (`Contrast`, `LerpHct`,
`VehicleTones`, `Rgb`, `HctColors`) and units (`OdometerUnit`, `FuelUnit`, `OdometerFormat`). The exception
findings are the MVI processors' `catch (throwable: Throwable)` around repository calls, plus decoding fallbacks in
`AndroidImageCodec`, `PhotoDecoding`, `PhotoPicker.android` and `CombinedTextRecognizer`.

## Goals / Non-Goals

**Goals:**
- Zero baseline entries, then no baseline files, with no behavior change the user can see.
- Each project-wide reconfiguration states the convention it follows, so it reads as policy, not as a way around a
  finding.

**Non-Goals:**
- Raising thresholds just to make findings disappear. A threshold changes only where the project's code style
  makes the default wrong everywhere (decisions 2 and 4), and never for a single file.

## Decisions

### 1. Order: lowest-risk first, each group its own commit, the gate green after every group

Mechanical fixes (lint UseKtx, magic numbers as named constants, file renames for MatchingDeclarationName) come
before refactors (complex and long functions). Each group regenerates the baselines and confirms only removals, so a
group can't hide a new finding behind an old entry. The baseline files are deleted only at the end, once empty.

### 2. Guard-clause returns: reconfigure `ReturnCount`

Most of the 20 ReturnCount entries are processors' `save` actions and validators that return early on each invalid
input, which is the project's MVI style. `config/detekt/detekt.yml` sets `ReturnCount.excludeGuardClauses: true`
(early returns at the top of a function don't count) and `max: 3`, with a comment. Entries still reported after
that are fixed by restructuring the function.

### 3. `catch (throwable: Throwable)` in processors and the repository: one undo-and-rethrow helper

**Corrected during apply.** The first version of this decision assumed these catches swallowed cancellation and
proposed a `catchingFailures` helper returning `Result`. In fact every one of the 13 sites undoes something
(resets `isSaving`/`isScanning`, deletes files promoted for a save that didn't happen) and then rethrows the same
throwable, so cancellation already propagated. Undoing on cancellation is also wanted: a cancelled save must not
leave the form stuck on "saving" or files orphaned. A `Result`-returning helper would have changed that behavior.

Instead, commonMain has `undoOnFailure(undo) { block }` (`util/UndoOnFailure.kt`): it runs the block and, if it throws
anything, runs `undo` and rethrows the same throwable unchanged. Its single `catch (Throwable)` carries the one
`TooGenericExceptionCaught` suppression, with that reason. Unit tests cover success (no undo), an exception (undo,
same instance rethrown) and a `CancellationException` (undo, still propagates). The processors (`AddVehicleProcessor`,
`EditVehicleProcessor`, `EventDetailsProcessor`, `LogEventProcessor`) and `SqlDelightVehicleRepository` use it at all
13 sites. Behavior is unchanged.

Decoding fallbacks that deliberately turn any failure into "no picture" or "no reading"
(`AndroidImageCodec`, `PhotoDecoding`, `CombinedTextRecognizer`) catch `Exception` and are suppressed at the
catch, with a comment naming the fallback. `PhotoPicker.android`'s specific catches (`ActivityNotFoundException`,
`IOException`, `SecurityException`) log or return the cause instead of swallowing it, or are suppressed where the
platform gives nothing useful to keep.

**During apply:** `PhotoPicker`'s causes are suppressed: `PhotoResult.Unreadable` has nowhere to carry one and the app
keeps no log. `FirebaseAuthRepository` had two baselined findings not listed above:
- `SignInCancelledException` now takes the original `GetCredentialCancellationException` as its cause.
- `signIn`'s generic catch now rethrows `CancellationException` first. Before, a cancelled sign-in turned into an
  ordinary failure result. The remaining catch, turning every other failure into the `Result` the sign-in screen
  reports, is suppressed with that reason.

### 4. Size and complexity rules: tests excluded, `when` over intents simplified, ported algorithms suppressed

- `LargeClass` and `DestructuringDeclarationWithTooManyEntries`: excluded for test source sets in
  `detekt.yml`. Test classes grow with cases, and `val (l, t, r, b)` is the clearest way to read a box in a test.
  `Geometry.kt`'s five-way destructuring is fixed.
- `CyclomaticComplexMethod`: `ignoreSimpleWhenEntries: true`. A processor's `map` is a `when` over intents with
  one call per branch, which isn't complex. Entries still reported (the big screens) are fixed by extracting
  sub-composables.
- `LongMethod` (screens): fixed by extracting sub-composables (sections of the form, the app bar). These are
  structural moves inside one file with no change to semantics or test tags.
- `PpOcr.kt` and `Geometry.kt` (CyclomaticComplexMethod, NestedBlockDepth, ComplexCondition,
  LoopWithTooManyJumpStatements): these are ports of PaddleOCR's DB post-processing and OpenCV's `minAreaRect` and
  perspective transform. They're kept structurally parallel to the reference so they can be compared line by line
  when results differ. Suppressed at each function, with a comment naming the reference. Their magic numbers still
  become named constants (decision 5).
- `TooManyFunctions` on `VehicleRepository`, its implementations and `PictureStore`: one repository per aggregate,
  one function per query or command. Suppressed on those declarations with that reason. `LogEventScreen.kt` (the
  file) and `ReadingDetection.kt` are split into files by concern instead.
- **During apply:**
  - Three more TooManyFunctions findings, suppressed with their reasons the same way: `FilePictureStore` (implements
    `PictureStore`), `LogEventProcessor` (one action builder per intent and save path, how this project's processors are
    written) and `CropState` (one function per crop operation), plus the `PpOcr` object itself (the ported pipeline's
    steps).
  - `LogEventScreen.kt` became `LogEventScreen.kt` (screen, form, its parts and dialogs wiring), `LogEventChoices.kt`
    (selectors), `LogEventFields.kt` (moment row, fuel amount, known odometer, error text), `EventNoteAndPhotos.kt` (the
    note and photo fields shared with the event details screen) and `LogEventDialogs.kt`.
  - `ReadingDetection.kt` gave up `TextBoxGeometry.kt` (row and place tests on boxes) and `ReadingLabels.kt` (the label
    sets and label lookup).
  - Declarations that moved between files changed from `private` to `internal` (still within the module).
  - ktlint 1.8 doesn't report unused imports, so the imports each new file inherited were pruned by name.
  - Maestro `appearance` failed intermittently after the screen splits (1 pass in 5 runs of `color-from-photo`,
    against 2 of 2 on the commit before this change). Probing both builds showed the same layout: the "Photo color"
    swatch's position after `scrollUntilVisible` varies run to run in both, and selecting it works once it's fully on
    screen. The flow tapped the swatch while it sat at the screen's bottom edge, where the tap's center lands in the
    system's gesture area. Its two scrolls to the swatch now use `centerElement: true`, and the manifest passed three
    runs in a row.

### 5. Magic numbers become named constants next to their use

Each number gets a `private const val` (or a companion constant) whose name says what it is:
`SRGB_LINEAR_THRESHOLD = 0.03928`, `RELATIVE_LUMINANCE_RED = 0.2126`, `HUE_FULL_TURN = 360.0`,
`MILLIS_PER_MINUTE = 60_000L`. Constants for a published formula (WCAG relative luminance, sRGB) cite it in a
comment. No new shared constants file: a constant lives next to its only use.

**During apply:** one exception. `applyTransform` (`Geometry.kt`) reads `m[0]` to `m[8]` as the elements of a row-major
3×3 matrix, written as the formula reads. Naming each index doesn't read better than the formula, so that function
is suppressed with that reason. `perspectiveTransform` names the system's shape instead (`CORNERS`, `UNKNOWNS`), and
`corners()` was rewritten to take the left and right pairs instead of indexing four points. Also, the two `0xFF`
channel masks in `PpOcr`/`RgbImage` showed up as new MagicNumber findings once their expressions' other numbers were
named (one baseline entry had covered the whole expression), and they became `CHANNEL_MASK`.

### 6. Android lint, one decision per finding

| Finding | Resolution |
|---|---|
| UseKtx ×5 | Fixed: `SharedPreferences.edit {}`, `Bitmap.scale(...)`. `scale` uses the same filtering flag as the current `createScaledBitmap` call. |
| RememberReturnType (`App.kt`, `remember(graph) { registerVehicleNavKeys(graph) }`) | Fixed. The registration has to happen during composition, before the back stack is restored, so it stays synchronous. It becomes a `remember` that returns the registration (or `Unit`-free equivalent) rather than moving to an effect. A Maestro run of `vehicles` covers restoring the back stack. |
| CredentialManagerMisuse | Fixed: an explicit `catch (e: NoCredentialException)` before the generic catch, mapped to the same failure result as today, with a comment. Behavior unchanged. |
| OldTargetApi (app) | Reconfigured: disabled in `androidApp`'s `lint {}`, with the same reasoning as `NewerVersionAvailable`. It fires whenever a newer SDK exists, and a `targetSdk` bump is its own change. |
| ChromeOsAbiSupport | Reconfigured: disabled in `androidApp`'s `lint {}`. Release builds are arm64-only on purpose (testers' phones; `build.gradle.kts` explains), and the app doesn't target ChromeOS. |
| MissingApplicationIcon | Suppressed: `tools:ignore="MissingApplicationIcon"` on `<application>`, with an XML comment pointing at `update-app-icon`. That change's tasks are updated to remove the suppression when it sets the icon. If `update-app-icon` is applied first, there's nothing to do here. |

### 7. Remaining small ones

- EmptyFunctionBlock and UnusedParameter (`AuthRepositoryFactory`, `CurrentActivityHolder`): the `fake` and
  `production` flavors share a signature, and one side has nothing to do. The block gets a comment saying so (detekt
  doesn't count a commented block as empty), and the unused parameters are suppressed with the same reason.
- MatchingDeclarationName (`LiveCamera.kt` declares `CameraPermission`, `TimeZoneChoices.kt` declares
  `ZoneChoice`): the file is renamed after its declaration.
  **Changed during apply:** both files are named after their main function (`LiveCameraPreview`, with its
  `LiveCamera.android.kt`/`LiveCamera.ios.kt` actuals; `timeZoneChoices()`, with `TimeZoneChoicesTest`), and their one
  class is a helper. Renaming them after the class would break that pairing, so each has a file-level suppression with
  that reason instead. `CurrentActivityHolder`'s empty bodies are lifecycle callbacks the interface requires:
  `EmptyFunctionBlock.ignoreOverridden: true` in `detekt.yml`, with that reason.

## Risks / Trade-offs

- [A refactor changes behavior subtly (screens, processors).] → Existing processor tests and Maestro manifests
  cover the happy paths. Before refactoring a function whose behavior no test pins, one is added first. Screens are
  only split into sub-composables, keeping test tags and semantics.
- [Suppressions accumulate instead of fixes.] → Each needs a reason a reviewer can check. Design decisions 3, 4 and
  7 name the only places expected to be suppressed, and anything else needs a line in this design.
- [Conflicts with in-flight changes touching the same files (e.g. `generalize-vehicle-event-model`, which reworks
  events).] → Those are still proposals without code. Apply this before them, or rebase this change's later groups
  if one lands first.

## Migration Plan

Groups land in the order of the tasks. Rollback for any group is reverting its commit. The baseline files from
`add-lint-quality-gates` still work until the last group removes them.
