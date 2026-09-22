# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Settle question 1 of the design first, and separately from the rest if useful (mutate in place vs. append
      a correction record) — it decides the shape of everything else. Then settle questions 2-6, update the
      proposal, the spec delta and these tasks, and verify with `openspec validate add-event-editing --strict`

## 2. To be planned after 1.1

- [ ] 2.1 Storage: either an update query (Option A) or a correction record and the derivation logic to fold it in
      (Option B), with unit and migration tests (planned in detail when 1.1 is done)
- [ ] 2.2 An edit entry point from `add-event-details-view`'s screen, reusing or adapting the log event form; tests
      by `docs/test-strategy.md`'s tiers (planned in detail when 1.1 is done)
- [ ] 2.3 A Maestro case correcting a logged event and seeing the correction everywhere it is shown; the final
      regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`)
