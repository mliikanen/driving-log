# Proposal

> **No app behavior changes.** This is a change to how the project tests itself, so its one spec is the new capability `test-strategy`.
> It builds on the Maestro flows of the archived changes (23 flows, and the `maestro/picture`, `maestro/theme` and `maestro/clock` script groups).

## Why

The Maestro suite has grown to 23 flows plus three script-driven groups and takes about 42 minutes on the emulator. Most of it is not
end-to-end coverage: it re-checks case variations (validation messages, every odometer key sequence, a future date refused, all eight vehicle
types) that the unit tests of the processors and rules already check in milliseconds, several flows repeat the same journey
(three "works offline and survives a restart" flows, five with a rotation step, `15-vehicle-picture` against `picture/*.yaml`, `22` against
`23`), and every flow that needs a photo uploads it just before using it. A slow suite that is run in full at the end of every change is
run less carefully and fails for reasons that are not the change. The project also has no written statement of what each kind of test
is for, so each new flow has been written to check everything about its feature.

## What Changes

- **`docs/test-strategy.md`** documents the strategy: pure unit tests exercise many input/output variations of small pieces of code; Compose
  integration tests (Robolectric) exercise MVI, a screen with its processor; Roborazzi screenshot tests make UI structure change only
  intentionally; Maestro flows test the **happy path of a feature end to end** (several per feature are fine), and case variations and logic
  never belong to them. It also says what is run when (below), and which tiers exist today and which are still to be adopted.
- **Maestro flows are cleaned up.** Flows that only repeat what a unit test (or another flow) already checks are removed, and the remaining
  ones are merged into one happy-path flow per journey. Before an assertion goes, the unit test that owns it is named, and where there is none it
  is added first. The 23 numbered flows become about a dozen, kept in the per-feature manifests below.
- **Manifests.** A *manifest* is a Maestro config file (`maestro/manifests/<area>.yaml`, run with `--config`): it lists the flows of one feature area and their order. Each manifest starts with one setup flow that uploads the test photos its flows use, **once for all of them**, instead of each flow
  uploading its own photos just before it needs them. `maestro/run.sh <manifest>…` runs manifests (and clears the emulator's old photo copies first);
  `maestro/run.sh --all` runs all.
- **What is run when changes.** The final regression run of a change (the last task of `/opsx:apply`, before `/opsx:archive`) is
  `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`. **Maestro is not part of it.** While applying, the
  manifests of the functionality the change touches are run. The whole Maestro suite is reserved for major refactorings (and is then run on request).
  `CLAUDE.md`, `openspec/config.yaml` (task rules) and the two open changes' final tasks are updated to say so.
- The script-driven groups (`picture` with its file checks over `adb`, `theme` with pixel sampling, `clock` with the 12/24-hour setting) become manifests
  of their own with a `run.sh`, since they change device state; they stay out of `run.sh --all`'s plain manifests the way they are out of the plain suite today.

Out of scope: adding Robolectric, Compose UI test or Roborazzi to the build and writing tests for them (the follow-ups `add-compose-integration-tests` and
`add-screenshot-tests`, after which the UI-level flows that remain can be thinned further), moving the unit tests to JUnit Jupiter (see design), running Maestro in CI,
and any change to the app.

## Capabilities

### New Capabilities
- `test-strategy`: which kind of test checks what, how end-to-end flows are organized into manifests and set up once, and what is run when.

### Modified Capabilities
<!-- None: no app behavior changes. -->

## Impact

- `docs/test-strategy.md` (new); `CLAUDE.md` (Commands and the Maestro-checking bullet); `openspec/config.yaml` (task rules and project context).
- `maestro/`: the numbered flows are merged and moved into manifest directories, `subflows/` and `assets/` are kept, `run.sh` is new, `reset-media.sh` is used by it,
  and the `picture`, `theme` and `clock` scripts move into their manifests. Several flows are deleted.
- Unit tests (`shared/src/commonTest`) gain the cases whose only checker was a flow that is deleted (the design lists them).
- Tasks 3.4 of `add-vehicle-color-theme` and 4.1 of `adjust-picture-crop` no longer ask for the whole suite.
- No app code, no dependencies, no data.
