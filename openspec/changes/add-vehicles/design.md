# Design

## Context

See proposal.md for motivation. Current state (from the archived `app-basic-stub` change):

- One screen: `App(homeProcessor)` renders `HomeScreen`. A Metro `AppGraph` provides `HomeProcessor`;
  `createAppGraph()` is called by `DrivingLogApplication` (Android) and `MainViewController` (iOS).
  On Android a `HomeViewModel` keeps the processor across rotation.
- Kide 2.2.0 core only. `kide-navigation` was deliberately deferred until a second screen exists; this change adds five.
- Build: Kotlin 2.4.20, AGP 9.4.1 with the `com.android.kotlin.multiplatform.library` plugin (`androidLibrary {}` in
  `shared`), `minSdk` 33, Android host tests enabled. `kotlinx-datetime` is already a dependency. No database, no
  kotlinx-serialization, no KSP.
- Project rules that shape this: offline-first, additive (append-only) data model, distances stored in meters,
  business logic in `commonMain`, tests without a device where possible, Maestro for flow tests, Firestore later.

## Goals / Non-Goals

**Goals:**
- A local database that works from shared code on Android and iOS, with a schema that Firestore sync can later mirror.
- Five screens (list, add, details, edit, full log) built as the reference pattern for later features.
- Validation and formatting logic in pure common Kotlin, unit tested.
- Maestro flows that cover every user journey in the specs.

**Non-Goals:**
- Sync, user accounts and ownership, deletion or hiding, other event types, changing a vehicle's odometer unit after
  creation, a custom on-screen keypad for odometer entry, a stored user-level unit preference and Settings screen (the default comes from the device region for
  now), fuel units, localized digits (numerals stay 0 to 9) and localized dates (dates use one fixed format).

## Decisions

### 1. SQLDelight 2.4.0 for local storage (over Room KMP)

SQLDelight generates typed Kotlin from `.sq` files and ships drivers for Android and iOS (native), so the schema, queries
and repository live in `commonMain`. Version 2.3.2 and later support AGP 9's new DSL and built-in Kotlin, and it needs
no KSP.

- Why it fits here: a SQL-first schema is easy to mirror in Firestore documents later; migrations are plain `.sqm` files
  that can be verified; queries return Flows through the coroutines extension, which suits Kide processors; and Android
  host tests can run the real SQL on the JVM with the JDBC SQLite driver, with no Robolectric or emulator.
- Alternative: Room KMP (2.8.x, `androidx.room` plus the bundled SQLite driver). It is Google-supported and also KMP,
  but it needs KSP (another compiler plugin next to Metro and the Compose compiler, on Kotlin 2.4.20 and AGP 9), and
  the bundled driver cannot run in Android host tests, so repository tests would need Robolectric or a device. Kept as
  the fallback if SQLDelight does not work with the KMP library plugin (see Risks).
- Alternative: Realm/others. Rejected as not SQLite.

### 2. Schema (version 1) and the additive log

```sql
CREATE TABLE vehicle (
    id            TEXT    NOT NULL PRIMARY KEY,   -- random UUID, safe to create offline and sync later
    name          TEXT    NOT NULL,
    license_plate TEXT,                           -- NULL when there is none
    odometer_unit TEXT    NOT NULL,               -- 'KILOMETERS' | 'KILOMETERS_TENTHS' | 'MILES' | 'MILES_TENTHS', chosen at creation
    created_at    INTEGER NOT NULL,               -- epoch milliseconds
    updated_at    INTEGER NOT NULL
);

CREATE TABLE vehicle_event (
    id              TEXT    NOT NULL PRIMARY KEY,
    vehicle_id      TEXT    NOT NULL REFERENCES vehicle(id),
    type            TEXT    NOT NULL,             -- 'INITIAL_ODOMETER' is the only value for now
    occurred_at     INTEGER NOT NULL,             -- epoch milliseconds
    odometer_meters INTEGER,                      -- nullable: future event types may not carry a reading
    created_at      INTEGER NOT NULL
);
CREATE INDEX vehicle_event_by_vehicle ON vehicle_event(vehicle_id, occurred_at DESC);
```

