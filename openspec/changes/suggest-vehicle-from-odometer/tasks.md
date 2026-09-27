# Tasks

## 1. Resolve the open design questions first

- [ ] 1.1 Answer design.md's open questions (matching rule, ambiguous-match handling, presentation, whether trip
      candidates are included at all) — ideally informed by real multi-vehicle usage once `add-odometer-ocr-capture`
      is built and in use, not guessed at in the abstract. Update design.md and the spec delta's scenarios to match
      before starting section 2.
- [ ] 1.2 Confirm `add-odometer-ocr-capture` is implemented and archived — this change has nothing to build against
      until candidate detection and classification exist.

## 2. Implementation (not yet scoped in detail — depends on 1.1's answers)

- [ ] 2.1 Wire the vehicle-matching suggestion into the log event form's vehicle selector, per whatever section 1
      resolved.
- [ ] 2.2 Unit-test the matching rule and the ambiguous-match and no-match paths from the finalized spec scenarios.

## 3. Verification

- [ ] 3.1 Run `maestro/run.sh distance` (or whichever manifest covers the Home-route log event form once this is
      built) and confirm it passes.
- [ ] 3.2 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`; confirm
      both pass before archiving.
