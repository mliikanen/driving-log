# Proposal

## Why

A refueling is stored today as one row that holds two facts: the fuel that went in and, optionally, the mileage
(a trip distance added, or an odometer count set). Every consumer of the log has to know that a `REFUELING` row can
also be a distance or an odometer reading: `knownOdometerAt` special-cases it, `RefuelingMileage` duplicates the
`DistanceEntry`/`OdometerAnchor` shapes, the row and details formatting branch on it, and every future kind of
input that also records mileage (trips, maintenance) would have to repeat the same trick. The project's data model
is already additive (one record per fact, aggregates derived); this change makes it atomic as well, before more
kinds of event are added on top of the compound shape.

The general rule this change adopts: **the log stores simple, atomic events, one fact each, and the UI combines
them where a screen needs to show what was entered together.** One save of the log event form is one *entry*,
which may produce more than one event.

## What Changes

- **One fact per event.** A refueling event holds only the fill: amount, unit, fuel type and "filled up." A
  refueling logged with mileage is saved as two events of one entry: the "Refueling" event and a "Distance" event
  (trip distance, or a new odometer count over a known odometer) or an "Odometer reading" event (a new odometer
  count where none is known, or a confirmed lower one), exactly the event a "Distance" entry with the same mileage
  would have saved. A refueling no longer sets or advances the odometer itself; its mileage event does.
- **The atomic events after this change** are three kinds, each one fact: Odometer reading (the odometer read a
  count at a moment), Distance (distance added; the typed odometer count it was computed from is provenance of the
  distance, not a second fact) and Refueling (fuel added). **The initial odometer and a later odometer reading are
  merged into one kind**: they record the same fact, and everything that told them apart (exactly one per vehicle,
  saved with the vehicle, no note, photos or Edit, the "Initial odometer" label) is about where the entry came
  from. So an entry records its source (the vehicle being added, or the log event form), and the reading saved
  when the vehicle was added is still labelled and treated as the "Initial odometer," exactly as today. Nothing else that is stored today is an event: a note and photos annotate an entry, a scan capture is the
  provenance of one event's number, and the remembered vehicle, fuel unit, fuel type and tenths choice are
  preferences outside the log.
- **Events of one entry share its note and photos.** The note and photos are stored once, on the entry, never
  copied onto each event. Every row of the entry shows the note and photo icons, and its details screen shows
  them; editing them from either row edits the one shared note and set of photos.
- **A scan belongs to the event whose number it filled**: a fuel-amount scan to the "Refueling" event, a mileage
  scan to the mileage event. Still at most one scan per entry (the latest accepted), as today.
- **The log lists one row per event.** A refueling logged with mileage shows two rows with the same date and time:
  "Refueling 42.3 L" and "Distance +30 km" (or "Odometer reading 45,300 km"). Tapping either opens the same details
  screen, which combines the entry: fuel amount, fuel type, fill-up state and the mileage line, as it does today.
- **Deterministic order for events at the same time.** Events are ordered by their date and time; events at the
  same time by the entry they belong to, in the order the entries were added (as today); and the events of one
  entry in a fixed order by kind: Odometer reading, Distance, Refueling (oldest first; newest-first
  lists show the reverse, so a refueling's row sits above its own mileage row). The order by kind deliberately
  applies only within one entry; see design.md for why a global order by kind would change how the odometer is
  derived.
- **Existing data is migrated on update, not discarded.** Every existing event becomes its own entry (keeping its
  note and photos; an initial odometer's entry records that it came from adding the vehicle, and both odometer
  kinds become odometer readings), and every existing refueling that carries mileage is split into a "Refueling" event and the
  mileage event it implied, in the same entry, at the same moment. Current odometers, notes, photos and scans are
  unchanged by the migration.

Out of scope: new kinds of event (maintenance, other liquids, standalone notes, trips), keeping one scan per
target instead of one per entry, any change to the log event form's fields or validation, and editing an event's
facts (still only an entry's note and photos can be edited).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `vehicle-log`: the log is defined as atomic events grouped into entries, the initial odometer being the odometer
  reading of the entry saved when the vehicle was added; the current odometer no longer
  special-cases refuelings; same-time ordering by entry and then by kind; note and photos belong to the entry and
  their icons show on every row of it; a row of a multi-event entry opens the entry's details; existing data is
  migrated.
- `refueling-logging`: saving a refueling with mileage stores a "Refueling" event and a separate mileage event of
  the same entry, together or not at all.
- `event-details`: the details screen shows the whole entry, whichever of its rows was tapped; Edit changes the
  entry's shared note and photos.
- `odometer-ocr-capture`: an accepted scan is stored with the event whose number it filled, not with the entry as a
  whole.

## Impact

- **Database** (`shared/src/commonMain/sqldelight`): a new `log_entry` table (the entry, its source and its note);
  `vehicle_event` gains `entry_id`, loses `note`, and its `INITIAL_ODOMETER`/`ODOMETER_ANCHOR` types become one
  `ODOMETER_READING` and the refueling's use of the mileage columns; `event_picture` references the
  entry instead of the event. Migration `12.sqm` (schema 12 → 13) rebuilds the tables (Android 13's SQLite has no
  `DROP COLUMN`) and splits existing refuelings. Every Maestro fixture goes stale and is regenerated
  (`docs/test-fixtures.md`); `VehicleMigrationJvmTest` gains the version-12 schema and the split.
- **Domain** (`vehicle/domain`): `VehicleEvent` gains `entryId`; `InitialOdometer` and `OdometerAnchor` become one
  `OdometerReading` case, and "initial" is read from the entry's source; `Refueling` loses `mileage`; `RefuelingMileage`
  stays only as the input to `addRefueling`; `knownOdometerAt` loses its refueling branch; a `LogEntry` read model
  (events + note + photos) for the details screen.
- **Repository**: `addRefueling` writes up to two events and one entry in one transaction; `updateEventNote`,
  `addEventPhoto` and `removeEventPhoto` act on the entry of the given event; a new `observeEntry`.
- **UI**: `EventRowContent` (refueling rows lose nothing, a mileage row is an ordinary Distance/Odometer reading
  row, an odometer reading's label and Edit action follow its entry's source), `EventDetailsProcessor`/`EventDetailsScreen` read the entry.
- **Maestro**: the `distance` manifest's refueling steps in `log-distance.yaml` assert the second row.
- **In-flight changes**: this settles the open question of `generalize-vehicle-event-model` in the opposite
  direction (atomic events grouped by entry, rather than a container event with facets); that stub should be
  closed or rewritten once this is accepted. `sweep-event-pictures` is unaffected in substance (its query reads
  `event_picture.id`). `add-trip-logging` already proposes a trip as a record of its own (a span with a start and an
  end), not as a container event. Whether membership is derived from the span (an entry dated between start and end
  belongs to it) or stored as a reference is its own open question 2; either way the unit of membership is the
  entry (a reference would go on `log_entry`), so a refueling and its mileage join or leave a trip together, and a
  trip's distance is the sum of its entries' "Distance" events, refueling mileage included without a special case. Its open question 4 (an odometer at start and end) can
  be answered with ordinary entries holding an "Odometer reading" or "Distance" event. It should be applied after
  this change (design.md, "Relation to in-flight changes").
