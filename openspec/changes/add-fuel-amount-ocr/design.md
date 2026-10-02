# Design

## Context

See proposal.md for why. What exists today to build on (`shared/.../vehicle/ocr/`):

- `ReadingDetection.kt`: `ReadingKind` (`ODOMETER`, `TRIP`), `DetectionBasis`, `Detection` (one recognized number,
  its box, kind, basis, label, unit flag), `detectReadings(photo, knownOdometer)` and `detectInitialOdometer(photo)`
  — two top-level classify functions sharing the same label/magnitude/`onePerPlace` machinery, selected by the
  calling context (an existing vehicle's log vs. a new vehicle's initial odometer), not by a flag passed through.
- `ScanDraft`/`ScanEditor`/`LiveScanner`: the live-scanner-or-chooser, photo review, and accept/discard state
  machine, parameterized by whichever classify function the caller passes to `photoPicked`/the live scanner's
  constructor at the point of use.
- `LogEventState.scan: ScanDraft` is one field per form, today only ever fed `::classify` (odometer/trip,
  `LogEventProcessor.classify()`), since a "Distance" entry and the refueling form's mileage section are the only
  scannable fields and never more than one is open at once.
- `LiveScannerContent.kt`/`ScanReviewContent.kt` label a box `"ODO"` or `"TRIP"` with a binary `if`/`else` on
  `kind == ReadingKind.ODOMETER`, and `LogEventProcessor.withScannedReading` dispatches on `kind` with an exhaustive
  `when` (two branches today) to decide which field and "way" a candidate fills.
