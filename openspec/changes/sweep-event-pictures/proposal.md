# Proposal (stub)

> **Stub.** Filed as an explicit follow-up of `add-event-pictures`, which deliberately excludes this from its own
> scope. Not ready to build: it needs `add-event-pictures`'s `event_picture` table and its second picture-store
> instance to exist first (see its design.md). The design itself is largely already resolved — see below — so this
> is mostly a sequencing stub, not an open-questions one.

## Why

`add-event-pictures` deliberately does not clean up photo files an abandoned edit session or a removed photo leaves
behind — its own design.md accepts that as a known, temporary gap. `vehicle-picture` already has exactly this
problem solved for vehicle pictures (`sweepPictures`, run once at app startup); event pictures need the same,
mirrored rather than newly designed.

## What Changes

- At app startup, alongside the existing vehicle-picture sweep, an equivalent sweep runs for event pictures: every
  photo file in the event pictures' storage root that no saved event references (a normal removal, or files left by
  an interrupted save or an abandoned edit session) is deleted; stale pending files (picked but never saved, past
  some age) are deleted the same way `vehicle-picture`'s sweep already ages out its own pending files.
- No change to how a photo is added, removed, shown or capped — this change only deletes files nothing refers to
  anymore.

## Capabilities

### Modified Capabilities
- `event-pictures`: adds the cleanup requirement, mirroring `vehicle-picture`'s existing "files that no vehicle
  refers to... SHALL be deleted when the app starts" requirement.

## Impact

- `shared/`: a new `sweepEventPictures(eventRepository, eventPictureStore)` function, parallel to the existing
  `sweepPictures(vehicleRepository, vehiclePictureStore)`, called alongside it at startup (`App.kt`). The underlying
  `sweep(referencedIds: Set<String>)` logic on the (generalized, per `add-event-pictures`) `FilePictureStore` needs
  no change — it already takes a generic referenced-id set; only the wrapper that computes that set from the
  database is new, and it is a small query (every `event_picture.id` currently in the table) rather than anything
  novel.
