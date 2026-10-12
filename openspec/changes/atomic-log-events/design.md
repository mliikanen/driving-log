# Design

## Context

See proposal.md for why. The relevant state today (schema version 12):

- `vehicle_event` is one wide table. `type` is `INITIAL_ODOMETER`, `ODOMETER_ANCHOR`, `DISTANCE` or `REFUELING`. A
  `REFUELING` row reuses the mileage columns (`odometer_meters`, `distance_meters`, `logged_odometer_meters`) for its
  optional mileage, and every row has a `note`.
- `event_picture.event_id` references `vehicle_event(id)`. `event_capture.event_id` is the key, with no foreign key
  on purpose (a capture outlives its event).
- In the domain, `VehicleEvent.Refueling.mileage: RefuelingMileage?` (`Added`/`Anchor`) mirrors
  `DistanceEntry`/`OdometerAnchor`, and `knownOdometerAt` has a branch for it. `Refueling.odometer` resolves an
  `Anchor` mileage.
- Logs are ordered `occurred_at, rowid` (`selectLog`/`selectRecentEvents` show it reversed), and `knownOdometerAt`
  depends on that order ("events at the same instant count in the order they were added").
- Android 13 (minSdk 33) ships SQLite 3.32, which has no `ALTER TABLE ... DROP COLUMN` (added in 3.35).

## Goals / Non-Goals

**Goals:**
- Every stored event holds one fact. The model expresses "these were entered together" without a compound event.
- Same-time ordering is deterministic, and the list order equals the derivation order.
- Existing data is migrated losslessly. The current odometer, notes, photos and captures are unchanged.
- Adding a future kind of event (maintenance, a standalone note) needs a new event type and, at most, a new entry
  shape. It never needs a new compound case.

