# Tasks

## 1. Validation logic

- [ ] 1.1 In `LogDistanceRules.kt`, add `LogDistanceResult.NeedsLowerOdometerConfirmation` (no payload needed — the
      UI already has both values). Add a `lowerOdometerConfirmed: Boolean = false` parameter to `validateLogDistance`.
      For `NEW_ODOMETER` with a known odometer and `typed.meters < known.meters`: return `Anchor(typed)` when
      `lowerOdometerConfirmed` is true, else `NeedsLowerOdometerConfirmation`. Leave the equal case
      (`typed.meters == known.meters`) on its existing `Invalid(OdometerNotHigher(known))` path, unchanged.
- [ ] 1.2 Unit-test `validateLogDistance` directly: a lower, unconfirmed count returns
      `NeedsLowerOdometerConfirmation`; the same input with `lowerOdometerConfirmed = true` returns
      `Anchor(typed)`; an equal count still returns `Invalid(OdometerNotHigher(known))` regardless of the new
      parameter; a higher count is unaffected.

## 2. Contract and processor

- [ ] 2.1 Add `lowerOdometerConfirmationPending: Boolean = false` to `LogEventState`, and
      `LogEventIntent.LowerOdometerConfirmed`/`LowerOdometerCancelled` to `LogEventIntent`.
- [ ] 2.2 In `LogEventProcessor.save()`, handle the new `NeedsLowerOdometerConfirmation` result by setting
      `lowerOdometerConfirmationPending = true` instead of `error`. Add `LowerOdometerCancelled ->` clearing the
      flag, and `LowerOdometerConfirmed -> saveConfirmedLowerOdometer()`.
- [ ] 2.3 Write `saveConfirmedLowerOdometer()`: re-run `validateLogDistance` with `lowerOdometerConfirmed = true`;
      on `Anchor`, clear the pending flag and save via `repository.addOdometerAnchor` (same `saving { }` helper and
      pending-note handling the existing anchor path already uses); on anything else (defensive — the moment or
      field could theoretically have changed underneath an open dialog), clear the flag and surface whatever
      `validateLogDistance` now says instead of silently saving something unexpected.
- [ ] 2.4 Unit-test the processor: saving a lower count sets `lowerOdometerConfirmationPending` and does not call the
      repository; confirming calls `repository.addOdometerAnchor` with the typed reading and clears the flag;
      cancelling clears the flag without calling the repository, leaving the typed field untouched.

## 3. UI

- [ ] 3.1 Add a `LowerOdometerDialog` composable in `LogEventScreen.kt`, structurally identical to the existing
      `RemoveNoteDialog` (a plain `AlertDialog`, a confirm `TextButton` test-tagged e.g. `lower_odometer_confirm`,
      a dismiss `TextButton` test-tagged e.g. `lower_odometer_cancel`), naming both the typed count and the known
      odometer in its title/body. Show it when `state.lowerOdometerConfirmationPending` is true, wired to the two
      new intents.

## 4. Documentation

- [ ] 4.1 Update `VehicleRepository.addOdometerAnchor`'s doc comment: it currently says "for a new odometer count
      logged where no odometer is known," which is no longer the only case once this ships. A one-line wording fix.

## 5. Verification

- [ ] 5.1 Add a case to the `distance` Maestro manifest: log a new odometer count lower than the vehicle's current
      one, confirm the dialog appears naming both values, cancel it and confirm nothing changed, then repeat and
      confirm it — check the vehicle's current odometer updated to the lower value and a later trip distance builds
      on it correctly.
- [ ] 5.2 Run `maestro/run.sh distance` and confirm it passes.
- [ ] 5.3 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
