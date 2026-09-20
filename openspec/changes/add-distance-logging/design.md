# Design

## Context

See proposal.md for motivation. Current state, from the archived `add-vehicles` change:

- The log is `vehicle_event` (`id`, `vehicle_id`, `type`, `occurred_at`, `odometer_meters`, `created_at`). The only type is
  `INITIAL_ODOMETER`, whose `odometer_meters` is the reading it sets. `VehicleEvent` is a sealed interface with one member,
  and every event is ordered `occurred_at DESC, rowid DESC`.
- The current odometer is derived in SQL (`selectVehicleDetails`): the `odometer_meters` of the latest event that has one.
- The schema is version 1 with no migrations. Local databases exist on test devices, so a schema change needs a migration.
- Every event moment is a single `occurred_at` epoch-millisecond instant (plus `created_at`), with no time zone; the initial
  odometer event's moment is the time of adding. Dates are drawn in the device's zone with a fixed `yyyy-MM-dd HH:mm` format.
- Odometer entry is `OdometerEntry` plus `OdometerField` (empty start, microwave fill, kept leading zero, locale
  separators), and by project convention every odometer input uses them. `OdometerUnit` has the four units.
- Screens are Kide processors with nav keys that carry the graph. A key must be equal by value (its `serialKey` and
  arguments) or a rotation builds a new processor and loses typed state, and registration of the keys must be idempotent.
- Vehicle details and the full log draw events through `event.odometer`, which today is always set.

## Goals / Non-Goals

**Goals:**
- Log a distance by trip distance or by new odometer count, at a chosen time and time zone, with a selectable unit.
- Store every event moment with the time zone it was entered in, and show it in that zone.
- Keep the log append-only and the current odometer derived, never stored.
- Migrate existing databases without losing data.

**Non-Goals:**
- Trips as spans, refuelings, editing or deleting entries, camera input. The trip change comes next; nothing here depends on it.

## Decisions

### 1. A `DISTANCE` event and schema version 2

`vehicle_event` gains four nullable columns, added by migration `1.sqm` (version 1 to 2):

```sql
ALTER TABLE vehicle_event ADD COLUMN distance_meters INTEGER;         -- set for DISTANCE events
ALTER TABLE vehicle_event ADD COLUMN logged_odometer_meters INTEGER;  -- DISTANCE by odometer: the count the user typed
ALTER TABLE vehicle_event ADD COLUMN occurred_zone TEXT;              -- IANA zone id the moment was entered in, e.g. Europe/Helsinki
ALTER TABLE vehicle_event ADD COLUMN occurred_offset_seconds INTEGER; -- that zone's UTC offset at the moment
```

`type = 'DISTANCE'` events have `distance_meters` (whole meters, above zero) and a null `odometer_meters`.
`odometer_meters` keeps its meaning: it is set only by odometer-setting events. `logged_odometer_meters` is provenance for
the log row (and later for camera input) and is never read when deriving an odometer. Reusing `odometer_meters` for it
was rejected: it would turn every distance entry made by odometer into an odometer-setting event and change the derivation.

In Kotlin, `VehicleEvent.DistanceEntry(id, occurredAt, distance, loggedOdometer)` joins `InitialOdometer`. `VehicleEvent.odometer`
becomes "the reading this event sets" (null for a distance entry); rows are drawn by event type, no longer by `odometer`.
Unknown types are already skipped when reading. Everything stored stays locale-agnostic (meters, epoch milliseconds, codes).

### 2. Ordering and the known odometer

Events are ordered by `occurred_at`, then insertion order (`rowid`), as today. The **known odometer at a time T** is a pure
function over the log (`knownOdometerAt(eventsOldestFirst, T)`, in the vehicle domain): find the latest odometer-setting event
with `occurredAt <= T` (ties: later in the list), and add the `distance` of every `DistanceEntry` after it with
`occurredAt <= T`. No odometer-setting event at or before T gives `null`. The form uses it live.

The **current odometer** is the same rule with T = the end of the log: the latest odometer-setting event plus every distance
entry after it. It stays in SQL for the details query, using the row-value comparison `(occurred_at, rowid) > (base.occurred_at, base.rowid)`
on `DISTANCE` events. A test asserts the SQL and the pure function agree on prepared logs, including entries before the
initial event and same-time ties. An entry logged now is inserted last, so among events with equal time it counts after
all of them, which is what the definition says.

Entries dated before the latest odometer-setting event are ordered before it, so they are outside the sum and change nothing.

### 3. The distance is calculated once, at entry

By odometer, `distance = entered - knownOdometerAt(T)` is computed when the user saves and stored as `distance_meters` next to the typed count.
It is not recomputed later. That matches the model in the request (a known odometer plus the distances after it) and keeps events
immutable: a distance entry added later at an earlier time changes the totals by its own distance, as it should, and does not
rewrite another entry. The price is that a typed count in an older entry can differ from the derived odometer at its time once
entries are inserted before it. That is accepted: the count is what the user typed, the distance is what was logged.