- The vehicle row holds only attributes of the vehicle itself (name, plate, unit). It has no odometer column: the current odometer
  is derived from the latest event that has an odometer, ordered by `occurred_at DESC, rowid DESC`. That is the
  additive model, and it makes the tie rule in the spec ("added last comes first") a query, not extra state.
- Events are only ever inserted in this change; there is no update or delete query for them.
- Distances are stored as whole meters (`INTEGER`), per the project rule, whatever the vehicle's unit. Input converts
  to meters and display converts back into the vehicle's unit (decision 4).
- `odometer_unit` is the vehicle's own setting (the project context makes the unit overridable per vehicle), so it is
  a stored attribute and not derived. It is written once, when the vehicle is added; there is no query that updates it.
- **Storage is locale-agnostic:** every stored value is an integer (meters, epoch milliseconds), a UUID, or an enum code
  such as `'KILOMETERS'` (never a translated label). No formatted numbers or dates are stored, and parsing the text the
  user typed happens once, before saving, with rules that do not read the device locale.
- `updated_at`, `created_at` and UUID ids are there so a later sync change does not need to rewrite the schema.
- No `user_id` yet, since there is no login. Adding ownership later is a migration (`.sqm`).
- Adding a vehicle inserts the vehicle and its initial event in one transaction (spec: both or neither).
- Editing runs one `UPDATE vehicle SET name, license_plate, updated_at`. It never touches `vehicle_event`.

### 3. Domain and repository

```
com.mikonoma.drivinglog.vehicle
  domain/   Vehicle, OdometerUnit (enum), VehicleEvent (sealed; InitialOdometer), Distance (meters), VehicleRepository
  input/    VehicleInput: name and plate trimming and validation; OdometerEntry: microwave-style digit model
  data/     SqlDelightVehicleRepository, DatabaseDriverFactory (expect/actual)
  list/ add/ details/ edit/ log/   one package per screen: contract, processor, screen, nav key
```

`VehicleRepository` exposes `observeVehicles()`, `observeVehicle(id)`, `observeRecentEvents(id, limit)`,
`observeLog(id)`, `addVehicle(name, plate, unit, odometerMeters)` and `updateVehicle(id, name, plate)`. Processors depend on
the interface, so processor tests use an in-memory fake and repository tests use a real in-memory database.
No use-case layer (`kide-clean-architecture`) yet: every operation is one repository call, so it would only add
indirection. Revisit when logic spans repositories (for example, refueling that prompts for an odometer).

`addVehicle` takes an injected `Clock`, an id generator and a dispatcher, so tests control time and ids.

### 4. Validation, conversion and formatting are pure common Kotlin

- **Trimming:** `String.trim()` (Unicode whitespace) on name and plate, then: empty name is an error; empty plate is
  `null`. Inner whitespace is untouched. The same function serves add and edit.
- **Odometer entry is a digit model, not text parsing.** `OdometerEntry` holds the unit and one non-negative `Long`, the
  value counted in the unit's step (whole kilometers or miles for the whole-number units; tenths for the tenths
  units). `press(d)` sets `value = value * 10 + d` unless the result would exceed the cap (`9 999 999` steps for whole
  units, `99 999 999` for tenths, that is 7 whole digits), in which case it is ignored; pressing 0 at 0 stays 0 for free.
  `backspace()` sets `value = value / 10`; `clear()` sets 0. So the tenths digits 1, 2, 3 give 1, 12, 123 steps, shown
  as 0.1, 1.2, 12.3, and from 12.3 backspace gives 1.2, 0.1, 0.0 (and stays 0.0), while 1.2, then 3, then backspace goes
  12.3 and back to 1.2.
- **Applying a text edit:** the system IME reports the field's new text, not key presses, so `applyEdit(newText)`
  reduces the edit to the model. Take the digits of `newText` (dropping every non-digit, which ignores `,`, `.` and `-`,
  typed or pasted). The field's previous digit text is the value's digits (`"0"` for zero). If the new digits start with
  the previous ones, `press` each extra digit (a paste of "123" onto "0" presses 1, 2, 3); if the previous digits start
  with the new ones, `backspace` once per removed digit; otherwise (for example select-all then type) `clear()` and
  press every digit. The field is then re-rendered from the model, which also normalizes leading zeros. There is no
  parsing of separators, no locale, and no invalid state.
