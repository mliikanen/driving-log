# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Settle the open questions of the design with the developer (one picture or several, square crop or
      original aspect ratio, thumbnail or icon on the row, ordering against `add-event-details-view` and
      `add-event-editing`, any relation to the planned OCR pipeline), update the proposal, the spec delta and these
      tasks, and verify with `openspec validate add-event-pictures --strict`

## 2. To be planned after 1.1

- [ ] 2.1 Storage: the picture pipeline reused or adapted for an event picture, migration and repository, with unit
      and migration tests (planned in detail when 1.1 is done)
- [ ] 2.2 The log event form's picture element and the row indicator; tests by `docs/test-strategy.md`'s tiers
      (planned in detail when 1.1 is done)
- [ ] 2.3 A Maestro case or manifest for logging an event with a picture; the final regression run
      (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`)