### 4. The form: two ways, one unit

State (persisted for rotation and process death): the way (`TRIP_DISTANCE` or `NEW_ODOMETER`), the unit, one `OdometerEntry` per way,
`occurredAtMillis`, and the current error. Kept out of the saved state (`@Transient`, rebuilt from the repository): the log events
and the vehicle's unit. The known odometer and the live distance are computed from those on demand, not stored. Kide's `restoreState` replaces the whole state, which would wipe the repository-derived fields if it ran after they loaded. In the app it does not:
the host restores right after the processor is created and the repository data arrives asynchronously afterwards, and a processor test that defers the collection
covers that order.

- **One unit for both fields.** The user chooses kilometers or miles (segmented control) and whether tenths are included (switch); the four
  `OdometerUnit` values are the combinations, and the defaults are the vehicle's unit split into those two choices. Changing either
  applies `OdometerEntry.withUnit` to both entries, so digits are kept exactly as when adding a vehicle. The choice is not stored on the
  entry; only meters are.
- **Conversion.** `distance = unit.stepsToMeters(steps)` (integer arithmetic, as for the initial odometer). Display is always in the vehicle's unit.
  A mile entry for a kilometer vehicle is rounded to the nearest meter, then shown rounded to the vehicle's step.
- **Fields.** Both use `OdometerField` (label "Trip distance" or "New odometer"), so the number keyboard, right-edge fill, kept leading zero, locale
  separators and empty start are the shared ones. That satisfies the convention that every odometer input uses the shared pieces; the trip distance is
  the same kind of number.

### 5. Time zones: storage, entry and rendering

**Storage model.** A moment that a user enters or sees is a `ZonedMoment(instant, zoneId, offsetSeconds)` in the domain and three columns
in storage: `occurred_at` (epoch milliseconds, the instant, which is what all ordering and derivation use), `occurred_zone` (the IANA id) and
`occurred_offset_seconds` (that zone's UTC offset at the instant). The zone id says where it was entered and lets the app offer that zone again; the
offset makes the shown wall-clock time exact and identical on every platform and after a time zone database update, so a stored time never
drifts. Both are plain text and integers, independent of the device locale. Every event carries them: distance entries take the zone chosen on the
form, and the initial odometer event takes the device's zone at the time of adding (`addVehicle` reads the injected `Clock` and a small
`DeviceTimeZone` provider, which tests fake, like `DeviceLocale`). Audit timestamps that users never see or enter (`created_at`, `updated_at`) stay plain
UTC instants. Events written before this change have null zone columns; they are shown in the device's current zone with no zone name.

**Rendering.** A moment is drawn as the instant shifted by its stored offset, formatted `yyyy-MM-dd HH:mm`, so it reads as it did when it was
entered even if the device is now in another zone. When the stored zone id differs from the device's current zone id, the zone id is appended
in parentheses ("2026-09-20 08:30 (America/New_York)"). No rendering path converts an event to the device zone. A later change can add a way to show
times in another zone ("until otherwise mentioned"); the model already has everything it needs.

**Entry.** The form holds a wall-clock `LocalDateTime` and a zone id, and derives the instant from them (`toInstant(zone)`). The default is the moment
the form was opened, expressed in the device's zone, and it is part of the saved state so it survives rotation and process death. Changing the zone keeps
the wall-clock date and time and changes which instant they mean: the user entering "18:30 in New York" wants 18:30 there, not a conversion. A wall-clock
time that does not exist (a daylight saving gap) is resolved forward and one that occurs twice (an overlap) takes the earlier offset, as kotlinx-datetime does;
the stored offset records what was chosen.

**Pickers.** Material 3 `DatePicker` in a `DatePickerDialog`, and `TimePicker` in an `AlertDialog`, both in common code. The date picker reports midnight UTC of the
chosen day, read as a `LocalDate` in UTC and combined with the picked hour and minute. The time zone is chosen from a dialog listing the
zones from kotlinx-datetime's `TimeZone.availableZoneIds` (common code; on Android it is the runtime's `java.time` list, 604 ids on the JDK used here, and on iOS
the Apple implementation, not checkable on this machine). That raw list mixes 488 `Continent/City` names with about 116 legacy aliases and abbreviations
(`Brazil/East`, `CET`, `CST6CDT`, `Canada/Eastern`, `Etc/...`), so the dialog shows only ids of the form `Continent/City` plus `UTC`, plus the device's own zone id
whatever it looks like. A search field filters by case-insensitive substring of the id; each row shows the id and its current UTC offset. The device zone is first
when unfiltered. Any stored id still renders, because rendering uses the stored offset.

### 6. Validation

At save, in this order, the first failing rule sets the error and nothing is saved:

