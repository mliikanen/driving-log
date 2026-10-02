# Proposal

## Why

`odometer-ocr-capture` already lets a photo fill in an odometer or trip-distance field instead of typing it, using
on-device OCR to find numeric readings and classify them by adjacent label text or magnitude. A fuel pump or
receipt display showing the amount dispensed is the same kind of problem — a numeric reading in a photo the user
would otherwise type by hand — and this was the OCR use case named when `add-refueling-logging` was first proposed
(the fuel-amount case, distinct from the fuel-type case `add-fuel-type-ocr` covers).

## What Changes

- The refueling form's fuel amount field gets its own "Scan a reading" action, next to the field itself (the
  mileage section below it already has its own, unchanged) — the same live-scanner/photo-chooser/review pipeline
  `odometer-ocr-capture` already built, reused rather than re-implemented.
- Detection adds a third reading kind, "fuel amount," alongside the existing odometer and trip kinds
  (`ReadingKind.FUEL_AMOUNT`). Unlike those two, a fuel-amount candidate is recognized **by label only** — a volume
  label ("L", "LITERS", "LITRES", "GAL", "GALLON", "GALLONS", "VOLUME") next to the number. There is no magnitude
  fallback: odometer/trip classification can fall back to a number's size because a known odometer gives it
  something to compare against, but a fuel amount has no such anchor (a pump's volume and its total price are
  often similar-looking numbers with nothing but their labels to tell them apart). An unlabeled number, or one next
  to any other label (including a price label like "$", "PRICE" or "TOTAL"), is simply never presented as a
  fuel-amount candidate — this also means a price number is never mistaken for one, without needing to recognize
  "this is a price" as its own concept.
- Accepting a fuel-amount candidate fills the fuel amount field (always two decimal places, `refueling-logging`'s
  own `FuelAmountEntry`), the same way accepting an odometer/trip candidate fills its field today.
- The live scanner and review screen, which today label a highlighted box "ODO" or "TRIP", gain a third label
  ("FUEL") for a fuel-amount candidate.

## Capabilities

### Modified Capabilities
- `odometer-ocr-capture`: the same capability that already added this action to the add-vehicle form's initial
  odometer (`scan-initial-odometer`) and reads LCD displays (`add-seven-segment-ocr`) now also offers it on the
  refueling form's fuel amount field, with its own, label-only classification rule for that one field. (It keeps
  its existing name — already a little broader than literally "odometer" after those two changes — rather than
  renaming it mid-series.)

`refueling-logging` itself needs no change: the fuel amount field, its validation and its unit already exist there;
this only adds another way to fill it, exactly as `odometer-ocr-capture`'s "Scan a reading" action needed no change
to `distance-logging` when it was added there.

## Impact

- `ReadingKind` (`shared/.../vehicle/ocr/ReadingDetection.kt`) gains `FUEL_AMOUNT`; the two UI spots that today
  assume only `ODOMETER`/`TRIP` exist (`LiveScannerContent.kt`, `ScanReviewContent.kt`, both a binary `if`/`else`)
  become exhaustive on the three kinds — the compiler enforces this once the enum grows.
- A new `detectFuelAmount(photo): List<Detection>` function, alongside the existing `detectReadings`/
  `detectInitialOdometer`, sharing their label/box/`onePerPlace` machinery but with its own, label-only rule (no
  `knownOdometer` parameter, since there is no magnitude fallback).
- `LogEventState`/`LogEventProcessor`/`LogEventScreen`: a `scanTarget` (mileage or fuel amount) remembers which
  field the currently-open scan is for, since the refueling form now has two independent "Scan a reading" actions
  sharing the same live scanner/review machinery (today's single scan state assumes only one target per form).
  Accepting a reading still dispatches on the detection's own kind, now with a third case.
- No evaluation photos of a fuel pump or receipt display exist yet in `maestro/assets/ocr/` (only `odo/` and
  `trip/`) — gathering some, and measuring the label words real displays actually use, is part of this change's own
  tasks, the same evidence-driven approach `add-seven-segment-ocr` took for LCD odometers.
