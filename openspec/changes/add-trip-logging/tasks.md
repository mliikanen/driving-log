# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Settle the open questions of the design with the developer (per vehicle or global, a reference or a time span, several open trips, odometer at start and end, editing and forgotten trips, the tile as a toggle, where a trip's log lives), update the proposal, the spec delta and these tasks, and verify with `openspec validate add-trip-logging --strict`
- [ ] 1.2 Confirm the order: `add-landing-screen` and `add-direct-logging` are archived (the tile and the vehicle selector exist), and write the `app-shell` and `distance-logging` deltas against the specs they leave

## 2. To be planned after 1.1

- [ ] 2.1 Data: the trip table and the entries' reference, migration and repository, with unit and migration tests (planned in detail when 1.1 is done)
- [ ] 2.2 Screens and the tile: start (with the vehicle selector), the open-trip state, end, a trip's log; Home tile toggling; tests by `docs/test-strategy.md`'s tiers (planned in detail when 1.1 is done)
- [ ] 2.3 Flows: a `trip` manifest with the happy path (start, log an entry, end) and the final regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`)
