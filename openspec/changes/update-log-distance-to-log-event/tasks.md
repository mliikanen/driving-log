# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Settle the open questions of the design with the developer (rename only or also a kind-of-event choice, what the visible words are after the rename, whether the code and the tags are renamed, whether the capability is), update the proposal, the spec delta and these tasks, and verify with `openspec validate update-log-distance-to-log-event --strict`
- [ ] 1.2 Confirm the order: `add-landing-screen` and `add-direct-logging` are archived, so the tile and the selector exist; write the `distance-logging` and `vehicles` deltas against the specs they leave

## 2. To be planned after 1.1

- [ ] 2.1 The visible words: the details action, the form title and the specs' wording; verified by the unit and flow checks that name them (planned in detail when 1.1 is done)
- [ ] 2.2 The mechanical rename of the code, tags and flows, if chosen, in its own commit, with the whole suite passing unchanged apart from names (planned in detail when 1.1 is done)
- [ ] 2.3 The final regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`) and the `distance` and `vehicles` manifests once while applying
