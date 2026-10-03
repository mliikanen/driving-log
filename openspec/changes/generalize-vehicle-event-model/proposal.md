# Proposal (stub)

> **Stub.** Not a priority right now, and looser than this project's other stubs on purpose: it captures an idea and
> why it's worth taking seriously, not a scoped plan. Filed while the reasoning that motivated it (during
> `add-refueling-logging`'s design) is fresh, for whenever it becomes worth picking up for real.

## Why

`VehicleEvent` is a closed sealed interface today: `InitialOdometer`, `OdometerAnchor`, `DistanceEntry`, and (once
`add-refueling-logging` lands) `Refueling`. Every new kind of loggable fact needs its own case, plus its own copy of
whatever cross-cutting fields already exist (`note` and `photoIds` are declared once on the interface, but each case
implements them separately — `InitialOdometer` always returning null/empty, since it structurally cannot carry
either). Two things already in view strain this:

- The project context anticipates more kinds of loggable facts later ("store also other contextual data to the
  diary, e.g. text, pictures, maintenance done, etc.") — each would need its own new sealed case under today's model.
- `add-refueling-logging` already has a case (optional mileage on a refueling) where **one real-world moment
  naturally carries two facts at once** (filling up and checking the odometer, arguably the normal case for a
  fill-up) — handled today by giving `Refueling` a nullable mileage field, rather than letting an event simply carry
  both an odometer-setting fact and a refueling fact as independent, composable pieces.

## What Changes (sketch, not a plan)

Instead of a closed sum type, an event could be a container that holds zero or more independent "facets" — an
odometer reading, a distance traveled, a refueling, a note, photos, and (eventually) whatever a future diary/
maintenance capability needs — rather than a fixed set of mutually-exclusive shapes. A "Distance" entry becomes a
container with just a distance facet; a fill-up-with-odometer-check becomes a container with both a refueling facet
and an odometer facet, with no new case needed for that combination.

## Capabilities

### Modified Capabilities
- *(not assessed — this would eventually touch `distance-logging`, `vehicle-log`, `event-details`, `event-pictures`,
  `confirm-lower-odometer`, `odometer-ocr-capture`, and `refueling-logging` once it exists: every capability that
  reads or writes a `VehicleEvent` today.)*

## Open questions (genuinely open, not a checklist to resolve now)

1. **What replaces compiler-enforced exhaustiveness?** A sealed interface guarantees, at compile time, that (say)
   an `InitialOdometer` cannot have a note. A container model would need runtime rules instead (which facet
   combinations are valid — can "initial odometer" and "refueling" ever coexist on one event?), losing that
   guarantee. Worth knowing whether that trade is actually worth it before committing to it.
2. **Migration.** Every already-archived capability's behavior (`distance-logging` through `odometer-ocr-capture`)
   is specified against today's closed-case model. Moving to a container model means carrying all of that forward
   without regressing any of it — a bigger undertaking than any single feature proposal so far.
3. **Does this want to happen before or after the diary/maintenance capability that would actually motivate it?**
   Building it speculatively, ahead of a concrete second or third use case beyond `add-refueling-logging`'s own
   mileage-facet blur, risks over-designing for a shape nothing yet needs.

## Impact

Not assessed — this is an idea, not a plan.
