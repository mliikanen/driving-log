# Proposal (stub)

> **Stub.** Filed as an explicit follow-up of `add-refueling-logging`, which deliberately excludes it from its own
> scope. Not ready to build: it needs `add-refueling-logging`'s fixed fuel-type list to exist first (`refueling-logging`
> or wherever that capability lands), and it has a real open question — where a Settings screen is reached from —
> that should be settled with the developer before design work starts, not guessed at here.

## Why

`add-refueling-logging` gives every refueling a fuel type chosen from a fixed list covering several markets (regular
and premium petrol, diesel, premium diesel, biodiesel, E85, LPG, CNG, hydrogen, other). Most users only ever need
two or three of those for the vehicles they actually own, and the list is expected to grow, not shrink, as further
fuel-related proposals land (AdBlue tracking is already filed as its own follow-up, `add-adblue-tracking`). A
Settings screen is the natural place to let a user narrow the fuel-type picker down to what's relevant to them,
without deleting or renaming anything in the underlying fixed list, and gives the app a home for whatever
app-wide preference needs come after this one.

## What Changes

- Adds the application's first Settings screen. No settings screen or navigation entry to one exists today.
- First use case: choosing which fuel types from `add-refueling-logging`'s fixed list are offered in the refueling
  form's fuel-type picker. A type left unchecked is simply hidden from that picker; it is not deleted from the
  fixed list, and any already-logged event that used it keeps showing it normally in its own details.
- Everything else about the fuel-type list (its members, their order, their stored codes) is unchanged; this change
  only adds a per-user visibility filter on top of it.

## Capabilities

### New Capabilities
- `settings`: a Settings screen and, as its first stored preference, the set of fuel types visible in the
  refueling form's picker. Exact path/name to be settled when this is designed for real (the project's existing
  capabilities are flat, not nested, so this likely stays `settings` rather than something like `app/settings`).

### Modified Capabilities
- *(left empty deliberately — `add-refueling-logging`'s capability does not exist as a main spec yet. Once it is
  archived, this change's design will need to add a "the picker only shows visible types" scenario to it or to
  wherever the fuel-type picker's requirement ends up.)*

## Open questions

1. **Where is Settings reached from?** `app-shell`'s Home screen grid is a fixed 2x2 of four actions, and the
   fourth slot ("Placeholder", a question-mark icon) has been sitting unused since `add-landing-screen` — it may
   have been meant for exactly this, or it may not have been. The alternative is a conventional Android/iOS
   pattern (an overflow or gear icon in a top app bar) that doesn't consume that scarce grid slot. This is a real
   product decision, not a detail to assume silently.
2. **Scope of "settings" beyond the first use case.** Is this change's Settings screen meant to be a general,
   extensible container from day one (a list/sectioned screen ready for more entries later), or built narrowly for
   just the fuel-type filter and generalized only when a second use case actually shows up?
3. **Selecting a filtered-out type on an old event.** If a user later hides a fuel type that an existing event
   already used, does editing that event's fuel type still offer it (since it's already the current value), or
   does editing force a change away from a hidden type? Leans toward "still offered for its own event," mirroring
   how a vehicle's own now-hidden state doesn't retroactively change its past log — but this should be confirmed
   against `add-refueling-logging`'s actual edit behavior once that exists.

## Impact

Not yet assessed — depends on `add-refueling-logging` existing and the open questions above being settled.
