# Proposal (stub)

> **Stub.** Short on purpose: it fixes the scope and lists the open questions, to be settled before `/opsx:apply`. A **future** change: it is meant to come after `add-landing-screen` and `add-direct-logging`, which introduce the Home screen's "Log event" tile and leave the form it opens still called "Log distance" (their design lists this as an open question).

## Why

The Home screen's action is **"Log event"** but the form it opens, and the action on the vehicle details screen that opens the same form, are **"Log distance"**. That was right while a distance entry was the only kind of thing that can be logged; the app's purpose (project context) also includes refuelings, receipts and other contextual notes, all of them events on a vehicle's log.
The name should say what the screen is for before more kinds arrive, and the same word should be used everywhere the user sees it.

## What Changes

- **Names the user sees:** the details screen's "Log distance" action, the form's title and the Home tile read **"Log event"**; the spec texts that name the action follow. (Whether the form also gets a way to choose the kind of event is an open question.)
- **Names in the code and the flows:** the screen, processor and state classes (`LogDistance*`), the test tags (`log_distance`, `log_way_distance`, and so on) and the Maestro flows follow (open question 3).
- **Not renamed:** stored names. The column `vehicle.log_distance_tenths` and the migrations that created it (`2.sqm`) stay as they are (a rename would need a migration for no user-visible gain), and the event type code `DISTANCE` is a kind of event that remains.

Out of scope: adding a second kind of event (refueling, notes: each its own change), changing what a distance entry is or how it is validated, a new icon, translations.

## Capabilities

### New Capabilities

None (see open question 4 about renaming the capability itself).

### Modified Capabilities

- `distance-logging`: the requirement that opens the form from the details screen is renamed and reworded ("Log a distance from the vehicle details screen" becomes "Log an event from the vehicle details screen"); other requirements that mention the action's name are reworded in the same way when this is applied.
- `vehicles`: the details screen requirement's scenario that mentions "Log distance".

## Impact

- `shared/`: `VehicleDetailsScreen` (the action), `LogDistanceScreen` (title), the `LogDistance*` classes and their tests if they are renamed (about 270 mentions), the landing tile and its tests.
- `maestro/`: the tags and the texts that flows use (`log_distance`, "Log distance"); `docs/test-strategy.md` (an example names `LogDistanceRulesTest`).
- `openspec/specs`: the wording above. No storage, dependency or permission change.
