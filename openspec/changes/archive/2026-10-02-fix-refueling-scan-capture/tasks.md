# Tasks

## 1. Repository

- [x] 1.1 Add `capture: PendingCapture?` to `VehicleRepository.addRefueling`'s signature, matching
      `addDistanceEntry`/`addOdometerAnchor`'s existing parameter exactly. Verify: `./gradlew :shared:compileKotlinAndroid`
      fails at every call site that doesn't yet pass it (expected, fixed by the remaining tasks).
- [x] 1.2 In `SqlDelightVehicleRepository.addRefueling`, promote the capture and store it with
      `eventCaptures.insertEventCapture` inside the same transaction as the event, deleting it on a failed save —
      the identical pattern `addDistanceEntry`/`addOdometerAnchor` already use. Verify:
      `SqlDelightVehicleRepositoryTest` — a refueling saved with an accepted capture can be read back via
      `captureOf(eventId)`; a refueling saved with no capture reads `null`; a failed save (the existing
      `fail_update` trigger pattern already used for other repository tests) leaves no orphaned capture photo.

## 2. The log event form

- [x] 2.1 Pass `capture = form.scan.accepted` from both `LogEventProcessor.saveRefueling` and
      `saveConfirmedLowerOdometerRefueling`'s `repository.addRefueling(...)` calls, matching
      `saveDistance`/`saveConfirmedLowerOdometerDistance`'s existing calls. Verify: `LogEventProcessorTest` — a
      refueling saved after accepting a scanned mileage reading (both the no-mileage/with-mileage and the
      lower-odometer-confirmed paths) passes the accepted capture through to the repository; a refueling saved
      with the reading typed by hand passes none.

## 3. Test fakes

- [x] 3.1 Add a `capture: PendingCapture? = null` field to `FakeVehicleRepository`'s `RefuelingCall` and its
      `addRefueling` override, mirroring `DistanceCall`/`AnchorCall`'s existing ones. Verify: the task 2.1 tests
      above compile and pass against the fake.

## 4. Verification

- [x] 4.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass before archiving. (No Maestro task: the stored capture is never shown in any user-facing
      screen, per `odometer-ocr-capture`'s own requirement, so there is nothing for a Maestro flow to assert that
      the unit/integration tests above don't already cover more precisely.)
