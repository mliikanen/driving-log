# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Settle the open questions of the design with the developer (re-observe vs. handed-in data, ordering
      against `add-event-pictures`/`add-event-editing`, which event kinds get a details screen), update the
      proposal, the spec delta and these tasks, and verify with `openspec validate add-event-details-view --strict`

## 2. To be planned after 1.1

- [ ] 2.1 Screen, processor and nav key for the details screen; `EventRow` gains a tap handler (planned in detail
      when 1.1 is done)
- [ ] 2.2 A Maestro case (in the `distance` manifest or its own) opening a logged event's details and going back;
      the final regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`, `openspec validate --all --strict`)
