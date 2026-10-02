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
- Nine real photos were gathered under `maestro/assets/ocr/fuel/` (task 1.1) and run through the real
  `TextRecognizer` pipeline (`PpOcrRealPhotosJvmTest.theFuelPumpDisplays`, task 1.2) — see "The evaluation" below.
  None are in English: none of the three pumps/receipts the developer had on hand happen to be. This matters
  because it invalidated the first draft of this design, which had guessed an English-only label set
  (`L`/`GAL`/`LITERS`/...) before any photo existed to check it against.

### The evaluation (task 1.2)

| Photo | Pump / language | Volume label, as read | Price-per-unit label, as read |
|---|---|---|---|
| `fuel/DSC_0025.jpg` | Finnish, Dresser Wayne | `LITRAA` | `€` `/litra` (two tokens here) |
| `fuel/DSC_0100.jpg` | Polish, PRONAR/ZAP | `dm^3` (the exponent reads as a literal `^3`) | `zl/dm3` (one token, lowercase, no exponent) |
| `fuel/DSC_0105.jpg` | Polish, Dresser | `dm` (the exponent is lost entirely here) | `Zt/dm` (a further-garbled `zł/dm³`) |
| `fuel/DSC_0426.jpg` | Croatian | `LITARA` | (none visible in this photo) |
| `fuel/DSC_0477.jpg` | Slovenian | `UITROV` (misread of `LITROV`; `LITROV` itself reads correctly elsewhere in the same photo's fine print, just not next to the number) | `EUR/L` |
| `fuel/DSC_0896.jpg` | Finnish, truck diesel | `LITRAA` | `€/litra` (one token here) |
| `fuel/IMAG0297.jpg` | Finnish | `LITRAA` | `€/litra` |
| `fuel/IMAG0303.jpg` | Finnish, Gilbarco | *(none — no volume label is in frame at all)* | `€/L` |
| `fuel/IMAG0305.jpg` | Finnish, truck diesel | `LITE` (misread of `LITRAA`) | *(not in frame)* |

Three findings changed the design from its first draft — the first two from the raw recognized text above, the
third only showed up once `detectFuelAmount` was actually run end-to-end against these photos, not just read by eye:

1. **The label set has to be the words five different languages actually use for "liters" (and a unit symbol,
   `dm³`, for one of them), not English words** — `LITRAA` (Finnish), `LITARA` (Croatian), `LITROV` (Slovenian),
   `dm³`/`dm` (Polish, as a volume unit — 1 dm³ is exactly 1 liter, not deciliters, so no unit conversion is
   needed, only recognizing the symbol). No photo on hand uses "L", "LITERS" or a gallon word at all; those stay in
   the set on the (untested) assumption that an English or US display would use them, but every word that actual
   evidence supports is now one of the five above.
2. **A volume label's *text* never collides with its own price-per-unit line's**, even when they share the same
   word root (`dm^3` vs. `zl/dm3`; `LITRAA` vs. `€/litra`): OCR renders them as different strings in every photo
   gathered, because the price line always has a currency symbol or code fused or adjacent to the unit. A plain
   string-equality label check is never fooled by a price-per-unit line into thinking it's a volume label.
3. **A volume label's *position*, on a dense pump display, is not nearly as exclusive**: `labelOf`'s existing rule
   (inherited unchanged from odometer/trip detection) assigns a label to whichever number is *nearest* to it,
   independent of whether something else — including non-matching text like `EUROA` — sits even closer to that
   number. On a dashboard, that's rarely ambiguous: ODO and TRIP readings are usually well separated. On a pump
   display with several short lines stacked close together (amount, volume, price-per-unit, each one line), the one
   recognized volume label can end up "nearest" to two or three of them at once. Running `detectFuelAmount` on the
   actual photos (not just reading the recognized words) found this on two of the nine: `DSC_0025.jpg` classifies
   the amount (`20.00`) and an unrelated fine-print number (`5`) as fuel-amount candidates alongside the real
   reading, all three nearest to the one `LITRAA`; `DSC_0100.jpg` does the same with the amount (`5954`) and the
   price-per-liter rate (`5.20`) alongside the real reading, all three nearest to the one `dm^3`. See "More than
   one candidate can share a label" below for how this is resolved — not by tightening the geometry.

Two of the nine (`DSC_0477.jpg`, `IMAG0305.jpg`) have their volume label misread badly enough (`UITROV`, `LITE`)
that the number next to it will not be classified with the label set below — see Risks. `IMAG0303.jpg` correctly
has nothing to classify at all (no volume label is in the photo). `DSC_0105.jpg` and `DSC_0426.jpg` have a
correctly-read label but the recognizer never finds the reading's own digits at all — a different, pre-existing
recognizer-accuracy gap, not a labeling problem. `PpOcrRealPhotosJvmTest`'s `aDenseDisplayCanSurfaceMoreThanOneCandidate`/
`aSingleNearbyNumberIsTheOnlyCandidate`/`aMisreadOrMissingLabelClassifiesNothing` record exactly what each of the
nine actually produces end to end.

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
- Use the recognized label for more than just deciding a number is a candidate: also preselect the fuel unit
  (liters/gallons) it implies, so accepting a reading doesn't leave the unit choice for the user to fix by hand.

**Non-Goals:**
- Fuel *type* recognition (`add-fuel-type-ocr`, a separate proposal).
- Cost/price recognition in any form — `refueling-logging` has no cost field, so a price number is never a value
  this change extracts. It can still appear as a candidate alongside the real reading on a dense display (see "More
  than one candidate can share a label"); the goal is never presenting it as the *only*, silently-accepted value,
  not guaranteeing it never appears in the list at all.
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

### No magnitude fallback for fuel amount — label only, and no separate price-exclusion list
`detectReadings`'s magnitude fallback exists because a known odometer gives an unlabeled number something to be
near. A fuel amount has no such anchor: there is nothing in the app that already knows roughly what a tank's worth
of fuel costs or weighs, and a pump display's volume and total price are both just a few digits with up to two
decimal places — frequently closer in magnitude to each other than either is to anything the app could compare
against. Leaning on label text alone is narrower than odometer's fallback-capable approach, exactly as proposal.md's
open question suspected — accepted as the only sound option rather than guessing a magnitude heuristic with no
anchor to ground it.

`VOLUME_LABELS`, from the evaluation above, not the originally-guessed English set: `"LITRAA"`, `"LITARA"`,
`"LITROV"`, `"dm^3"`, `"dm"`, plus `"L"`, `"LITERS"`, `"LITRES"`, `"GAL"`, `"GALLON"`, `"GALLONS"` kept as an
untested but reasonable hypothesis for an English/US display, since none was available to check. A number is a
fuel-amount candidate only when one of these sits next to it (`labelOf`'s existing adjacency rule); there is no
second list of price labels to exclude by. The first draft of this design had one (a price label would actively
*exclude* a number, the way a clock excludes itself from odometer detection by being neither labeled nor
plausible) — the evaluation's finding 2 above showed the *text* collision it was guarding against doesn't happen
(a price-per-unit line never literally reads as a recognized volume word), so a second, negative list added nothing
a positive one didn't already cover *by text*. Finding 3 above is a different, *positional* problem a label
blocklist would not have fixed anyway (the money amount's number isn't near a price label at all — it's near the
*volume* label, just not as near as the real reading is) — that one is resolved by the next decision, not by a
blocklist of any kind.

### More than one candidate can share a label — resolved by the review screen, not by tighter geometry
Finding 3 above means a dense pump display can classify an unrelated number (the total amount, a price-per-unit
rate) as `FUEL_AMOUNT` alongside the real reading, because they are all "nearest" to the one visible volume label.
Two ways to close this were considered and rejected in favor of a third:
- **Tighten `labelOf`'s geometry just for fuel amounts** (a much smaller gap, or requiring horizontal alignment):
  rejected — the two problem photos (`DSC_0025.jpg`, `DSC_0100.jpg`) have the real reading and the false ones at
  *comparable* distances from the label (all in one stacked column), so a tighter threshold risks losing the real
  candidate along with the false ones, trading one failure mode for another without evidence it nets out better.
- **Make matching label-centric** (each label claims only its single nearest number, one-to-one, instead of every
  number independently finding its own nearest label): rejected for this change — it would change shared
  `labelOf` behavior that odometer/trip detection already relies on and has its own passing tests against, a
  bigger and riskier edit than this change's own scope justifies for two affected photos out of nine.
- **Accept multiple candidates and let the user pick (chosen)**: the review screen already does exactly this for
  odometer/trip detection (`odometer-ocr-capture`'s own "Several candidates" scenario) — a dense display simply
  shows more boxes, and the user, who already knows what they actually pumped, taps the right one by its value.
  This needed no code change at all: `detectFuelAmount` already behaves this way, since it was built by reusing
  `labelOf` unchanged (the first Decision above); only the spec and this design needed correcting to describe it
  accurately instead of the stricter, unverified claim the first draft made.

### A recognized label also preselects the fuel unit
Separately from deciding *which number* is a candidate, the label that made it one also says which unit it was
read in — liters for every non-English word/symbol the evaluation measured, gallons for the untested English/US
hypothesis. Accepting a fuel-amount candidate now also sets `LogEventState.fuelUnit` from `reading.label` (a small
`fuelUnitOf(label): FuelUnit?` mapping in `LogEventProcessor`, kept out of `ReadingDetection.kt` since unit
preselection is domain-specific behavior, not something the OCR layer itself needs to know about), leaving the
unit as it was when the label doesn't say either way. This was raised as an alternative to label-based *candidate*
detection (use the label only for the unit, and classify candidates by something else, such as every
plausible-looking number regardless of label) — tried against the evaluation's own recognized text and rejected:
every photo's fuel-grade price columns (`95`, `98`, two digits, no decimal) sit well inside the fuel-amount digit
bounds and would also qualify as candidates with no label filter at all, which is worse noise than the dense-display
case above, not better. Label-based candidate detection stays; unit preselection from the same label is a small,
additive improvement on top of it, not a replacement for it.

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

- **[Risk] On a dense pump display, the real reading can be classified alongside the total amount and/or a
  price-per-unit rate, all under the same label** (`DSC_0025.jpg`, `DSC_0100.jpg` — finding 3 above) →
  **Mitigation**: accepted by design, not patched — "More than one candidate can share a label" above. The user
  picks the correct one on the review screen, same as an already-established multi-candidate odometer/trip scan.
- **[Risk] Two of the nine gathered photos (`DSC_0477.jpg`, `IMAG0305.jpg`) will not classify a fuel-amount
  candidate at all, because the recognizer misreads their specific volume label badly enough that it matches
  nothing in `VOLUME_LABELS`** (`UITROV`, `LITE` — see the evaluation table) → **Mitigation**: accepted, the same
  kind of gap `add-seven-segment-ocr` accepted (5 of 6 LCD photos, not 6) rather than chasing every OCR artifact;
  the stored photo and detections (once `fix-refueling-scan-capture` lands) are what a future misdetection like
  this is diagnosed from. Not worth fuzzy-matching single-letter OCR confusions (`L`→`U`) into the label rule for
  two photos out of nine.
- **[Risk] Two more of the nine (`DSC_0105.jpg`, `DSC_0426.jpg`) classify nothing because the recognizer never
  finds the reading's own digits at all**, a different gap from label misreading → **Mitigation**: accepted, the
  same "digit-level OCR noise is out of scope" trade-off already listed below, just total absence rather than a
  wrong value; nothing about label matching would fix a number the recognizer never produced in the first place.
  Net across all nine gathered photos: 2 classify cleanly (`DSC_0896.jpg`, `IMAG0297.jpg`), 2 classify correctly
  alongside extra candidates the user filters out by value (`DSC_0025.jpg`, `DSC_0100.jpg`), and 5 classify nothing
  (`DSC_0105.jpg`, `DSC_0426.jpg`: digits never found; `DSC_0477.jpg`, `IMAG0305.jpg`: label misread; `IMAG0303.jpg`:
  no volume label in frame at all) — see `PpOcrRealPhotosJvmTest` for the exact, asserted breakdown.
- **[Risk] The English/US portion of `VOLUME_LABELS` (`L`, `GAL`, `GALLONS`, ...) is still an untested guess** —
  none of the nine real photos available happened to be an English-labeled display → **Mitigation**: kept as a
  reasonable hypothesis rather than dropped outright (an English pump almost certainly does say "GAL" or similar),
  but explicitly flagged here as the one part of the label set this evaluation could not check; correcting it is a
  one-line follow-up once a real English-labeled photo is available, the same way this evaluation corrected the
  rest.
- **[Trade-off] A fuel-amount scan's photo/detections are not persisted** → this was true when this design was
  first drafted; `fix-refueling-scan-capture` (applied and archived separately) now wires `addRefueling`'s capture
  parameter through, so once that change is in place a fuel-amount scan's capture is stored like any other. No
  longer a limitation of this change by the time it ships after that one.
- **[Trade-off] Digit-level OCR noise is out of scope here** (e.g. `DSC_0025.jpg`'s volume reads as `2.12` instead
  of `12.12`, `IMAG0305.jpg`'s as `9.351` instead of `9.35`) → accepted; this is the same recognizer accuracy
  characteristic every existing reading (odometer, trip) already has, not something specific to fuel amounts, and
  the review screen's "pick which candidate to accept" step is the app's existing, only correction mechanism for
  it.
