# Proposal

## Why

The project has anticipated refueling tracking from the start: the project context describes capturing "refueling
amounts," prompting the user "to include mileage (either odo or trip) at the refueling time" whenever a refueling is
logged, a gallon/liter unit toggle, and an accent color ("Fuel Gauge Gold") already reserved for fuel in the theme.
`distance-logging`'s own "Kind" selector was built with this in mind: it is deliberately shown but disabled today,
its own requirement stating it "becomes usable once a later change adds a second kind." This proposal is that
second kind: users currently have no way to log a refueling at all, so there is no data to base a fuel-consumption
calculation on, which is the concrete motivation named for building this now.

## What Changes

- Adds "Refueling" as the log event form's second `Kind`, alongside "Distance." Opening the form from a vehicle's
  details screen or the Home screen, choosing the vehicle (from Home), the date/time/zone picker, notes and
  photos all work exactly as they already do for a "Distance" entry — only the kind-specific fields differ.
- A refueling entry records: a fuel amount, a fuel type (from a fixed list, see below), a "filled up" checkbox
  (checked by default), and, optionally, a mileage reading (the same "Trip distance" or "New odometer" choice a
  "Distance" entry already offers) — offered on the form but not required to save. A refueling with mileage entered
  advances or sets the vehicle's odometer exactly like a "Distance" entry or an odometer anchor already does; one
  saved without mileage does not touch the odometer at all.
- Fuel amount unit (liters or gallons) and fuel type each remember the **last value the user chose**, independent
  of which vehicle is being logged and never derived from an earlier event — a global, per-user preference, not a
  per-vehicle one (`vehicles`' existing per-vehicle unit override is for odometer/trip units only; this is
  intentionally different).
- The fuel type list: Regular petrol/gasoline, Premium petrol/gasoline, Diesel, Premium diesel, Biodiesel, E85/flex
  fuel, LPG, CNG, Hydrogen, Other. AdBlue/diesel exhaust fluid is deliberately excluded (it is not a propulsion
  fuel; a separate follow-up, `add-adblue-tracking`, is filed for it).
- A refueling's row shows a "Refueling" label and its fuel amount as the row's figure (e.g. "42.3 L"), the same way
  a "Distance" row shows "+30 km." The existing note/photo presence-icon cluster on a row extends to cover a
  refueling event, the same as it already does for "Distance" and "Odometer reading."
- The details screen shows a refueling's fuel amount, fuel type, "filled up" state, and its mileage reading (if
  any) alongside the note/photo sections it already shows for other kinds. The existing "Edit" action's scope is
  **not** extended: it continues to let the user change only the note and the photos after saving, exactly as it
  does today for "Distance" and "Odometer reading" events. A refueling's fuel amount, fuel type, "filled up" flag,
  and its mileage (or the absence of one) are fixed once saved — the same way a "Distance" entry's own distance is
  already fixed once saved, never recomputed if an earlier event is later inserted into the log.
- The existing lower-odometer confirmation (`confirm-lower-odometer`) and the odometer scanner's "Scan a reading"
  action (`odometer-ocr-capture`) both extend to a refueling's optional mileage field, since it is the same
  underlying field and Way choice a "Distance" entry already uses.

**Explicitly out of scope, filed as separate follow-ups:**
- Cost/price tracking (no unit price or total price field).
- Computing or showing fuel consumption. This change stores the "filled up" flag and the fuel amount; nothing
  derives or displays a consumption figure from them. (Whenever that follow-up is designed, it should compute
  "distance since the last fill-up" on demand from the log, the same way `currentOdometer`/`knownOdometerAt`
  already are — never as a stored or incrementally-maintained aggregate, since a backdated insert or edit anywhere
  in the log can change it.)
