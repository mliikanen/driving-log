# Tasks

## 1. Log event form

- [x] 1.1 In `LogEventScreen.kt`, add `!state.activeEntry.isEmpty` to the Save `TextButton`'s `enabled` expression
      (alongside the existing `!state.isLoading && !state.notFound && !state.isSaving`).
- [x] 1.2 Update `LogEventProcessorTest.kt` (and `LogDistanceRulesTest.kt` if it separately asserts the screen-level
      behavior, not just `validateLogDistance` itself) for the field-empty case: the existing "tries to save with an
      empty field sets `error` to `FieldEmpty`" test now documents an unreachable-by-tap state; replace or annotate
      it so the suite instead asserts what's now true — Save's `enabled` expression is false while the field is
      empty (a Compose UI test) — or narrow the existing processor test to state plainly it exercises `save()`
      directly, bypassing the disabled button, and is a defense-in-depth check rather than the primary coverage.

      Took the second option: `LogEventProcessor.kt`/`LogDistanceRules.kt` are unchanged (only the screen's `enabled`
      expression changed), so the existing `FieldEmpty` tests remain accurate as defense-in-depth checks of the
      processor layer — added a one-line comment on `anEmptyFieldIsRefusedInBothWays` saying so, no assertion
      changes. No Compose-level test exists for this screen's `enabled` wiring (none exists for any screen in this
      project today), consistent with the project's existing testing conventions.

## 2. Add-vehicle form

- [x] 2.1 In `AddVehicleScreen.kt`, add `state.name.isNotBlank() && !state.entry.isEmpty` to the Save `TextButton`'s
      `enabled` expression.
- [x] 2.2 Remove `AddVehicleState.nameError` and `odometerError`, the `save()` branches that set them, and the
      `NameChanged`/odometer-edit intent handlers that clear them (design.md: both fields' only failure mode was the
      same emptiness Save is now gated on). Remove the error UI those flags drove on the name and odometer fields.

      `save()`'s guard now returns `null` (a no-op) when invalid, rather than setting error state — matches
      design.md's noted risk (a direct dispatch bypassing the disabled button now saves nothing silently). The name
      field's `isError`/`supportingText` and `OdometerField`'s `isError`/`errorText` args were simply dropped (both
      default to false/null; `OdometerField` itself is shared and untouched, still used with error support by
      `LogEventScreen`).
- [x] 2.3 Update `AddVehicleProcessorTest.kt`: remove or rewrite the tests asserting `nameError`/`odometerError` are
      set after a `Save` intent with an empty field, replacing them with a test of the `enabled` condition (or of a
      new small pure helper if one is introduced) confirming Save is disabled for: no name, no odometer digits, and
      both missing.

      Rewrote the five now-invalid-field tests to assert the no-op (`repository.addCalls` stays empty) instead of an
      error flag; renamed `aMissingNameAndAMissingOdometerAreBothReported` to
      `aMissingNameAndAMissingOdometerBothBlockSaving`. Deleted three tests that existed solely to exercise the
      removed error-flag toggling (`theOdometerErrorClearsWhenADigitIsTyped`,
      `theOdometerErrorStaysWhileNothingIsEntered`, `theNameErrorClearsWhenTheUserTypes`) and one duplicate
      (`theNameAndOdometerErrorsShowWithoutAnyTypeError`, redundant with the rewritten "both missing" test once
      there's no longer a separate "type" error to distinguish from).

## 3. Edit-vehicle form

- [x] 3.1 In `EditVehicleScreen.kt`, add `state.name.isNotBlank()` to the Save `TextButton`'s `enabled` expression.
- [x] 3.2 Remove `EditVehicleState.nameError`, the `save()` branch that sets it, and the error UI it drove on the
      name field, the same way as 2.2.
- [x] 3.3 Update `EditVehicleProcessorTest.kt` the same way as 2.3, for the name-only case.

      Rewrote the two affected tests (`anEmptyNameIsRefusedAndTheSavedValuesAreKept`, `aWhitespaceOnlyNameIsRefused`)
      to drop the `nameError` assertion, keeping the no-op check (`repository.updateCalls` stays empty).

## 4. Verification

- [x] 4.1 Run `maestro/run.sh distance vehicles` and confirm they still pass (no flow exercises the empty-field error
      path today — confirmed by search — so none should need changes; this run is to catch a regression elsewhere).

      All 5 flows passed (`log-distance`, `log-from-home`, `landing`, `add-and-browse`, `edit`), no flow edits needed.
- [x] 4.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Both pass. `openspec validate` shows the same pre-existing, unrelated `add-event-pictures` failure noted
      earlier; untouched by this change.