1. the active field is empty (`Enter the trip distance` or `Enter the odometer reading`);
2. the chosen moment, as an instant in its own zone, is later than `clock.now()` (`The time cannot be in the future`); wall-clock times are never compared across zones;
3. trip distance: the value is zero (`The distance must be more than zero`);
4. new odometer: no known odometer at that time (`No odometer is known at this time. Enter the trip distance instead.`), or the count is not higher
   than the known odometer (`Enter a reading higher than <known odometer>`, formatted in the vehicle's unit).

While the way is "New odometer" the form already shows the known odometer, the live distance, and the no-known-odometer message
(the save button stays enabled and the same message is the error, so the rule is one place). Errors clear when the user types, changes the
time or switches the way.

### 7. Repository, processor, navigation

- `VehicleRepository.addDistanceEntry(vehicleId, occurredAt: ZonedMoment, distance, loggedOdometer)` inserts one `DISTANCE` event in a single statement
  (it requires `distance > 0`). Ids and the clock stay injected.
- `LogDistanceProcessor` is created with Metro assisted injection (`vehicleId`), like the details and edit processors. It observes the vehicle
  and its log, and never re-implements an entry rule: it forwards field text to `OdometerEntry.applyEdit`.
- `LogDistanceNavKey(graph, vehicleId)` with value-based `equals`, `hashCode` and `toString`, `saveArgs` = the vehicle id, a serializer for the
  form state, and registration in `registerVehicleNavKeys`. The details processor gets a `ShowLogDistance` effect, and its screen a "Log distance" button.

### 8. Rows in the log

A `Distance` row shows the label "Distance", the date and time in the zone it was entered in (with the zone id when it is not the device's zone), and the distance as "+30 km" in the vehicle's unit and locale; when
`logged_odometer_meters` is set it adds the count the user typed ("Odometer 45,250 km"). An `Initial odometer` row is unchanged. The
recent events and the full log use the same row composable.

### 9. Migration and its test

`1.sqm` holds the four `ALTER TABLE` statements; the generated schema version becomes 2 and the Android and native drivers run `migrate` on
upgrade. A JVM test builds a version-1 database from the version-1 DDL kept as test data, inserts a vehicle and its initial event, runs
`Schema.migrate(driver, 1, 2)`, and checks the data is intact, the new columns are null (a legacy event reads back with no zone and is drawn in the device zone), and a distance entry with a zone can then be added. A fresh
database is created at version 2 with the same tables.

### 10. Testing

- Time zones (pure): wall-clock plus zone to a `ZonedMoment` and back, a daylight saving gap and overlap, the same wall-clock time in two zones giving different instants,
  the future check across zones (16:30 in Helsinki against 10:00 in New York), rendering with the stored offset and the zone id shown only when it differs from the device zone,
  and rendering unchanged after the device zone changes; the zone search filter.
- Pure: `knownOdometerAt` (no baseline, before the baseline, ties, several entries, times between entries), validation, conversion of every unit
  combination (kilometers or miles, with or without tenths) to meters.
- Repository on real SQL: the migration, adding and reading a distance entry, the derived current odometer (SQL versus the pure function), entries
  before the initial event ignored, ordering by time then insertion, the log reversed for the known odometer.
- Processor with a fake repository: defaults per vehicle unit, way switching keeping each number, unit changes keeping digits, every validation error,
  the live known odometer and distance following the time, the default time being the open time in the device zone, changing the zone keeping the wall-clock time, saving results with the zone.
- Maestro on the emulator (English (United States)): log by distance, log by odometer, the error cases, tenths and another unit, and the date picker
  (typed date in the picker's text mode, the date computed in the flow), including a date before the initial odometer, and choosing another time zone from the searchable list and seeing it in the log row.
  The emulator's own time zone is changed with `adb` for a manual check that stored zones keep rendering.

## Risks / Trade-offs

- [A schema migration on existing databases] → Tested from a real version-1 database, and the columns are nullable so existing rows stay valid.
- [The Material 3 date and time pickers are new ground here, on Android and iOS, and hard to drive from Maestro] → Keep the picker a thin wrapper,
  test the date and time arithmetic as pure functions, drive the picker in Maestro through its text input mode with a date the flow computes,
  and fall back to processor-level tests if a picker cannot be driven reliably.
- [Time zone and daylight saving edge cases when combining a picked day and time] → One conversion function with tests around a daylight saving change;
  stored values are epoch milliseconds either way.
- [The zone list is long, platforms can differ in which zone ids they know, and the raw list has legacy aliases] → Filter to `Continent/City` plus UTC and the device zone, searchable; a stored id that a device does not know still renders, because rendering uses the stored offset, not the id.
- [Storing both an id and an offset could disagree after a time zone rules update] → They are both facts about the moment when it was entered; rendering uses only the offset, so nothing changes silently.
- [Events written before this change have no zone] → Shown in the device's zone with no zone name; only test builds have such data.
- [A typed count in an older entry can differ from the odometer derived at its time after earlier entries are added] → Accepted, documented in decision 3.
- [Entries by odometer are rounded to whole meters when the unit is miles] → At most one meter per entry, far below what any unit shows.
- [The derived current odometer could exceed the 7 whole digits the entry field accepts] → Display and derivation handle larger values; only typing is capped.
