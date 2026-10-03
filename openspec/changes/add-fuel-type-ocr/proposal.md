# Proposal (stub)

> **Stub.** Filed as an explicit follow-up of `add-refueling-logging`, which deliberately excludes OCR from its own
> scope (manual entry only). Not ready to build: it needs `add-refueling-logging`'s fuel-type list to exist first.
> Named as an "optional stretch-goal" by the developer, kept separate from `add-fuel-amount-ocr` on purpose.

## Why

`add-refueling-logging` gives every refueling a fuel type chosen manually from a fixed list. A fuel pump's nozzle,
handle or display is normally labelled with the fuel type (`DIESEL`, `UNLEADED`, `SUPER`, `E85`, and regional
variants of each), so the same photo a user takes for the amount could also identify the type, without typing or
picking one by hand.

## What Changes

Not yet designed in detail. At minimum: recognize text anywhere in the scanned photo, match it against a keyword
table for `add-refueling-logging`'s fixed fuel-type list (`DIESEL`/`DSL` -> Diesel, `UNLEADED`/`REGULAR` -> Regular
petrol, `SUPER`/`SUPER PLUS`/`PREMIUM` -> Premium petrol, `E85`/`FLEX` -> E85, `LPG`/`AUTOGAS` -> LPG, `CNG` -> CNG,
`H2`/`HYDROGEN` -> Hydrogen, and so on), and let the user accept a detected type into the fuel-type field.

Unlike `add-fuel-amount-ocr`, this is not digit recognition at all — it is keyword matching against recognized
text, closer in kind to the *label-recognition* half of `odometer-ocr-capture`'s existing classifier (which already
matches text like "ODO" or "TRIP" next to a number) than to its digit-reading half. That existing label-matching
building block may transfer here more directly than anything `add-fuel-amount-ocr` can reuse.

## Capabilities

### Modified Capabilities
- `refueling-logging`: adds an OCR-assisted way to fill the fuel-type field.

## Open questions

1. **Keyword table completeness and false positives.** A pump photo can contain a lot of incidental text (ads,
   prices, other nozzles' labels in frame). The keyword table needs real-world photos to validate against, not
   just the fuel-type list's own names.
2. **Regional synonyms.** European vs. US labelling differs meaningfully (e.g. "Super" means premium petrol in much
   of Europe, not a fuel type of its own) — the keyword table needs to be locale-aware in a way `add-refueling-logging`'s
   underlying fixed, locale-agnostic type codes don't otherwise need to be.
3. **One scan or two, shared with `add-fuel-amount-ocr`?** Same open question as filed there — not settled by
   either stub alone.
4. **Interaction with `add-settings-screen`.** If a user has hidden most of the fixed fuel-type list down to the
   two or three types they actually use, should a detected type outside that visible set still be offered, or
   suppressed the same way the picker itself would hide it?

## Impact

Not yet assessed — depends on `add-refueling-logging` existing and the open questions above being settled.
