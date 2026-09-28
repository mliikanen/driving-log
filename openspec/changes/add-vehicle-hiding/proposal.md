# Proposal

## Why

A vehicle the user no longer drives (sold, scrapped, a borrowed one) stays in the vehicle list and the log event form's
selector for good: there is no way to put it away. The project's own rule is that vehicles are never deleted, only
hidden and restorable (the vehicles a user has will later be shared with others, whose history must not vanish), so
the way to put one away is to hide it, from its details screen, after a confirmation.

(The request was to remove a vehicle and all its data. Asked, the developer chose hiding, which the project context
already specifies, over deletion; nothing is deleted by this change.)

## What Changes

- **Hide a vehicle** from its details screen: an overflow action "Hide vehicle", confirmed in a dialog that says what
  hiding does. The vehicle leaves the vehicle list and the details screen closes.
- **A hidden vehicle keeps everything**: its log, pictures, notes, scans and every other record stay stored as they
  were; only where it is shown changes.
- **Hidden vehicles are not offered anywhere** but where they are restored: not in the vehicle list, not in the log
  event form's vehicle selector (the vehicle last logged for being hidden, the selector starts on the first visible
  one), and not as a candidate for any automatic vehicle detection (`suggest-vehicle-from-odometer`, later).
- **Restore**: when any vehicle is hidden, the vehicle list ends with a "Hidden vehicles" entry, opening a screen that
  lists them with a "Restore" action each; a restored vehicle is back in the list and the selector, with all its data.
- **Home screen**: the vehicles action opens the vehicle list while the user has any vehicle, hidden or not (so a user
  who has hidden every vehicle can still reach them to restore); "Log event" needs a visible vehicle.

## Out of Scope

- Deleting a vehicle or any of its data (the project's rule; see Why).
- Hiding a vehicle from the list by a swipe or long-press: from the details screen only, as asked.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicles`: the vehicle list shows visible vehicles and a "Hidden vehicles" entry; the details screen offers "Hide
  vehicle"; hiding (with confirmation) and restoring are added.
- `distance-logging`: the Home screen's vehicle selector lists visible vehicles only, and starts on a visible one when
  the vehicle last logged for is hidden.
- `app-shell`: the Home screen's vehicles action counts hidden vehicles; its "Log event" action needs a visible one.

## Impact

- A `.sqm` migration adding a nullable `hidden_at` to `vehicle` (makes every checked-in Maestro fixture stale;
  regenerated, per `docs/test-fixtures.md`).
- `VehicleRepository`: visible and hidden vehicles observed apart; hide and restore; the picture sweep keeps hidden
  vehicles' pictures.
- The vehicle details screen (overflow menu, dialog), the vehicle list (the entry), a new hidden-vehicles screen and
  its navigation key, the Home screen's processor.
- The `vehicles` Maestro manifest gains a flow hiding and restoring a vehicle.
