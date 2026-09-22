# Design (stub)

## Context

`vehicle-picture`'s sweep is already generic where it matters: `FilePictureStore.sweep(referencedIds: Set<String>)`
(after `add-event-pictures`'s rename) deletes any file in its root whose id is not in the passed set, plus stale
pending files, with no vehicle- or event-specific logic inside it. Only the thin wrapper that computes
`referencedIds` (`sweepPictures`, `shared/.../vehicle/picture/PictureSweep.kt`) is per-owner, querying
`VehicleRepository` for every vehicle's `pictureId`. It is called once at startup from `App.kt`.

## Decisions (mostly settled already; recorded here rather than re-argued)

1. **A parallel `sweepEventPictures` wrapper**, not a change to `sweep()` itself: `add-event-pictures`'s design
   already chose a *separate* `FilePictureStore` instance (a separate root) for event pictures rather than sharing
   the vehicle one, so the two sweeps stay independent — each computes its own referenced-id set from its own table
   (`event_picture` vs. `vehicle.picture_id`) and sweeps its own root. No shared-root union logic is needed.
2. **Called alongside the existing sweep at startup**, same place (`App.kt`'s `LaunchedEffect`), same
   `runCatching` wrapping so a sweep failure never blocks the app from starting.

## Open questions

1. Pending-file age-out: `vehicle-picture`'s sweep ages out pending files older than 24 hours. Does event pictures'
   sweep reuse the same 24-hour threshold, or does a different one make sense given up to 5 pending files can exist
   per in-progress event (vs. one for a vehicle)? Likely reuse as-is; worth a moment's confirmation before building.
2. Does the edit screen's (`add-event-editing`/`add-event-pictures`) own "leave without saving" path also need to
   proactively delete its session's pending files immediately (as the compose-time form already does), rather than
   only relying on the next startup's sweep to catch them eventually? Both is probably right — immediate cleanup
   where the app already knows a file is abandoned, the sweep as a backstop for whatever that misses (a crash, a
   killed process) — but this changes what "abandoned" needs to be discoverable by `sweep()` versus by an in-app
   cleanup path, worth confirming before writing tasks.

## Risks

- **Sequencing**: cannot be built before `add-event-pictures` exists (its table, its second store instance). Not a
  real risk otherwise — the mechanism is a near-direct copy of already-shipped, tested code.
