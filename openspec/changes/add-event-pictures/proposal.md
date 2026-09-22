# Proposal (stub)

> **Stub.** Short on purpose: it fixes the scope and lists the open questions, to be settled before `/opsx:apply`. Filed as a follow-up of `add-event-notes`, alongside `add-event-details-view` and `add-event-editing`. Nothing here is built yet.

## Why

A note (`add-event-notes`) covers text, but some context is easier to capture as a photo: a dashboard shot for an
odometer count, a fuel pump or receipt for a refueling once that exists, proof of damage. The project context already
plans for pictures at the fuel-pump/receipt stage (ML Kit OCR); this is the plain "attach a photo to what you just
logged" version, with no reading of it yet.

## What Changes

- The log event form gains a picture element, in the spirit of the vehicle's own picture field (`vehicle-picture`):
  the system's own chooser of image sources, stored in the app's private storage, referenced by id only.
- A logged event's picture is attached when the event is saved, and shown as an indicator on its row (a thumbnail or
  an icon — open question) in the recent events and the full log.
- Viewing the picture full-size is likely part of `add-event-details-view` rather than this change; the two are
  closely related and may need reordering against each other.

Out of scope for the stub: OCR of the picture (reading an amount, a fuel type or a cost from it), multiple pictures
per event, and editing or removing a picture after the event is saved (`add-event-editing`).

## Capabilities

### New Capabilities
- `event-pictures`: a picture attached to a logged event, its storage and its indicator on the row.

### Modified Capabilities
- `distance-logging`: the log event form gains the picture element; saving stores it with the new event.
- `vehicle-log`: a distance entry or an odometer anchor event can carry a picture; its row shows that it has one.

## Impact

- Likely reuses the picture pipeline `vehicle-picture` already established (crop-to-square or not — open question;
  a fuel pump or dashboard photo may not want a square crop), its private-storage convention and `VehiclePictureStore`
  or an event-scoped equivalent.
- Schema: an event-picture reference, migration.
- Depends on, or should be sequenced against, `add-event-details-view` (where the full picture is likely viewed) and
  `add-event-editing` (removing or replacing a picture after saving).
