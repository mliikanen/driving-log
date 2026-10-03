# Proposal (stub)

> **Stub.** Filed as an explicit follow-up of `add-refueling-logging`, which deliberately excludes AdBlue (diesel
> exhaust fluid, DEF) from its fuel-type list. Not ready to build: it needs `add-refueling-logging` to exist first,
> and has real open questions about whether AdBlue fits the same event shape as a fuel refueling at all.

## Why

Many diesel vehicles with SCR aftertreatment need AdBlue (diesel exhaust fluid) topped up every few thousand
kilometers, refilled at the pump or from a bottle, separately from the diesel tank itself. It is not a propulsion
fuel and does not belong in `add-refueling-logging`'s fuel-type list (excluded from that change on purpose), but
drivers who need it would plausibly want it in the same log. This is filed now, while the exclusion is fresh, so
the intent is not lost.

## What Changes

Not yet designed. At minimum, some way to record an AdBlue top-up: an amount (liters, no gallons/miles-family
ambiguity the way fuel has — AdBlue is sold and dosed in liters everywhere, including in the US), and a date/time.
Whether it also wants a "topped up to full" flag the way `add-refueling-logging`'s fuel amount does, whether it
needs a mileage reading at all (a driver could plausibly want to correlate AdBlue consumption with distance driven,
the same motivation `add-refueling-logging` has for fuel), and how it is entered are all open.

## Capabilities

### New Capabilities
- *(not yet named — depends on the open questions below)*

## Open questions

1. **Is this the same "Kind" on the log event form, a variant of the refueling kind, or a wholly separate event
   kind?** AdBlue top-ups are typically far less frequent than fuel refuelings and are never mixed with them in the
   same pump transaction, which argues for keeping it distinct rather than folding it into `add-refueling-logging`'s
   Refueling kind (e.g. as another "fuel type"). But duplicating that kind's whole shape (mileage, filled-up-style
   flag, notes, photos) for a single liters field may be more machinery than the feature needs.
2. **Does it need a mileage reading at all?** If the point is only "remember when I last topped up AdBlue," a
   plain date/time may be enough; if the point is consumption tracking (litres of AdBlue per 1000 km, a real thing
   diesel owners track), it needs the same optional mileage mechanic `add-refueling-logging` has for fuel.
3. **Should this wait on `add-settings-screen`?** If AdBlue becomes its own "Kind," it may want the same kind of
   user-level visibility control (a driver without an SCR diesel never wants to see it) that `add-settings-screen`
   is being built for fuel types — worth designing together rather than bolting on twice.

## Impact

Not yet assessed — depends on the open questions above being settled with the developer.