- **Unit change keeps the number:** switching between a whole and a tenths unit rescales `value` (whole to tenths:
  `* 10`; tenths to whole: `(value + 5) / 10`, rounding half up); switching within the same kind (for example
  Kilometers to Miles) keeps `value` as is, because a reading is a number read off a dial, not a distance to convert.
- **Conversion to meters** uses integer arithmetic only, from `value` in steps. Kilometers: `meters = value * 1000`;
  kilometers with 100 m: `value * 100`. Miles: `(value * 1 609 344 + 500) / 1000`; miles with tenths:
  `(value * 1 609 344 + 5000) / 10 000` (a mile is 1 609.344 m), rounded to the nearest meter. The largest product is
  about 1.6e14, within a `Long`.
- **Display from meters** into the vehicle's unit, rounding half up to the unit's step: `KILOMETERS` whole
  kilometers `(m + 500) / 1000`; `KILOMETERS_TENTHS` tenths `(m + 50) / 100`, always one decimal digit; `MILES` whole miles
  `(m * 1000 + 804 672) / 1 609 344`; `MILES_TENTHS` tenths of a mile `(m * 10 000 + 804 672) / 1 609 344`, always one
  decimal digit (the largest intermediate is about 1.6e17, within a `Long`). The values are assembled as integer part and fraction, again without floating
  point.
- **Locale-aware rendering:** the formatter takes a small `NumberSymbols(decimalSeparator, groupingSeparator)` and
  builds the text from the integer parts: group the integer part in threes with the grouping separator, then the
  decimal separator and fraction, then a space and `km` or `mi`. A `DeviceLocale` provider supplies the symbols and the
  region (see decision 8). Digits stay ASCII 0 to 9 in every locale, a deliberate limit for now. So 45200.3 km is
  "45,200.3 km" in English (US) and "45 200,3 km" in Finnish (the grouping separator there is a no-break space).
- **Dates** use one fixed format, `yyyy-MM-dd HH:mm` in the device's time zone. Localized dates are left for later.
- **List order:** sorted in Kotlin by `name.lowercase()`, then `createdAt`. SQLite `COLLATE NOCASE` only folds ASCII, so
  it would put "Ärhäkkä" and "ärhäkkä" apart.

### 5. Navigation with `kide-navigation` (Navigation 3)

`kide-navigation` 2.2.0 supports Android and iOS and brings Navigation 3 and the Compose 1.12 line this project uses.
Each screen gets a `ScreenNavKey` that ties the screen to its processor: `VehicleListNavKey` (start),
`AddVehicleNavKey`, `VehicleDetailsNavKey(vehicleId)`, `EditVehicleNavKey(vehicleId)`, `VehicleLogNavKey(vehicleId)`.
Kide keeps the back stack and each screen's state across configuration changes and process death, which replaces the
Android-only `HomeViewModel` from the stub (deleted, along with the `home` package: the vehicle list is the Home screen).

- **Keys are registered prototypes that carry the graph.** Kide restores a screen after process death by finding the key in
  `ScreenNavKeyRegistry` by its `serialKey` and calling `restoreArgs(savedArgs)`, which returns a new key. So each key
  class takes the `AppGraph` in its constructor (no global holder), `createProcessor()` asks the graph for its processor
  (an assisted factory for keys that carry a vehicle id), `saveArgs()` returns the vehicle id and `restoreArgs` builds a
  new key with it. The keys are registered once at startup, from a function in `shared` that both platform entry points
  call. Screens that open another screen build the next key from the graph they hold.
- **State persistence is optional.** `stateSerializer` and `saveArgs` default to `null` in Kide 2.2.0. Only the add and edit
  forms provide `@Serializable` state (so typed text survives process death; repository-backed data is `@Transient`),
  which needs the kotlinx-serialization plugin. The list, details and log screens reload from the database.
- Screens navigate through `ScreenContext.navigateTo(key)` and `onBack`, driven by side effects from the processor.

### 6. The unit is a per-vehicle setting, defaulted from the device region

