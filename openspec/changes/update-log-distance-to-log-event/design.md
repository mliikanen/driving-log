# Design (stub)

## Context

The words "log distance" appear in three layers: what the user sees (the details screen's action "Log distance", the form's title "Log distance", and, with `add-landing-screen`, the tile "Log event" that opens the same form), the code and the tests (`LogDistanceScreen`, `LogDistanceProcessor`, `LogDistanceState`, `LogDistanceNavKey`, about 270 mentions in `shared/src`, the test tags
`log_distance`, `log_way_distance`, `log_way_odometer`, `log_unit_*`, `log_tenths`, `log_date`, `log_zone`, the flows that use them) and what is stored (`vehicle.log_distance_tenths`, created in `2.sqm`; the event type `DISTANCE`). Kide restores a screen by its `serialKey` (`vehicle-log-distance`), so a change of that key would break a restored back stack from the previous version.

## Decisions (proposed, to be confirmed)

1. **Rename what the user sees first, everywhere at once.** The action, the title, the tile and the specs use one word: "Log event".
2. **Do not rename stored names or the serial key.** `log_distance_tenths` and the migrations stay; the `serialKey` `vehicle-log-distance` stays so a saved back stack still restores (a comment says why the name is old).
3. **Rename the code in one mechanical step, separate from the behavior:** classes `LogDistance*` to `LogEvent*` only if the developer wants it (open question 3); a rename with no behavior change is checked by the whole test suite passing unchanged apart from names.

## Open questions

1. Is this only a rename, or does the form also get a **kind of event** choice (distance now; refueling, note later)? Proposed: only the rename now; the choice arrives with the second kind of event.
2. After the rename, does "distance" still appear anywhere the user sees it (the two ways are "Trip distance" and "New odometer": they are ways of entering a distance entry and keep their names)?
3. Rename the code (classes, files, tags) or only the visible text? Renaming the tags forces every Maestro flow to change; keeping `log_distance` as a tag is possible but leaves a permanent mismatch.
4. Rename the capability `distance-logging` to `event-logging`? It renames a spec directory (a REMOVED and ADDED in OpenSpec terms), so it is proposed only when a second kind of event makes the name wrong.

## Risks

- **A mechanical rename across 270 places** hides mistakes: it is done in its own commit, with the suite as the check.
- **Restoring a saved back stack** after the update: the serial key stays (decision 2).
