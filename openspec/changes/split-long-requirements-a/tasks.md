# Tasks

No app code changes in this change. Every task leaves `scripts/validate-specs.sh` green. Before touching tests, read
`docs/test-strategy.md`.

## 1. Check that no behavior changed (design Decision 2)

- [x] 1.1 `refueling-logging`: compare each of the 6 MODIFIED descriptions with `main`'s text sentence by sentence,
      and the fuel type list in "The fuel type is chosen from a fixed list" with the old one word for word. Fix the
      delta where a SHALL, SHALL NOT, condition, value or list item was lost or changed. Verify: every old normative
      statement is found in the delta; any rewording that could be read differently is listed under this task.
      - Fixed: "(from the Home screen)" restored on the vehicle selector; a typed zero again says the *entry* isn't
        saved; the mileage is again saved as the event "a 'Distance' entry would save" (which decides "Distance" or
        "Odometer reading").
      - Reads differently: the mileage section's fields "carry no such asterisk" became "SHALL NOT" carry it, a
        statement made normative. It's what the scenario "The required field is marked" already requires.
- [x] 1.2 `vehicle-log`: the same for its 8 MODIFIED descriptions, with "Recent events on the details screen" checked
      against its two new requirements together. Verify: as 1.1, and the "Platform note" of "The log is not changed by
      editing the vehicle" is still in its block.
      - Fixed: "Recent events" again says its scenarios use a 24-hour setting (they still assume it).
      - Reads differently: "a 'Refueling' event does not participate" became "SHALL neither set nor add to the
        odometer" (made normative, same meaning); "an event written before time zones were stored has no zone"
        became "an event stored without a zone", which only those older events are.
- [x] 1.3 `odometer-ocr-capture`: the same for its 10 MODIFIED descriptions, checking the two split requirements
      against their three new ones together, and the label lists (volume, odometer, trip) word for word. Verify: as
      1.1.
      - Fixed: the live scanner again SHALL *offer* a close action (the short text only said what it does).
      - Reads differently: "no magnitude fallback and no plausible-range check" is now covered by "SHALL NOT use
        magnitude" alone; the add-vehicle scan's four rules ("discards", "keeps only the latest", "no screen shows",
        "does not remove") are bound by "SHALL store them ... under the rules of a scan accepted on the log event
        form" instead of a SHALL each; the fuel-amount MAY's explanation (nearest label only) and its pointer to
        "Several candidates" became SHALLs in the new requirement (design Decision 2).
- [x] 1.4 Check the lengths once the deltas are final. Verify: `openspec validate split-long-requirements-a --strict`
      passes; `node scripts/check-requirement-length.mjs` passes; an archive in a scratch copy of `openspec/` leaves
      0 requirements over 500 characters in the three specs and 52 in all (`openspec validate --specs --strict`).

## 2. A test for each new scenario (design Decision 4)

For each, find the test that already checks it (the lowest kind that can, `docs/test-strategy.md`); add one only
where none does. A scenario the app turns out not to meet is rewritten to what the old requirement says, never the
app changed to fit it.

- [x] 2.1 `refueling-logging`, "The whole list for an unfiltered vehicle": an "Other" vehicle is offered all ten fuel
      types. Look in `VehicleFuelTypeTest`, `FuelTypeTest` and `LogEventProcessorTest`. Verify: the test is named here,
      or a new one passes in `./gradlew :shared:allTests`.
      - Existing: `VehicleFuelTypeTest.otherOffersEveryFuelTypeUnfiltered` (an "Other" vehicle gets every entry).
        Added: `FuelTypeTest.theListIsTheTenFuelTypesInOrder` (the entries are the ten of the list, in order).
- [x] 2.2 `vehicle-log`, "A refueling from another time zone in the full log" and "An odometer reading's row has no
      plus sign". Look in `EventRowContentTest`, `FormatMomentTest` and `VehicleLogProcessorTest`. Verify: as 2.1.
      - Existing: `EventRowContentTest.anAnchorRowIsLabelledOdometerReadingAndShowsItsReadingWithoutAPlusSign`.
        Added: `EventRowContentTest.aRefuelingFromAnotherZoneShowsItsZone` (the zone tests only used distance rows).
- [x] 2.3 `odometer-ocr-capture`, "The user picks the amount over the price": confirming one of two fuel-amount
      candidates sets the fuel amount to it. Look in `LogEventProcessorTest` and `ReadingDetectionTest`. Verify: as 2.1.
      - Added: `LogEventProcessorTest.theUserPicksTheAmountOverAPriceThatSharesTheVolumeLabel` (no test had a price
        sharing the volume label).
- [x] 2.4 `odometer-ocr-capture`, "A mileage scan that set a first odometer" (the scan goes under the "Odometer
      reading" event) and "A scanned refueling's details show no scan". Look in `SqlDelightVehicleRepositoryTest` and
      `EventDetailsProcessorTest`. Verify: as 2.1.
      - Existing: `SqlDelightVehicleRepositoryTest.aScannedRefuelingsMileageIsSavedWithItsScan` (an `Anchor` mileage's
        scan is on the "Odometer reading" event). Added: `EventDetailsProcessorTest.aRefuelingsDetailsShowOnlyItsAttachedPhotos`;
        no screen reads a stored scan back (`captureOf` is called only by the repository and its tests).

## 3. Regression run

- [x] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh`, without Maestro (no flow is touched). Verify: both pass.
