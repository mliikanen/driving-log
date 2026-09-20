# Proposal

## Why

A vehicle has an odometer and a log, but the only thing the log can hold is the initial reading, so nothing records
driving. The app's purpose is logging mileage, and the first step is the simplest way a driver captures it: reading the
trip meter, or reading the new odometer count. This change adds exactly that: recording distance. Trips (grouping
distances into spans) and refuelings are separate, upcoming changes that build on the same log.

## What Changes

- The vehicle details screen gets a "Log distance" action that opens a form for one distance entry, with two ways to
  enter it: **Trip distance** (the distance itself, from the trip meter) or **New odometer** (the odometer count now; the
  distance is calculated from the previous known odometer).
- The **previous known odometer** at a given time is the latest odometer-setting event (the initial odometer, today)
  at or before that time, plus the distances logged after it up to that time. The form shows it, and the calculated
  distance, while the user types.
- Each entry has a **date and time and a time zone**, chosen with a picker. The moment defaults to when the form was
  opened and the time zone to the device's; the moment may be in the past and may not be in the future. The user can pick
  any time zone, so an entry can be recorded in the zone where it happened. An entry dated before the vehicle's initial odometer is allowed: it is kept in the
  log at its time and does not change any odometer. There is no known odometer at such a time, so the new-odometer way is
  unavailable for it.
- The **unit** of an entry is chosen on the form: kilometers or miles, defaulting to the vehicle's, with an optional
  "include tenths" switch, also defaulting to the vehicle's. Whole-number trip meters and tenths trip meters are both
  possible whatever the vehicle's odometer unit. Entries are typed in the same microwave-style field as the initial
  odometer and shown in the vehicle's unit.
- A saved entry becomes a **Distance** event in the vehicle's log. It shows in the recent events and the full log, newest
  first by event time, with its distance. The vehicle's current odometer is the latest odometer-setting event's reading
  plus every distance entry logged after it, still derived from the log and never stored.
- **Time zones are part of the storage model.** Every log event's moment is stored as the instant plus the IANA time zone
  it was entered in and that zone's UTC offset at the moment, and it is shown in that zone, whatever the device's zone is
  now, until the user chooses otherwise (a way to choose is not part of this change). The zone is shown next to the time
  when it differs from the device's zone. The initial odometer event records the device's zone at the time of adding.
- Storage moves to schema version 2 with a migration for existing databases: distance entries need a distance column,
  entries made by odometer keep the count the user typed for provenance (never used to derive the odometer), and event
  moments gain their zone columns. Events written before this change carry no zone and are shown in the device's zone.

Out of scope (later changes):
- **Trip support, planned as the next change.** A trip is an optional span in the log, defined by a trip start marker and
  a trip end marker, that contains every distance entry and refueling with a time between them, whenever it was added.
  Decisions already taken for it: trips cannot overlap for a vehicle; a trip may have an optional name; a start or end
  marker can be undone with a cancel marker. Nothing in this change implements or prepares any of it beyond keeping the
  log append-only: a distance entry has no reference to a trip, because membership will be derived from event times.
- A setting or control to show times in another zone than the one they were entered in, editing a stored entry's zone,
  refuelings, editing or deleting a distance entry, other ways to set the odometer, and reading distances by camera or OCR.

## Capabilities

### New Capabilities
- `distance-logging`: logging a distance entry from the vehicle details screen, by trip distance or by new odometer,
  with its unit, date and time, validation, and the known odometer it is calculated from.

### Modified Capabilities
- `vehicle-log`: the log gains the distance event; the current odometer becomes the latest odometer-setting reading plus
  the distances after it; the recent events and full log show distance entries and order by event time; the log is only
  ever added to.
- `vehicles`: the details screen gains the "Log distance" action.

## Impact

- Code: a new `distance` feature (form contract, processor, screen, nav key, validation), the date, time and time zone picker,
  a `ZonedMoment` (instant, zone id, offset) used by every event,
  a `Distance` event type and a pure known-odometer function in the vehicle domain, repository additions, the details and
  full log rows, and the nav key registration.
- Data: schema version 2 (migration `1.sqm`: four nullable columns on `vehicle_event`: the distance, the typed count, the
  time zone id and the UTC offset), and the derived current-odometer query. Audit timestamps that users never see or enter
  (`created_at`, `updated_at`) stay plain UTC instants. Existing databases keep working and gain the columns.
- Dependencies: none new. The Material 3 date and time pickers are already in the Compose Multiplatform material3 library, and kotlinx-datetime already
  lists the available time zones.
- Project context: `openspec/config.yaml` gains what a distance entry is, how the current odometer is derived, and the rule
  that every date and time a user enters or sees is stored with its time zone and shown in the zone it was entered in.
- Tests: new unit, migration, repository and processor tests, and Maestro flows for logging by distance, by odometer,
  the validation cases and the date picker.
- Verification: the date and time picker and the migration are new ground for this project; iOS compiles but cannot be
  run on this machine.
