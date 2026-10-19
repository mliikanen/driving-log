# Proposal

## Why

Six specs state things the app stopped doing when later changes added to it (#31, #34). A Purpose describes only
distance entries; the add vehicle form "returns to the vehicle list" also when opened from Home; the field order
misses the fuel type; the log "is only ever added to" except a note edit, though photos and refuelings are edited too;
the Kind selector has rules for a single kind that can't happen; a platform note names Android and iOS APIs. A test
written from these would fail against a correct app, and a proposal reading them starts from a wrong picture.

They keep appearing for two reasons (design.md, Context): a change writes deltas only for the requirements it sets
out to change, and nothing makes it look for the statements elsewhere that its new behavior makes false; and OpenSpec
cannot rename or drop a scenario through MODIFIED, so changes that tried kept stale scenarios on purpose.

## What Changes

- **`distance-logging`**: the Purpose covers refuelings, the Home screen route, notes and photos. "Log an event from
  the vehicle details screen", "A pending note is saved with the event" and "Photos can be added to the log event
  form" include a refueling. "Logging only adds to the log" names the real exception: an entry's note and photos.
  "The kind of event is chosen" is replaced by "The kind of event is chosen from Distance and Refueling", without the
  single-kind rule and its two scenarios.
- **`vehicles`**: the Purpose lists every editable field. "Add a vehicle" returns to the screen it was opened from
  (the app's behavior, which `maestro/subflows/add-vehicle.yaml` relies on) and includes the fuel type; the fixed
  field order puts the fuel type between type and color.
- **`vehicle-log`**: "The log is not changed by editing the vehicle" lists every vehicle field and names the real
  exception; two requirements cite `event-details` by its new name instead of the old name or change names.
- **`event-details`**: "A 'Distance' or 'Odometer reading' event's note can be edited" is renamed "An event's note
  and photos can be edited"; "The details screen shows a 'Distance' or 'Odometer reading' event's photos" is renamed
  "The details screen shows an event's photos" and includes a refueling.
- **`app-shell`**: "Log event" is unavailable only while the user has no vehicle, not "until the changes that build
  it".
- **`vehicle-picture`**: "A vehicle can have one optional picture" is replaced by "A vehicle has at most one
  picture", the same requirement with the placeholder scenario named for what it checks. The chooser's platform note
  states the platform behavior instead of the APIs (recorded in the designs of `add-vehicle-picture` and
  `ask-camera-for-camera-app`).
- **Code and tests, behavior unchanged**: the Kind selector's dead single-kind branch goes; `CropStateTest`'s three
  "move button" tests and two `CropState` comments name the arrow keys.
- **Root cause**: `openspec/config.yaml`'s artifact rules make every proposal sweep the main specs, tests and comments
  for statements its behavior makes false, and say how to fix a stale scenario name OpenSpec can't rename.

Out of scope:
- "Vehicle details screen"'s scenario "Open the log distance form": `add-vehicle-hiding` modifies that requirement,
  so renaming the scenario here would conflict. That change gets a comment on its PR to rename it the same way.
- `vehicles`' "Odometer readings are entered in a microwave-style number field" (#34): #32 already moved the other
  screens into "Every odometer field behaves the same", which names the log event form. Nothing is left to fix.
- A spec rule that fuel amounts show two decimals, or other new behavior.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `distance-logging`: the requirements and Purpose above.
- `vehicles`: "Add a vehicle", "The add vehicle form's fields come in a fixed order", the Purpose.
- `vehicle-log`: "The log is not changed by editing the vehicle", "Distance and odometer-anchor events can carry a
  note", "Distance and odometer-anchor events can carry photos".
- `event-details`: two requirements renamed, one widened to refuelings.
- `app-shell`: "Home screen actions that are not available are disabled".
- `vehicle-picture`: "A vehicle can have one optional picture" (replaced), "The photo comes from an app the user
  chooses through the system" (platform note).

## Impact

- Specs as above. The two Purposes are edited in the main specs directly, as OpenSpec keeps them out of deltas.
- `shared/.../vehicle/distance/LogEventChoices.kt` (`KindSelector`), `shared/.../vehicle/picture/CropState.kt`
  (comments), `shared/src/commonTest/.../vehicle/picture/CropStateTest.kt` (three names). No behavior changes, so no
  Maestro flow changes and no release notes.
- `openspec/config.yaml` rules and `docs/change-workflow.md` (Proposing).
