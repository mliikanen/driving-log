# Tasks

## 1. Storage

- [x] 1.1 Add a `selectEventById` SQLDelight query and `VehicleRepository.observeEvent(vehicleId, eventId):
      Flow<VehicleEvent?>` (SqlDelight and Fake implementations). Verify with a unit test that it returns the
      matching event, updates when the event changes, and returns `null` for an unknown id.

      Revised during implementation: "updates when the event changes" has no update query to exercise yet
      (`vehicle_event` is still append-only; `add-event-editing` adds one). Tested instead that the flow is
      query-backed (a fresh read sees a row that started existing after the first read), plus the matching-event and
      unknown-id/other-vehicle cases. All pass (`SqlDelightVehicleRepositoryTest`, `FakeVehicleRepository`).

## 2. Screen

- [x] 2.1 Add `EventDetailsNavKey`, a details processor and screen showing kind, full date/time/zone, figure and
      note (omitted for "Initial odometer"), backed by `observeEvent`. Verify with unit tests of the processor's
      state per `docs/test-strategy.md` (Kide MVI contract: initial state, updates when the observed event changes).

      New package `vehicle.eventdetails` (`EventDetailsContract.kt`/`Processor.kt`/`Screen.kt`), registered nav key,
      wired into `AppGraph`. The screen reuses `eventRowContent` (the row's own formatter) for kind/moment/figure so
      it never drifts from the row; the note section renders only when `event.note != null`, which is already always
      false for "Initial odometer" at the domain level — no extra branching needed. `EventDetailsProcessorTest`
      passes; `:androidApp:assembleDebug` succeeds with the new DI wiring.
- [x] 2.2 Give `EventRow` a tap handler that opens the details screen, wired from both the recent events section and
      the full log. Verify with a Compose integration test (or the existing `EventRowContentTest` tier, whichever
      `docs/test-strategy.md` calls for) that tapping a row invokes the handler with that event's id.

      Revised during implementation: `docs/test-strategy.md` marks Compose integration tests "Not yet adopted", and
      `EventRowContentTest` only covers the pure `eventRowContent` formatter, not the composable's interaction — so
      the wiring itself is verified two ways instead: a `VehicleDetailsProcessorTest` unit test that the new
      `EventClicked` intent produces `ShowEventDetails`, and Maestro (task 3.1) for the actual tap. `EventRow` gained
      an `onClick: (String) -> Unit` param (`Modifier.clickable`); `VehicleLogScreen` got a direct
      `onShowEventDetails` callback (its processor has no intents to route through); `VehicleDetailsScreen` got a
      new `EventClicked`/`ShowEventDetails` intent/effect pair, matching its existing pattern.

## 3. Verification

- [x] 3.1 Add a case to the `distance` Maestro manifest: log a distance entry with a note, open its details from the
      recent events, confirm the fields and note text are shown, go back, confirm the list is shown again. Run
      `maestro/run.sh distance` and confirm it passes.

      Revised during implementation: added the case to the *full log* (not recent events, both already open the same
      screen) in `log-distance.yaml`, after the note tests. Debugging a real flakiness took several iterations —
      root cause: at that point in the flow the log holds all 5 recent events (the display cap), so "View full log"
      scrolls to its lowest possible position, within the device's bottom `mandatorySystemGestures` inset, where
      taps are not reliably delivered; every earlier tap of that button in this flow happens with only 4 events,
      safely above the inset, so this was the first time the flow ever scrolled to it at the full cap. Fixed with
      `centerElement: true` on that `scrollUntilVisible`. Two other hardenings added along the way (a generous
      `extendedWaitUntil` instead of a plain `assertVisible` after the details screen's own back navigation, for its
      real but harmless crossfade transition) remain as defensive insurance. `maestro/run.sh distance`:
      `log-distance (3m 12s)`, `log-from-home (2m 28s)`, both passed.
- [x] 3.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.

      Confirmed: `BUILD SUCCESSFUL` and `openspec validate --all --strict` reports `17 passed, 0 failed` (only
      pre-existing INFO-level "requirement text is very long" notices, unrelated to this change).