**Non-Goals:**
- No new event kinds, no changes to the form's fields or validation, and no editing of an event's facts.
- No sync design. The entry is a sync-friendly unit, but nothing here commits to Firestore's shape.
- Refactoring `VehicleEvent` away from a sealed interface. It stays sealed, and each case is now one fact (see
  Decision 1's alternative).

## Decisions

### 1. Atomic events, grouped by an entry. Not a container event with facets.

An **entry** (`log_entry`) is one save: adding a vehicle, or saving the log event form. Each **event**
(`vehicle_event`) belongs to exactly one entry and holds exactly one fact. The atomic kinds after this change:

| Event | Fact | Attributes (not separate facts) |
|---|---|---|
| `ODOMETER_READING` | the odometer read a count at a moment (sets the odometer) | reading |
| `DISTANCE` | distance added | distance; `logged_odometer_meters` (the count it was computed from: provenance of this distance, never used to derive an odometer) |
| `REFUELING` | fuel added | amount, unit, fuel type, filled up |

What is **not** an event:
- **Note and photos**: annotations of the entry (Decision 3).
- **Scan capture**: provenance of one event's number (Decision 4).
- **Remembered vehicle, fuel unit, fuel type and tenths choice**: preferences in `app_state`/`vehicle`, outside the
  log.

**The initial odometer and an odometer anchor are merged into `ODOMETER_READING`.** Both record the same fact,
"the odometer read X at T", and `knownOdometerAt` already treats them alike: the latest one is the baseline.
Everything that told them apart is about where the entry came from, not about the fact:

| Today's difference | Now expressed by |
|---|---|
| Exactly one per vehicle, saved with the vehicle | the entry's `source = VEHICLE_ADDED` (only `addVehicle` writes it) |
| No note, no photos, no Edit | the entry's source: the add-vehicle form has no note or photos, and Edit is not offered for a `VEHICLE_ADDED` entry |
| Label "Initial odometer" vs "Odometer reading" | derived from the entry's source when the row is formatted |
| A scan accepted on the add-vehicle form is kept with it | unchanged: the scan is stored under the event whose number it filled (Decision 4) |
| A distance in the same minute as adding the vehicle counts | entry order (Decision 5), not the type |

*Alternative: keep two types* (the first draft of this design). It costs nothing now, but the rule "one type per
fact" would have its first exception on day one, and every consumer that only cares whether the odometer was set
would still match two types.

A `DISTANCE` logged by odometer is still one event. The user's input was a count, but the stored fact is the
distance, and the count is kept only to show it on the row. Splitting it into an odometer reading plus a distance
would turn every odometer-logged distance into an anchor and change how the odometer is derived.

*Alternative: a container event with facets* (the `generalize-vehicle-event-model` stub). One row per input, with
optional facets. It keeps "entered together" implicit but makes every consumer check which facets are present,
which is the same problem as today's nullable mileage, generalized. Atomic events keep each consumer narrow:
`knownOdometerAt` reads odometer-setting and distance events, and fuel statistics will read refueling events. Only
the screens that show an entry (details, edit) combine events.

*Alternative: link events by equal timestamps, with no entry.* Two separate inputs in the same minute would merge,
and notes and photos would have no single home. Rejected.

### 2. Schema

```sql
CREATE TABLE log_entry (
    id          TEXT    NOT NULL PRIMARY KEY,
    -- Where the entry was saved from: VEHICLE_ADDED (the add-vehicle form) or LOG_FORM (the log event form).
    -- The ODOMETER_READING of a VEHICLE_ADDED entry is the vehicle's initial odometer.
    source      TEXT    NOT NULL,
    -- The note typed when the entry was logged, shared by all of its events. Null when none.
    note        TEXT,
    created_at  INTEGER NOT NULL
);

-- vehicle_event: + entry_id NOT NULL REFERENCES log_entry(id); - note;
-- type: ODOMETER_READING | DISTANCE | REFUELING (INITIAL_ODOMETER and ODOMETER_ANCHOR are gone).
-- The mileage columns stay; after this change only DISTANCE/ODOMETER_READING fill them,
-- and only REFUELING fills the fuel columns.

-- event_picture: event_id -> entry_id REFERENCES log_entry(id).
-- event_capture: unchanged (keyed by event id).
```

- `occurred_at`/zone stay on the event, not on the entry. Each event is self-contained ("what happened when"), and
  the queries that derive the odometer read only `vehicle_event`. The repository writes the same moment to every
  event of an entry. That duplication is cheap and confined to one write path.
- `vehicle_id` stays on the event, for the same reason. The entry does not need one.
- An entry's **creation order** is `log_entry`'s rowid, which replaces `vehicle_event.rowid` as the tie-breaker.
- The initial odometer also gets an entry (`source = VEHICLE_ADDED`, note always null), so every event has
  exactly one entry. No nullable `entry_id` and no special cases.
- `source` is an open set of codes, so a future source (a trip's start or end, say) is a new code, not a new event
  type.
- The insert queries for each event type stop taking `note`. A new `insertLogEntry`, and `updateEntryNote`
  replaces `updateEventNote`. `touchEvent` becomes `touchEntry`: it updates every event of the entry, so the live
  queries of any row of the entry re-run.

### 3. Note and photos belong to the entry, shown from every one of its events

Stored once, on `log_entry`/`event_picture.entry_id`, and never copied. That avoids the alternatives' problems:
- *Duplicate onto each event*: photo files are shared or copied, and edits must keep the copies in sync.
- *Attach to a "primary" event* (the refueling): an ad-hoc rank of importance, and a mileage row cut off by the
  5-row recent list loses nothing, but a primary row cut off hides the icons.

The read model: every `VehicleEvent` exposes its entry's `note`/`photoIds`. The log queries join `log_entry` and
read photo presence per entry, so `EventRowContent` is unchanged in shape and every row of an entry shows the icons.
`updateEventNote`, `addEventPhoto` and `removeEventPhoto` keep their event-id parameters (the details screen
navigates by event id) and resolve the entry inside the repository.

A future standalone note fits either way. It can be a new `NOTE` event type in an entry, or an entry whose only
content is its note (then the rule "an entry has at least one event" is relaxed). That choice belongs to that
change. This one only makes sure the note already lives on the entry.

### 4. A scan stays keyed by event, and goes to the event whose number it filled

`event_capture` is unchanged. On save, the processor already knows the accepted scan's target (`ScanTarget`: the
fuel amount or the mileage). `addRefueling` takes the capture together with its target and inserts it under the
refueling event or the mileage event. Still one scan per form (the latest accepted). Keeping one scan per target
would be a reasonable follow-up, but it changes `odometer-ocr-capture`'s "a second scan replaces the first" and is
out of scope.

### 5. Ordering: by instant, then entry order, then kind within the entry

```
ORDER BY occurred_at, log_entry.rowid, kind_rank      -- oldest first (derivation)
ORDER BY occurred_at DESC, log_entry.rowid DESC, kind_rank DESC   -- newest first (lists)
kind_rank: ODOMETER_READING 0, DISTANCE 1, REFUELING 2
```

`kind_rank` is a `CASE` expression in the queries, not a stored column. A `VehicleEvent.Kind.rank` mirrors it in
Kotlin, so `FakeVehicleRepository` and tests sort the same way.

**Why kind order applies only within one entry, not to all events at the same instant** (the literal reading of
"sort by type when the timestamp is the same"). Any global order by kind changes how the odometer is derived for
same-minute entries, because times are kept to the minute and the form defaults to "now":
- *Odometer-setting before Distance:* the user logs a 30 km trip at 12:00 and then confirms a lower odometer count
  of 44,000 at 12:00. The anchor would sort first and the trip would be added on top, so the current odometer would
  read 44,030, not the 44,000 the confirmation dialog promised.
- *Distance before odometer-setting:* the vehicle is added at 12:00 and a trip is logged at 12:00 (the time the form
  offers). The trip would sort before the initial odometer and not count, which breaks `vehicle-log`'s explicit
  "An entry in the minute the vehicle was added counts".
- Either way, the list order would differ from the derivation order, and the rows would no longer read as the
  history the odometer was computed from.

Within one entry, kind order is safe: an entry holds at most one odometer-affecting event. That is the only place
where two events have no "added first" between them. A refueling's row then sits directly above its mileage row in
newest-first lists, which reads as "drove, then refueled".

`knownOdometerAt`'s contract changes from "ordered by instant and by when added" to this order. Its refueling
branch goes away: a refueling is now neither odometer-setting nor a distance.

### 6. Repository write path

`addRefueling(…, mileage: RefuelingMileage?, …)` keeps its signature. `RefuelingMileage` survives only as this
input type. In one transaction:
1. insert the `log_entry` (with the note);
2. insert the mileage event, if any: `DISTANCE` for `Added`, `ODOMETER_READING` for `Anchor`, through the same
   queries `addDistanceEntry`/`addOdometerAnchor` use;
3. insert the `REFUELING` event (fuel columns only);
4. insert the photos under the entry, and the capture under the event its target names;
5. update the remembered preferences, as today.

The insert order inside the transaction does not matter for ordering (Decision 5). `addDistanceEntry`,
`addOdometerAnchor` (`LOG_FORM`) and `addVehicle` (`VEHICLE_ADDED`) each write one entry and one event.
In the domain, `VehicleEvent.InitialOdometer` and `OdometerAnchor` become one `OdometerReading` case, and each event
carries its entry's `source`, so "is this the initial odometer" is `source == VEHICLE_ADDED`.

`observeEntry(vehicleId, eventId): Flow<LogEntry?>` returns `LogEntry(id, events, note, photoIds)` for the
details and edit screens. `EventDetailsProcessor` finds the refueling (if any) and the mileage event among
`events`, which is what today's `Refueling.mileage` gave it. `observeEvent` is removed once nothing uses it.

### 7. Rows

`EventRowContent` needs no new shape. A mileage event of a refueling is an ordinary `DistanceEntry` or
`OdometerReading` row, with the same label, figure, accent color and typed-count line. No visual grouping is added:
the shared time and adjacency make the pair clear. Tying the rows together (a bracket, a shared background) was
considered and left for later if the list proves hard to read.

## Risks / Trade-offs

- [The table rebuild in the migration is the riskiest step (foreign keys, indexes, row order)] → Use SQLite's
  documented 12-step rebuild with `PRAGMA foreign_keys` off. `VehicleMigrationJvmTest` gets a version-12 schema
  fixture with a refueling of each mileage shape, with notes, photos and a capture. It asserts the split, the
  unchanged current odometer, the moved notes and photos, and that the migrated schema equals a fresh one (the
  existing `aMigratedDatabaseHasTheSameTablesAsAFreshOne` covers the last).
- [A migrated refueling's capture may have been a mileage scan, yet stays on the refueling event] → Accepted.
  Captures are never shown and exist only to review misdetections, and the accepted detection's `kind`
  (`ODOMETER`/`TRIP`/`FUEL_AMOUNT`) still says what it was. Re-keying it in SQL would need `json_extract`, and
  relying on JSON1 on every Android 13 SQLite build is not worth it for data nobody sees.
- [Two rows per refueling make the 5-row recent list show fewer inputs] → Accepted, and specified: the list counts
  events. Showing combined rows was weighed against it; one row per fact keeps the distance accent on distances and
  the fuel accent on fuel.
- [Joining `log_entry` in every log query costs a little] → Logs are per vehicle and small, and the join is by
  primary key.
- [In-flight changes written against the old shape] → `sweep-event-pictures` reads `event_picture.id` only and is
  unaffected. `add-trip-logging` should be applied after this one (see "Relation to in-flight changes").

## Migration Plan

`12.sqm` (schema 12 → 13), inside SQLDelight's migration transaction:
1. `CREATE TABLE log_entry`, then `INSERT INTO log_entry SELECT id, CASE type WHEN 'INITIAL_ODOMETER' THEN
   'VEHICLE_ADDED' ELSE 'LOG_FORM' END, note, created_at FROM vehicle_event ORDER BY rowid`. Every existing event gets an entry **with the event's own id**, so entry order equals the old row order
   and `event_picture.event_id` values are already valid entry ids.
2. Rebuild `vehicle_event` as `vehicle_event_new` (with `entry_id`, without `note`):
   - every row: `entry_id = id`, with the mileage columns cleared on `REFUELING` rows, and `INITIAL_ODOMETER` and
     `ODOMETER_ANCHOR` both rewritten to `ODOMETER_READING`;
   - for each `REFUELING` row with `distance_meters` set: an extra `DISTANCE` row (`distance_meters`,
     `logged_odometer_meters`), and with `odometer_meters` set an extra `ODOMETER_READING` row. Each copies
     `vehicle_id`, `occurred_at`, zone, offset and `created_at`, with `entry_id` = the refueling's id and
     `id = refueling id || '-mileage'`. Event ids are opaque strings, and this one is deterministic, so the
     migration is testable without random ids;
   - drop the old table, rename, and recreate `vehicle_event_by_vehicle`, plus an index on `entry_id`.
3. Rebuild `event_picture` with `entry_id` (same values) and its index.
4. `event_capture`: no change. Keys are event ids, and the refueling keeps its id.

The current odometer is unchanged because a refueling's former `Added` mileage is now a `DISTANCE` event and its
`Anchor` mileage an `ODOMETER_READING`, at the same instant, in the same entry order. `knownOdometerAt` read them
the same way before.

A destructive migration (dropping the log) was acceptable per the request but is not needed. Rollback is a
reinstall of the previous build plus clearing data, as for every earlier migration. There is no downgrade
migration. The Maestro fixtures are regenerated (`./gradlew :shared:generateMaestroFixtures`,
`docs/test-fixtures.md`).

## Relation to in-flight changes

- `generalize-vehicle-event-model` (stub): answered in the other direction (Decision 1); the stub has been
  discarded.
- `add-trip-logging` (stub): a trip remains a record of its own, a span, not an event or a container. Whether
  membership is derived from the span or stored as a reference is its open question 2. If it is a reference, it goes
  on `log_entry`, so a refueling and its mileage join a trip together. Either way, a trip's distance is the sum of
  its entries' `DISTANCE` events. Its open question 4 (an odometer at start and end) can use ordinary entries holding
  an `ODOMETER_READING`/`DISTANCE` event (with a source of their own, if the trip wants to tell them apart).
- `sweep-event-pictures`: unaffected (reads `event_picture.id`).
- `add-fuel-type-ocr`: a fuel-type scan, if it gets one of its own, would go to the `REFUELING` event (Decision 4).

## Open Questions

- Whether the full log should visually tie the rows of one entry together (Decision 7) can be decided after using
  it. It does not change the model or the specs' row content.