- OCR-assisted entry of the fuel amount (`add-fuel-amount-ocr`) or the fuel type (`add-fuel-type-ocr`) from a photo.
- AdBlue/diesel exhaust fluid tracking (`add-adblue-tracking`).
- Letting the user hide fuel types they never use from the picker (`add-settings-screen`'s first use case).
- Any change to how `VehicleEvent` itself is modeled (`generalize-vehicle-event-model` is a separate, low-priority
  idea explored alongside this proposal; nothing here depends on it or conflicts with it).

## Capabilities

### New Capabilities
- `refueling-logging`: the refueling event's data (fuel amount, fuel type, "filled up" flag, optional mileage), the
  log event form's "Refueling" kind, and the global last-used fuel unit/fuel type memory.

### Modified Capabilities
- `distance-logging`: "The kind of event is chosen" — the Kind selector becomes an enabled, two-item choice
  ("Distance", "Refueling") instead of a disabled, single-item one. "A trip distance must be entered and above
  zero" is scoped explicitly to the "Distance" kind's own field, since a refueling's mileage does not gate Save the
  way a "Distance" entry's field does (`refueling-logging` states the fuel amount's own, separate save-gating rule).
  Everything else already-generic in this capability (the Way choice itself, the previous-known-odometer
  calculation, both lower-odometer-confirmation requirements, the odometer scanner's "Scan a reading" action) is
  written in terms of "the entry"/"the log event form" without assuming a "Distance" kind, and already covers a
  refueling's optional mileage once it exists, with no wording change needed.
- `vehicle-log`: "Current odometer is derived from the log" — a refueling's mileage (when present) counts toward
  the running total the same way a distance entry's does. "Recent events on the details screen" gains a refueling
  row's display rule (fuel amount as its figure, a "Refueling" label). A new requirement states that a refueling
  event can carry a note and photos, mirroring the existing "Distance and odometer-anchor events can carry a
  note"/"...can carry photos" requirements. The existing note/photo presence-icon requirement is already generic
  ("any event that has a non-empty note, one or more photos, or both") and needs no change.
- `event-details`: a new requirement shows a refueling's fuel amount, fuel type, "filled up" state and mileage (if
  any); the existing "Edit" requirement is modified to list "Refueling" among the kinds it is offered for, and to
  state explicitly that its scope (note and photos only) is unchanged — a refueling's fuel fields and mileage stay
  fixed once saved, the same way a "Distance" entry's own distance already is.

## Impact

- `shared/src/commonMain/kotlin/.../vehicle/domain/VehicleEvent.kt`: a new `Refueling` case (amount, fuel type,
  filled-up flag, optional mileage, note, photoIds).
- `shared/src/commonMain/sqldelight/.../db/VehicleEvent.sq` and a new migration: refueling's fields are stored as
  columns on the existing `vehicle_event` table (not a separate table), so the existing `observeLog`/`observeEvent`
  reactive queries (and everything built on them: `currentOdometer`, `knownOdometerAt`, a future consumption
  calculation) see a refueling's writes for free, the same way they already see a "Distance" entry's — avoiding the
  cross-table reactivity gap `add-event-pictures` had to work around for `event_picture` with a manual "touch"
  write.
- `shared/src/commonMain/sqldelight/.../db/AppState.sq`: two new keys in the existing key-value table
  (`last_fuel_unit`, `last_fuel_type`), alongside the existing `last_logged_vehicle_id` — no new table needed.
- `shared/.../vehicle/domain/VehicleRepository.kt` and `SqlDelightVehicleRepository.kt`: a new `addRefueling(...)`
  method and the two new preference reads/writes.
- `shared/.../vehicle/distance/LogEventContract.kt` / `LogEventProcessor.kt` / `LogEventScreen.kt`: the Kind
  selector becomes interactive; refueling-specific fields (amount, fuel type, filled-up) on the form.
- `shared/.../vehicle/format/EventRowContent.kt`, `.../vehicle/ui/EventRow.kt`: a refueling's row label and figure.
- `shared/.../vehicle/eventdetails/EventDetailsContract.kt` / `EventDetailsProcessor.kt` / `EventDetailsScreen.kt`:
  showing a refueling's fields on the details screen.
- Maestro: fixtures (`docs/test-fixtures.md`) need regenerating once the schema changes; a new or extended
  `distance` (or dedicated `refueling`) manifest case for the happy path.