The add form's state holds `unit: OdometerUnit`, initialized from `defaultOdometerUnit(regionCode)`, and shows the four
units as radio options with the default selected. `defaultOdometerUnit` is a pure function: `MILES` for the region codes
`US`, `GB`, `LR` and `MM`, otherwise (or when the region is unknown) `KILOMETERS`. It stands in for the user-level
preference (project context: the per-user unit system, with a per-vehicle override), which does not exist yet: when a
stored preference and Settings screen arrive, only the source of this default changes and the vehicle's own unit stays
as stored. The mapping is a list of four region codes, easy to revise. The default is always a whole-number unit; the two
tenths units exist so an odometer that shows the extra digit (including a reading read by OCR later) is stored and
shown without losing it.

Name validation runs at save time. The odometer cannot be invalid because the entry model only accepts digits within the
cap, and changing the unit rescales the entry (decision 4). The edit
screen has no unit or odometer fields and `updateVehicle` cannot change either. A later change can add unit switching
(the log stays valid because it is stored in meters).

### 7. The odometer field uses the system IME

No custom keypad. A reusable `OdometerField` composable (in `ui/`, since refueling and OCR confirmation also enter
readings) wraps a Compose `BasicTextField` and lets the system keyboard do the typing, in number mode.

- **Keyboard:** `KeyboardOptions(keyboardType = KeyboardType.Number)`, which is `TYPE_CLASS_NUMBER` on Android and the
  number pad on iOS, so no separator key is offered where the platform allows it. Some Android keyboards still show
  `.`, `,` and `-` in number mode, so non-digits are also dropped in `applyEdit` (decision 4). Hardware keyboards work the
  same way.
- **State:** the field's text is the model's digits (`value` without leading zeros, `"0"` for zero); the selection is
  always forced to the end. `onValueChange` passes the new text to the add screen's processor as one intent, which
  calls `applyEdit`. The processor owns the `OdometerEntry`, so all the behavior is unit tested without Compose.
- **Display:** a `VisualTransformation` renders the digits through the same locale-aware formatter as every reading
  (decision 4): the tenths units insert the locale's decimal separator before the last digit, padding to at least
  `0` + separator + digit, and integer digits get the locale's grouping separator. The offset mapping puts the cursor
  at the end. So the user never types a separator and still sees the locale's one.
- **Right edge fill:** text alignment is end, and the unit abbreviation is a separate fixed trailing label at the field's
  right edge, so digits enter next to the label and push the earlier ones left, and the right end of the number does
  not move. A small clear icon after the label resets the value to 0. Long values shrink or scroll rather than wrap.
- **Accessibility:** the field has a label ("Odometer" with the unit) and announces its formatted value; the clear
  icon has a content description.
- **Test tags:** `odo_field` and `odo_clear`. Maestro drives it like a user: `tapOn` the field, `inputText` digits (a
  multi-digit commit is handled by `applyEdit`) and `eraseText` for backspace, then asserts the visible text.
- The form scrolls so the system keyboard does not hide the fields, and the odometer field stays visible while it is
  focused (checked in final verification).

### 8. Dependency injection

`AppGraph` gains a singleton `VehicleRepository` and `Clock`, plus assisted factories for the id-carrying processors.
It is created with `createAppGraph(driverFactory)`: `DatabaseDriverFactory` is an `expect class` (Android wraps
`Context`; iOS has none) and is passed into the graph as a `@Provides` input, so the graph builds the `SqlDriver`
and the database once. `DeviceLocale` is a second `expect class` with no constructor arguments, provided by the graph:
it reads the current locale on every call (Android: `Locale.getDefault()` and `DecimalFormatSymbols`; iOS:
`NSLocale.currentLocale` for the region, decimal separator and grouping separator), so a locale change is picked up
without restarting. Tests pass a fake `DeviceLocale`. `DrivingLogApplication` and `MainViewController` supply the platform factory. The `createGraph`
call stays inside `shared`, where the Metro plugin is applied.

### 9. Tests

