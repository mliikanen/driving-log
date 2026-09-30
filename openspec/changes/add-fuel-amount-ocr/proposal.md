# Proposal (stub)

> **Stub.** Filed as an explicit follow-up of `add-refueling-logging`, which deliberately excludes OCR from its own
> scope (manual entry only). Not ready to build: it needs `add-refueling-logging`'s fuel amount field to exist
> first, and has a real open question — whether the existing odometer scanner's classification approach actually
> transfers — that should be settled by design work, not assumed here.

## Why

`odometer-ocr-capture` already lets a photo fill in an odometer or trip-distance field instead of typing it, using
on-device OCR to find numeric readings and classify them by adjacent label text or magnitude. A fuel pump or
receipt display showing the amount dispensed is the same kind of problem — a numeric reading in a photo the user
would otherwise type by hand — and this was the OCR use case named when `add-refueling-logging` was first proposed
(the fuel-amount case, distinct from the fuel-type case `add-fuel-type-ocr` covers).

## What Changes

Not yet designed in detail. At minimum: a "Scan a reading" style action on the refueling form's amount field,
reusing `odometer-ocr-capture`'s photo/live-scanner pipeline and `add-seven-segment-ocr`'s digit recognition (pump
and receipt displays are printed or seven-segment, the same two kinds already handled), to detect a fuel-amount
candidate and let the user accept it into the amount field.

## Capabilities

### Modified Capabilities
- `refueling-logging`: adds an OCR-assisted way to fill the amount field, mirroring `distance-logging`'s existing
  "Scan a reading" requirement.
- *(possibly `odometer-ocr-capture` or a new capability, depending on how much of its classification logic actually
  reuses cleanly — see the open question below.)*

## Open questions

1. **Does the odometer scanner's two-tier classification (label text, else magnitude against a known value)
   transfer at all?** Odometer readings have a magnitude anchor to fall back on (the vehicle's own known odometer).
   A fuel pump display typically shows *volume* and *total price* as two numbers of similar magnitude and format
   (e.g. `12.345 GAL` next to `$45.67`) with no comparable anchor to tell them apart by size. This likely means
   fuel-amount detection has to lean almost entirely on label text (`L`, `GAL`, `VOLUME` vs `$`, `PRICE`, `TOTAL`),
   a narrower and less forgiving signal than odometer's fallback-capable approach — worth designing for explicitly,
   not assuming it inherits odometer's robustness.
2. **One scan or two?** `add-fuel-type-ocr` would point the camera at the same pump photo. Are these built as one
   combined scan (one photo, two kinds of candidate extracted) or genuinely separate scans, one per field? They are
   filed as separate proposals per the developer's own request, but that doesn't settle whether the underlying scan
   flow is shared.
3. **Does the currently-out-of-scope cost/price field change this?** If `add-refueling-logging` never gains a cost
   field, a pump photo's price number is simply never a candidate for anything — worth confirming price recognition
   is truly not needed here (not even to help rule out the volume candidate by elimination).

## Impact

Not yet assessed — depends on `add-refueling-logging` existing and the open questions above being settled.
