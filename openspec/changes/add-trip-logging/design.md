# Design (stub)

## Context

There is no notion of a trip in the code or the specs beyond the sentence in the project context. Events (`vehicle_event`: the initial odometer, odometer anchors, distance entries) are rows of a vehicle's log, each with an instant, a zone and the zone's offset. Totals and the current odometer are derived, never stored. The Home screen (from `add-landing-screen`)
has a disabled "Trip" tile, and `add-direct-logging` adds a vehicle selector to the log form and remembers the last vehicle logged for in `app_state`.

## Decisions (proposed, to be confirmed)

1. **A trip is a record, its totals are derived.** `trip (id, vehicle_id, started_at, start_zone, start_offset, ended_at NULL, end_zone, end_offset)`: an open trip has no end. Its distance is the sum of its entries' distances, computed when shown.
2. **Which entries belong to a trip: by a reference, not by time.** Entries get a nullable `trip_id`, set at save time when a trip of that vehicle is open. Grouping by the time span alone (the trip's start and end) would be derivable but wrong for backdated entries (an entry the user dates last week must not fall into a trip by accident) and for edits of times. *Open question 2 keeps the alternative.*
3. **One open trip per vehicle (proposed), at most one shown on the tile.** The tile toggles for the vehicle last logged for; with several open trips the tile says "End trip" and the selector chooses which. *Open question 3.*
4. **Starting reuses the selector of `add-direct-logging`** (the vehicle chosen in a small sheet or screen, starting on the remembered one) and records the moment as the minute-precision `ZonedMoment` the rest of the app uses.

## Open questions

1. Is a trip per vehicle (a journey with one car) or global (a journey, whichever vehicles)? Proposed: per vehicle.
2. Do entries reference their trip (proposed) or are they grouped by time? What about entries logged after a trip ends but dated inside it?
3. Can several trips be open at once (two vehicles), and what does the tile do then?
4. Does starting a trip record an odometer reading (a "new odometer" entry at start and at end), and are they required or offered?
5. Can a trip's start and end be edited or entered afterwards (forgot to press start), and what if it is left open for days? A reminder? (No notifications yet: the app requests no permission.)
6. The tile: a toggle ("Start trip" / "End trip") or two tiles? Proposed: one toggling tile.
7. Where does a trip's log live: on the vehicle's details screen, a screen of its own, or both?

## Risks

- **Scope**: the model touches the log, the details and the totals; it should be applied only after the questions above are answered and after `add-direct-logging`.
- **Migration**: an existing database has no trips and no `trip_id`; entries stay outside any trip (no backfill).
