# Proposal

## Why

Driving Log has an app shell but nothing to log against: every later feature (odometer readings, refuelings,
trips, camera capture) belongs to a vehicle. Users need to be able to define their vehicles first. This change
also brings the first persistent data, so it settles how data is stored on the device, in line with the
offline-first and additive data model rules, until Firestore support arrives.

## What Changes

- Users can add a vehicle with a name (required), a license plate (optional), an odometer unit and a current
  odometer reading (required: the field starts empty, and a typed 0 is valid). The odometer unit is a per-vehicle setting: kilometers, kilometers with a 100 m
  indicator (one decimal), miles, or miles with tenths (one decimal). The tenths units keep the extra digit that
  such odometers show, which also suits readings captured by OCR later. The form preselects a default from the device region (miles in the US, UK, Liberia
  and Myanmar, kilometers elsewhere), the user can change it, and it is fixed once the vehicle is added.
- The initial odometer is entered in a microwave-oven-style number field that uses the system number keyboard:
  digits enter from the right and fill the field from its right edge, so with a tenths unit typing 1, 2, 3 shows 0.1,
  1.2, 12.3, and with a whole-number unit 1, 12, 123. Backspace removes the last digit (typing 3 then backspace on 1.2
  goes 12.3 and back to 1.2). The decimal separator is added automatically and drawn with the device locale's
  separator. Whole-number units take no decimals. Readings everywhere are shown in the vehicle's unit using the device
  locale's decimal and thousands separators. Stored data never depends on the locale (whole meters, enum codes, UUIDs,
  epoch milliseconds).
- The Home screen becomes the vehicle list: all vehicles the user has added, with an action to add one and an
  empty state when there are none.
- Tapping a vehicle opens its details screen: name, license plate, current odometer in the vehicle's unit, the 5
  most recent log events, and a way to open the full log.
- Users can open the full log of a vehicle, listing every event newest first.
- Users can change a vehicle's name and license plate. Surrounding whitespace is trimmed from both, on add and
  on edit.
- Each vehicle has an event log. The only event type in this change is the initial odometer reading, written
  when the vehicle is added. The current odometer is derived from the log, never stored as a running total, so
  the log stays additive.
- Vehicles and their log are stored in a local SQLite database, using SQLDelight, which works on Android and
  iOS from shared code. Data survives app restarts and works fully offline.
- The app gains its first multi-screen navigation, using `kide-navigation` (deferred in `app-basic-stub` until a
  second screen existed).
- Maestro flows verify the user journeys on Android.

Out of scope (each is its own later change): editing the odometer or adding other event types (refuelings, trips,
maintenance), changing a vehicle's odometer unit after it is added, deleting, hiding or sharing vehicles, login and
per-user data ownership, Firestore and sync, a stored user-level unit preference and its Settings screen (until then
the default comes from the device region), a custom on-screen keypad for odometer entry, fuel units (gallons, mpg), localized digits and dates, vehicle
identification from the camera or odometer, and the iOS app (the code is shared, but no Xcode project exists to run Maestro against).

## Capabilities

### New Capabilities
- `vehicles`: adding, listing, viewing and editing vehicles, including the per-vehicle odometer unit and its
  region-based default, the odometer number field, input trimming and validation, locale-aware display, and keeping them
  across restarts and without a network.
- `vehicle-log`: the per-vehicle event log: its initial odometer event, the current odometer derived from it, the
  5 most recent events on the details screen, and the full log screen.

### Modified Capabilities
- `app-shell`: the Home screen requirement changes from a static empty state with no actions to the vehicle list,
  with an empty state that offers adding a vehicle.

## Impact

- Code: new `vehicle` feature packages in `shared/src/commonMain` (domain, data, list, add, details, edit, log
  screens and processors); SQL schema and queries under `shared/src/commonMain/sqldelight`; driver factories in
  `androidMain` and `iosMain` (including a small device-locale provider for the region and the separators); navigation keys replace the single-screen setup in `App.kt`, `MainActivity`,
  `HomeViewModel` and `MainViewController`.
- Dependencies: SQLDelight (Gradle plugin, runtime, coroutines extensions, Android and native drivers, and the JVM
  SQLite driver for Android host tests), `kide-navigation`, and kotlinx-serialization (required by Kide navigation
  for state persistence). No Firebase.
- Tooling: Maestro flows in `maestro/`, run against the Android emulator; Maestro is already installed here.
- Specs: new `vehicles` and `vehicle-log` specs; `openspec/config.yaml` project context gains the storage and
  navigation choices.
- Data: this is the first schema (version 1); each vehicle stores its odometer unit, while distances are stored in
  meters, and nothing stored depends on the locale. With no login yet, vehicles are not tied to a user; adding
  ownership when accounts arrive will be a migration.
- Verification: this is the first change to exercise SQLDelight with AGP 9.4.1's KMP library plugin and Kotlin
  2.4.20, so it may surface build fixes. iOS compiles but cannot be run on this Linux machine.