- `commonTest`: name and plate validation; `OdometerEntry` (1, 2, 3 giving 1, 12, 123 and 0.1, 1.2, 12.3; backspace:
  1.2 then 3 then backspace gives 12.3 then 1.2, 12.3 backspace three times gives 1.2, 0.1, 0.0, the sequence 1, 2,
  backspace, backspace, 2, 3, 0 showing 0.1, 1.2, 0.1, 0.0, 0.2, 2.3, 23.0, backspace at zero,
  backspace on a whole unit 123 to 12; clear; zero at zero; the digit cap; `applyEdit` for appended digits, deletions,
  pasted digits, non-digits ignored and replacement; unit changes rescaling and rounding); conversion and formatting (English (US)
  and Finnish symbols, rounding of prepared meter values for whole-number units); `defaultOdometerUnit` for US, GB, FI
  and an unknown region; processors
  with `kide-test` against a fake repository (navigation side effects, error states, no save on invalid input).
- Repository tests run real SQL against an in-memory database, using an `expect` test driver factory: `androidHostTest`
  uses the JDBC SQLite driver (runs on the JVM here); `iosTest` uses the native in-memory driver (not runnable on
  this machine). Cases: atomic add, the unit stored and returned per vehicle, initial event contents, derived current odometer with a prepared newer event, 5 most
  recent of 7 events, same-time tie order, edit leaves the log untouched. Persistence across reopening uses a
  temporary file database in the JVM test. A locale test adds vehicles under the Finnish and English (US) default
  locales (`Locale.setDefault`) and asserts the stored rows are identical.
- **Maestro** (project rule) covers the flows on the Android emulator, in `maestro/`, one file per journey, each starting
  with `launchApp` and `clearState: true`: add with all fields, add with name and unit only (defaults), one add per unit
  (kilometers, kilometers with 100 m, miles, miles with tenths) checking the displayed odometer, the preselected default unit, odometer
  entry (microwave-style fill, backspace including 1.2, 3, backspace, clear, maximum length, unit change) and trimming, list order and empty state, details and full log, edit (change, clear plate, empty name refused, cancel),
  and persistence across a restart with airplane mode on. Interactive elements get Compose `testTag`s and the root sets
  `testTagsAsResourceId` so Maestro can select them by id; visible text is used for assertions. Maestro 2.10.0 is
  already installed. iOS flows wait for the Xcode project.

## Risks / Trade-offs

- [SQLDelight 2.4.0 with AGP 9.4.1's `com.android.kotlin.multiplatform.library` plugin and Kotlin 2.4.20 is untested
  here; SQLDelight's release notes only mention AGP 9.0 and built-in Kotlin support] → Task 1.1 is a spike that builds
  and tests before anything else is written. If it fails, stop and switch to Room KMP (decision 1) after confirming
  with the user.
- [iOS native driver needs `-lsqlite3` linked, and iOS code cannot be built into an app or run here] → Add the linker
  option, verify klib compilation for both iOS targets, and leave running it for when the Xcode project exists.
- [Persisting form state adds a serializer for the two forms] → Small and optional in Kide; it keeps typed text across
  process death.
- [No `user_id` on vehicles, so data created now has no owner once login arrives] → Accepted: pre-login data is
  local test data; ownership arrives with a migration in the change that adds accounts.
- [Dates use a fixed format and digits stay 0 to 9 in every locale, so the app is only partly localized] → Deliberate
  for now; only the separators the user asked for follow the device locale.
- [Deriving the default unit from the region stands in for a user-level preference that does not exist yet, and the
  region list (US, GB, LR, MM) is a judgment call] → Isolated in one pure function with tests; the user can always
  change the unit on the form; replaced when the preference and Settings screen are built.
- [Whole-number units round the shown reading, so 123.5 entered for a "Kilometers" vehicle is shown as 124 km while
  123 500 m is stored] → Accepted and specified: storage keeps what was entered, display follows the unit.
- [The Android emulator's default locale is English (United States), which preselects "Miles"] → Maestro flows choose the
  unit explicitly, and the default-unit check states the locale it expects; other regions are unit tested and checked
  by hand with a per-app locale.
- [Multi-event scenarios cannot be reached through the UI] → Covered by repository and processor tests with prepared
  data, and stated in the spec.
