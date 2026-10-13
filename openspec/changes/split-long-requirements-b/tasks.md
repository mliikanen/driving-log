# Tasks

Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read `docs/test-strategy.md`.

## 1. Check that no behavior changed

- [ ] 1.1 `vehicle-picture`: compare each of the 6 MODIFIED descriptions with `main`'s text sentence by sentence,
      the three split requirements against their 8 new ones together, and check the two platform notes are below their
      scenarios word for word. Fix the delta where a SHALL, SHALL NOT, condition or value was lost. Verify: every old
      normative statement is found in the delta; any rewording that could be read differently is listed here.
- [ ] 1.2 `vehicle-fuel-type`, `vehicle-type` and `firebase-auth`: the same, with the code lists checked word for word.
      Verify: as 1.1.
- [ ] 1.3 `licensing` and `event-details`: the same. Verify: as 1.1.
- [ ] 1.4 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-b --strict`
      and `node scripts/check-requirement-length.mjs` pass; an archive in a scratch copy of `openspec/` keeps every
      scenario, leaves 0 requirements over 500 characters in the six specs and 36 in all.

## 2. A test for each new scenario

For each, name the test that checks it (the lowest kind that can, `docs/test-strategy.md`), or add one.

- [ ] 2.1 `vehicle-fuel-type`, "Every fuel type's code", and `vehicle-type`, "Every type's code". Verify: the tests
      are named here, existing or new, and pass in `./gradlew :shared:allTests`.
- [ ] 2.2 `vehicle-picture`, "Changing a picture by tapping it" and "A file that is not an image when changing a
      picture". Verify: as 2.1.
- [ ] 2.3 `vehicle-picture`, "Taking a photo on Android asks for nothing": `maestro/check-permissions.sh` (the app
      requests no system permission) or a test of the manifest. Verify: the check is named here.
- [ ] 2.4 `vehicle-picture`, "The crop screen's app bar", "Zoom with the keyboard", "Two turns", "The buttons stay
      clear of the navigation bar" and "The crop survives a process restart". Verify: as 2.1, or the device check named
      here where no automated test can check it.
- [ ] 2.5 `event-details`, "The photo cap on the edit screen", "A photo added from the refueling row is seen from the
      mileage row" and "A refueling's edit screen". Verify: as 2.1.
- [ ] 2.6 `licensing`, "Material under the project's own license" and "A dependency no checked-in material needs":
      check `THIRD_PARTY_NOTICES.md`. Verify: the entries are quoted here.

## 3. Regression run

- [ ] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro. Verify: both pass.
