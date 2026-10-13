# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [x] 1.1 `app-shell`: compare each of the 5 MODIFIED descriptions with `main`'s text sentence by sentence, the split
      ones against their new requirements together, with the palette's names and codes checked character for
      character. Verify: every old normative statement is found in the delta; any rewording that could be read
      differently is listed here.
      - Clean: every palette name and hex code of the old text is in the new one. Reads differently: the dark
        scheme's "so that buttons, icons and switches stand out from it" lost "from it"; the Home screen's tile size and
        disabled actions are each their own requirement.
- [x] 1.2 `vehicle-color` and `event-pictures`: the same. Verify: as 1.1.
      - Fixed: "Vehicles from before colors existed SHALL get the default color on the update" (a first draft lost
        "on the update"). Reads differently: the presets' "(a preset's swatch when the current color is that preset,
        otherwise the segment of that color)" is now "A current color that is not a preset SHALL be selected as its
        segment" plus "the one of the vehicle's current color".
- [x] 1.3 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-d --strict`
      passes, and an archive in a scratch copy of `openspec/` keeps every scenario and leaves 0 requirements over 500
      characters in the whole project (`openspec validate --all --strict` passes there).
      - 12 changed, 12 added; every scenario kept; `openspec validate --all --strict` passes on the scratch archive.

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one; a device
check is recorded where only the screen can show it.

- [x] 2.1 `app-shell`, the theme scenarios ("The light scheme's accents and dividers", "A primary button in light
      mode", "System bar icons in dark mode"). Verify: the tests are named here, existing or new, and pass in
      `./gradlew :shared:allTests`, or the device check is recorded.
      - Existing: `ThemeColorsTest.theLightSchemeUsesThePaletteWhereTheDocumentSaysSo` (secondary Refinery Teal,
        outlineVariant Exhaust Fog) and `everyTextPairMeetsTheRuleInTheLightScheme` ("onPrimary on primary", 4.5:1).
      - "System bar icons in dark mode": device check on the `dl34` emulator in night mode: the window sets neither
        `APPEARANCE_LIGHT_STATUS_BARS` nor `APPEARANCE_LIGHT_NAVIGATION_BARS`, and the screenshot shows light icons on
        both bars.
- [x] 2.2 `app-shell`, the permission and Home screen scenarios ("Opening a form that can scan asks nothing", "Tapping
      the edge of a tile", "After the first vehicle is added", "A screen reader reads a disabled action"). Verify: as
      2.1.
      - "Opening a form that can scan asks nothing": device check with CAMERA revoked: the log event form opens, shows
        "Scan a reading", and no permission prompt appears.
      - "Tapping the edge of a tile": device check: a tap 12 px inside the first tile's corner opens the vehicle list.
      - "After the first vehicle is added": existing `LandingProcessorTest.theTileFollowsAVehicleBeingAdded`.
      - "A screen reader reads a disabled action": existing `LandingProcessorTest.theTripAndPlaceholderTilesAreNeverEnabled`
        and `thePlaceholdersHaveTheirNamesAndIcons`; device check: `landing_trip` is `enabled=false` and labelled
        "Trip".
- [x] 2.3 `vehicle-color` scenarios ("A saved color that is not a preset", "Saving applies the chosen color", "The same
      photo twice", "Choosing presets leaves the photo color alone", "A saved color reaching the details screen").
      Verify: as 2.1.
      - Existing: `ColorChoiceTest.aSavedColorThatIsNotAPresetIsTheSelectedOldColor`,
        `EditVehicleProcessorTest.changingTheColorSavesIt`, `ColorExtractorTest.theSameSamplesGiveTheSameColorTwice`,
        `ColorChoiceTest.choosingAPresetAfterThePhotoKeepsThePhotoColorOfferedButNotSelected`.
      - "A saved color reaching the details screen": device check (design Decision 3): a screen recording of saving
        Red for a vehicle without a picture shows the details screen's icon container moving from the old color to
        the new one over about 14 frames (0.45 s at 30 fps), from the color it showed, without a jump. No automated
        test can check it without making `AnimatedColorState` testable.

## 3. Strict spec validation again

- [x] 3.1 Make `scripts/validate-specs.sh` run `openspec validate --all --strict --no-interactive` alone, and delete
      `scripts/check-requirement-length.mjs` and `scripts/check-requirement-length.test.mjs` (design Decision 2).
      Verify: `scripts/validate-specs.sh` passes on the archived specs of a scratch copy and fails on one with a
      requirement over 500 characters; `node --test 'scripts/*.test.mjs'` passes.
      - `scripts/validate-specs.sh` passes on a scratch copy with this change archived (14 specs) and exits 1 once a
        `licensing` requirement is padded past 500 characters; `node --test 'scripts/*.test.mjs'`: 39 pass. On the branch
        it fails until the archive commit, since the main specs still hold the long requirements.
- [x] 3.2 Update `.github/workflows/pr-check.yml`'s spec-validation job (no full history needed, comments say what
      runs), `docs/test-strategy.md`'s Spec validation section and `docs/change-workflow.md`'s check list. Verify: no
      file outside `openspec/changes/archive/` mentions `check-requirement-length` or the lenient main-spec step.
      - Only `docs/test-strategy.md`'s history paragraph and the vendored OpenSpec skill files (third-party, unchanged)
        mention the old steps.

## 4. Regression run

- [x] 4.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.

      - Gradle part passes. `scripts/validate-specs.sh` is strict now, so it passes once the specs are archived; it is
        run again on the archive commit.