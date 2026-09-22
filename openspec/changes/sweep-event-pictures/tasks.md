# Tasks (stub)

## 1. Before any code

- [ ] 1.1 Confirm `add-event-pictures` is applied (its `event_picture` table and second `FilePictureStore` instance
      exist). Settle the design's open questions (pending-file age-out threshold, whether the edit/compose screens
      also proactively clean up their own abandoned session files) with the developer, update the proposal, the
      spec delta and these tasks, and verify with `openspec validate sweep-event-pictures --strict`.

## 2. To be planned after 1.1

- [ ] 2.1 `sweepEventPictures(eventRepository, eventPictureStore)`, called alongside the existing vehicle sweep at
      startup; unit tests mirroring `vehicle-picture`'s own sweep tests (planned in detail when 1.1 is done).
- [ ] 2.2 Final regression run (`./gradlew :shared:allTests :androidApp:assembleDebug`,
      `openspec validate --all --strict`). No Maestro case is expected to be needed (the existing vehicle-picture
      sweep has none either — it is a unit-tested background cleanup, not user-visible behavior), but confirm that
      still holds once 1.1 is settled.
