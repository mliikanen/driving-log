# Tasks

No app code changes in this change. Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read
`docs/test-strategy.md`.

## 1. Check that no behavior changed (design Decision 2)

- [ ] 1.1 `refueling-logging`: compare each of the 6 MODIFIED descriptions with `main`'s text sentence by sentence,
      and the fuel type list in "The fuel type is chosen from a fixed list" with the old one word for word. Fix the
      delta where a SHALL, SHALL NOT, condition, value or list item was lost or changed. Verify: every old normative
      statement is found in the delta; any rewording that could be read differently is listed under this task.
- [ ] 1.2 `vehicle-log`: the same for its 8 MODIFIED descriptions, with "Recent events on the details screen" checked
      against its two new requirements together. Verify: as 1.1, and the "Platform note" of "The log is not changed by
      editing the vehicle" is still in its block.
- [ ] 1.3 `odometer-ocr-capture`: the same for its 10 MODIFIED descriptions, checking the two split requirements
      against their three new ones together, and the label lists (volume, odometer, trip) word for word. Verify: as
      1.1.
- [ ] 1.4 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-a --strict`
      passes; `node scripts/check-requirement-length.mjs` passes; an archive in a scratch copy of `openspec/` leaves
      0 requirements over 500 characters in the three specs and 52 in all (`openspec validate --specs --strict`).

## 2. A test for each new scenario (design Decision 4)

For each, find the test that already checks it (the lowest kind that can, `docs/test-strategy.md`); add one only
where none does. A scenario the app turns out not to meet is rewritten to what the old requirement says, never the
app changed to fit it.

- [ ] 2.1 `refueling-logging`, "The whole list for an unfiltered vehicle": an "Other" vehicle is offered all ten fuel
      types. Look in `VehicleFuelTypeTest`, `FuelTypeTest` and `LogEventProcessorTest`. Verify: the test is named here,
      or a new one passes in `./gradlew :shared:allTests`.
- [ ] 2.2 `vehicle-log`, "A refueling from another time zone in the full log" and "An odometer reading's row has no
      plus sign". Look in `EventRowContentTest`, `FormatMomentTest` and `VehicleLogProcessorTest`. Verify: as 2.1.
- [ ] 2.3 `odometer-ocr-capture`, "The user picks the amount over the price": confirming one of two fuel-amount
      candidates sets the fuel amount to it. Look in `LogEventProcessorTest` and `ReadingDetectionTest`. Verify: as 2.1.
- [ ] 2.4 `odometer-ocr-capture`, "A mileage scan that set a first odometer" (the scan goes under the "Odometer
      reading" event) and "A scanned refueling's details show no scan". Look in `SqlDelightVehicleRepositoryTest` and
      `EventDetailsProcessorTest`. Verify: as 2.1.

## 3. Regression run

- [ ] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro (no flow is touched). Verify: both pass.