- No fuel-pump or receipt photos exist yet under `maestro/assets/ocr/` (only `odo/` and `trip/`, from
  `add-seven-segment-ocr`'s own evaluation) — unlike that change, this one has no measured evidence yet for which
  label words real displays use or how reliably they're read; task 1 below gathers it before anything is tuned
  against it.

**Known, separate limitation, not addressed here** (surfaced during this design, filed as its own follow-up by
request): `VehicleRepository.addRefueling` has no `capture` parameter, unlike `addDistanceEntry`/`addOdometerAnchor`
— so a scan accepted on the refueling form's *existing* mileage section is already silently never persisted
today, despite `odometer-ocr-capture`'s "kept only if the entry is saved" requirement. The fuel-amount scan this
change adds inherits the same gap until that's fixed separately: accepting a fuel-amount candidate fills the field
correctly, but its photo/detections are not stored with the saved refueling event.

## Goals / Non-Goals

**Goals:**
- Reuse the existing classify-function-per-caller pattern exactly: a third top-level function, not a parameter
  threaded through the shared detection machinery.
- Make the refueling form support two independent, simultaneously-present scan actions (mileage, fuel amount)
  without a user able to confuse which field a given scan session targets.

**Non-Goals:**
- Fuel *type* recognition (`add-fuel-type-ocr`, a separate proposal).
- Cost/price recognition in any form — `refueling-logging` has no cost field, so a price number is only ever
  something to correctly *not* classify as a fuel amount (resolved in the spec delta), never a value to extract.
- Fixing `addRefueling`'s missing capture parameter (see above) — filed separately.
- Marking a region of the photo, or any new recognition engine — this reuses `odometer-ocr-capture`'s existing
  pipeline unchanged; `add-seven-segment-ocr`'s design.md already settled those questions for readings generally.

## Decisions

### `ReadingKind` gains `FUEL_AMOUNT`; a new `detectFuelAmount(photo)` function, no `knownOdometer` parameter
Extending the existing enum (rather than a parallel type) keeps `Detection`/`ScanDraft`/the review screen's
rendering generic over "whatever kind this turned out to be," exactly as adding a third case already does for the
two UI spots with a binary `if`/`else` — the compiler forces both into an exhaustive `when` once the enum grows, so
nothing can silently mislabel a fuel-amount candidate as "TRIP." `detectFuelAmount` shares `labelOf`'s
label-adjacency scan and `onePerPlace`'s same-place merging with `detectReadings`, but takes no known-value
parameter at all: the next decision is why.

### No magnitude fallback for fuel amount — label only
`detectReadings`'s magnitude fallback exists because a known odometer gives an unlabeled number something to be
near. A fuel amount has no such anchor: there is nothing in the app that already knows roughly what a tank's worth
of fuel costs or weighs, and a pump display's volume and total price are both just a few digits with up to two
decimal places — frequently closer in magnitude to each other than either is to anything the app could compare
against. Leaning on label text alone (`VOLUME_LABELS`: "L", "LITERS", "LITRES", "GAL", "GALLON", "GALLONS",
"VOLUME") is narrower than odometer's fallback-capable approach, exactly as proposal.md's open question suspected —
accepted as the only sound option rather than guessing a magnitude heuristic with no anchor to ground it. A price
label (`$`, "PRICE", "TOTAL", "COST") is recognized only so a number next to one is *excluded* outright, the same
way a clock or dial number is excluded from odometer/trip detection by being neither labeled nor plausible by
magnitude — not because price itself needs its own `ReadingKind`.

### The refueling form's two scan actions share one `scan: ScanDraft`, picked apart by a new `scanTarget` field
Rather than give the fuel amount field its own, second `ScanDraft`/`ScanEditor` pair, `LogEventState` gains one more
field, `scanTarget: ScanTarget` (`MILEAGE` or `FUEL_AMOUNT`, defaulting to `MILEAGE`), set when `ScannerOpened` is
dispatched (which becomes `data class ScannerOpened(val target: ScanTarget)`, from today's parameterless object) and
read wherever a classify function is chosen: `scanPicked`'s photo-chooser path and `liveScanner()`'s live-camera
path both switch on `state.scanTarget` between `::classify` and `::detectFuelAmount`. This mirrors how `ScanDraft`
is already one instance per form rather than one per field, and avoids a second, mostly-identical review/live-scan
UI wired up for no behavioral difference once the candidates themselves carry their own, correctly-typed `kind`.
Accepting a candidate (`ScanConfirmed`/`LiveReadingTapped`) still dispatches purely on the detection's own `kind`,
unchanged in shape from today — `scanTarget` only decides what the *next* scan looks for, never what an *already
found* candidate does once accepted, so a photo that happens to contain both an odometer-shaped and a
fuel-amount-shaped number behaves correctly regardless of which action opened the scanner (whichever classify
function ran is what actually limits which kinds appear as candidates at all).

**Alternative considered**: a second, independent `ScanDraft` field for the fuel amount scan. Rejected — the two
scans can never be open at once (one live scanner, one review screen), so a second field would only ever duplicate
state that's already mutually exclusive in practice, while every other piece of scan machinery (permission
handling, the live camera, the review screen's composable) would need to be instantiated twice for no reason.

### A detected value becomes `FuelAmountEntry.steps` by padding a single fractional digit
`FuelAmountEntry` is always hundredths, regardless of unit (`refueling-logging`'s own design). A detected
`Detection.value` can have zero, one or two fraction digits (the recognizer's own regex allows up to two). Zero or
two digits map directly (`"42".."00"` → `4200`; `"42.30"` → `4230`); exactly one digit is treated as tenths and
padded with a trailing zero (`"42.3"` → `4230`), the same promotion `withScannedReading` already does for an
odometer reading with one decimal (treated as the unit-with-tenths' tenths place, never as hundredths of the whole
unit). `FuelAmountEntry(steps = ...)` is constructed directly, the same way `OdometerEntry(unit, steps)` already is
for an accepted odometer/trip reading — not via `applyEdit`, which exists for keystroke-by-keystroke editing, not
for replacing the whole value at once.

### The live scanner and review screen gain a third label, "FUEL"
Both spots' existing binary `if (kind == ODOMETER) "ODO" else "TRIP"` becomes an exhaustive `when` over the three
kinds (`"ODO"`, `"TRIP"`, `"FUEL"`) — a one-line change at each of the two call sites found, caught by the compiler
rather than needing to be hunted down by hand.

## Risks / Trade-offs

- **[Risk] The guessed label set (`L`/`GAL`/`VOLUME`/...) is unverified against any real pump or receipt photo** →
  **Mitigation**: task 1 gathers real photos (phone shots of pumps and fuel receipts, mirroring
  `add-seven-segment-ocr`'s own photo-first methodology) before the label set is finalized or shipped; this
  design's list is a starting hypothesis from common US/EU pump displays, explicitly expected to be corrected by
  that evidence, not assumed correct in advance.
- **[Risk] A pump photo's price number could still slip through if printed with a volume-sounding unit nearby by
  coincidence (e.g. a currency-per-liter unit price line)** → **Mitigation**: the gathered photos (above) should
  include at least one showing a per-unit price line, to check this concretely rather than reason about it
  abstractly; if it proves to be a real problem, excluding a number followed by a currency symbol even without an
  explicit price label is a narrow, later fix, not a reason to block this change on a hypothetical.
- **[Trade-off] A fuel-amount scan's photo/detections are not persisted (see Context)** → accepted, scoped out of
  this change by request; filed as its own follow-up.
- **[Trade-off] No Maestro coverage is added for a feature with no real test photo in the repository yet** → the
  final task gathers photos and a unit-level fixture from them (mirroring how `add-seven-segment-ocr` checked its
  port against real photos via a JVM test, not Maestro); a Maestro flow can follow once the label set is confirmed
  against real evidence, matching `docs/test-strategy.md`'s "the lowest kind of test that can check it."
