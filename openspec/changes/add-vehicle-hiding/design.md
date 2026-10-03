# Design

## Context

See proposal.md for why. Today every vehicle is always shown: `VehicleRepository.observeVehicles()` (the `selectVehicles`
query, every row of `vehicle`) feeds four places, which need different answers once vehicles can be hidden:

| Reader | Needs |
|---|---|
| `VehicleListProcessor` (the list) | visible vehicles, and whether any is hidden |
| `LogEventProcessor` (the Home screen's selector) | visible vehicles |
| `LandingProcessor` (the Home screen's tiles, one `hasVehicles` for both) | any vehicle at all (first tile); a visible one ("Log event") |
| `sweepPictures` (start-up clean-up of picture files no vehicle refers to) | **every** vehicle: a hidden vehicle's picture must never be swept |

The vehicle details screen's top app bar has one action (edit); the screens are `ScreenNavKey`s registered in
`VehicleNavKeys.kt`.

## Goals / Non-Goals

**Goals:**
- Hide and restore without touching any of a vehicle's records.
- Every existing reader keeps working, each seeing the vehicles it should.

**Non-Goals:**
- Deletion (proposal.md, Why).
- Hiding from the list itself (a swipe or long-press).

## Decisions

### Hidden is a timestamp on the vehicle row
A migration adds `hidden_at INTEGER` (null: shown; the time it was hidden otherwise) to `vehicle`, with
`hideVehicle(hidden_at, updated_at, id)` and `restoreVehicle(updated_at, id)`. A timestamp rather than a flag costs nothing
and tells, later, when; nothing else about the vehicle or its events changes. Every existing row migrates as shown (null).
The migration makes the checked-in Maestro fixtures stale: they are regenerated (`docs/test-fixtures.md`).

Alternatives: a separate `hidden_vehicle` table (a join for every read, for no gain); moving hidden vehicles' rows (the
opposite of "nothing about it changes").

### Visible and hidden are read apart; "all vehicles" only where it is needed
`observeVehicles()` keeps its name and becomes visible vehicles only (`WHERE hidden_at IS NULL`), so the list and the
selector need no change of their own. New: `observeHiddenVehicles()` (the restore screen, and the list's entry) and
`observeHasAnyVehicle()` (the Home screen's first tile). The picture sweep reads a new `allPictureIds()` over every row,
hidden or not, instead of `observeVehicles()`, which would now miss hidden vehicles' pictures and delete them; a test pins
that. `observeVehicle(id)` (the details screen, the log, events) is unchanged: a hidden vehicle's records are still
readable by id, which restoring relies on.

### The Home screen tracks two things
`LandingState.hasVehicles` splits into `hasAnyVehicle` (the first tile: list, or add when there is none at all) and
`hasVisibleVehicle` ("Log event"). The selector's existing fallback (the remembered vehicle is not among the vehicles →
the first one) covers a hidden remembered vehicle without a change, since `observeVehicles()` no longer lists it; a test
pins it.

### Hide from the details screen's overflow menu, with an `AlertDialog`
The details screen's top app bar gains an overflow menu (a "More options" icon button and a Material `DropdownMenu`) with
"Hide vehicle", next to the edit action. It opens an `AlertDialog`: title "Hide <name>?", text "It will no longer be listed
or offered when logging. Its log and everything else about it are kept, and you can restore it from Hidden vehicles.",
actions "Hide" and "Cancel". Confirming hides it through the processor, which emits an effect the screen answers by going
back to the vehicle list. The dialog's open state is the processor's (so a rotation keeps it).

### Restore from a hidden vehicles screen
A new `HiddenVehiclesNavKey` / processor / screen lists `observeHiddenVehicles()` with the vehicle list's row (picture,
name, plate, same order) plus a "Restore" text button per row. The list's "Hidden vehicles" entry, after the last vehicle
(a list item with a count, "Hidden vehicles (2)"), opens it; it is shown only while `observeHiddenVehicles()` is not
empty. When the screen's list becomes empty (the last one restored) it goes back.

## Risks / Trade-offs

- **[Risk]** A future reader of `observeVehicles()` that needs every vehicle (a sweep, an export) would silently miss
  hidden ones → **Mitigation**: its KDoc says "visible vehicles", the all-vehicles readers are named for it
  (`allPictureIds`), and the sweep's test fails if hidden pictures are swept.
- **[Trade-off]** A hidden vehicle's details screen, log and events stay reachable by id (a stale back stack after a
  restore of process state) → accepted: they show its data, as restoring would; nothing lists it.
