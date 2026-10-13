# Tasks

## 1. Move the rules into a doc

- [ ] 1.1 Write `docs/licensing.md` with the five sections of design Decision 1, each stating its requirement's rules
      and its scenarios' cases. Verify: every SHALL, SHALL NOT, MAY and scenario of `openspec/specs/licensing/spec.md`
      is found in the doc (listed here, requirement by requirement), and what the doc says about `LICENSE`, `NOTICE`,
      the README and `THIRD_PARTY_NOTICES.md` matches the files as they are.
- [ ] 1.2 Name the doc in `CLAUDE.md`'s list of docs and licensing in `openspec/config.yaml`'s specs-versus-docs rule
      (design Decision 2). Verify: both read naturally in place.

## 2. Retire the capability

- [ ] 2.1 Check the REMOVED delta and the archive. Verify: `openspec validate retire-licensing-spec --strict` passes;
      an archive in a scratch copy of `openspec/` deletes `openspec/specs/licensing/` and `openspec validate --all
      --strict` passes there.

## 3. Regression run

- [ ] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh` (after the archive, since `licensing` goes with it), without Maestro. Verify: both pass.
