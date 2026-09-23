# Tasks

## 1. Log event form

- [ ] 1.1 In `LogEventScreen.kt`, add `!state.activeEntry.isEmpty` to the Save `TextButton`'s `enabled` expression
      (alongside the existing `!state.isLoading && !state.notFound && !state.isSaving`).
- [ ] 1.2 Update `LogEventProcessorTest.kt` (and `LogDistanceRulesTest.kt` if it separately asserts the screen-level
      behavior, not just `validateLogDistance` itself) for the field-empty case: the existing "tries to save with an
      empty field sets `error` to `FieldEmpty`" test now documents an unreachable-by-tap state; replace or annotate
      it so the suite instead asserts what's now true — Save's `enabled` expression is false while the field is
      empty (a Compose UI test) — or narrow the existing processor test to state plainly it exercises `save()`
      directly, bypassing the disabled button, and is a defense-in-depth check rather than the primary coverage.

## 2. Add-vehicle form

- [ ] 2.1 In `AddVehicleScreen.kt`, add `state.name.isNotBlank() && !state.entry.isEmpty` to the Save `TextButton`'s
      `enabled` expression.
- [ ] 2.2 Remove `AddVehicleState.nameError` and `odometerError`, the `save()` branches that set them, and the
      `NameChanged`/odometer-edit intent handlers that clear them (design.md: both fields' only failure mode was the
      same emptiness Save is now gated on). Remove the error UI those flags drove on the name and odometer fields.
- [ ] 2.3 Update `AddVehicleProcessorTest.kt`: remove or rewrite the tests asserting `nameError`/`odometerError` are
      set after a `Save` intent with an empty field, replacing them with a test of the `enabled` condition (or of a
      new small pure helper if one is introduced) confirming Save is disabled for: no name, no odometer digits, and
      both missing.

## 3. Edit-vehicle form

- [ ] 3.1 In `EditVehicleScreen.kt`, add `state.name.isNotBlank()` to the Save `TextButton`'s `enabled` expression.
- [ ] 3.2 Remove `EditVehicleState.nameError`, the `save()` branch that sets it, and the error UI it drove on the
      name field, the same way as 2.2.
- [ ] 3.3 Update `EditVehicleProcessorTest.kt` the same way as 2.3, for the name-only case.

## 4. Verification

- [ ] 4.1 Run `maestro/run.sh distance vehicles` and confirm they still pass (no flow exercises the empty-field error
      path today — confirmed by search — so none should need changes; this run is to catch a regression elsewhere).
- [ ] 4.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
