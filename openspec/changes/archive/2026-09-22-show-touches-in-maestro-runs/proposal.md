# Proposal

## Why

Debugging a Maestro flow failure today means guessing where a tap actually landed from screenshots and
`uiautomator dump` bounds alone — real, time-consuming work this session hit directly while tracking down a flow
failure that turned out to be a mistargeted tap. Android's own "Show taps" developer setting (a visual marker at
every touch point) would have shown that immediately, and costs nothing to leave on for every test run.

## What Changes

- `maestro/run.sh` enables the device's "Show taps" setting (`adb shell settings put system show_touches 1`) once,
  at the start of a `run.sh` invocation, regardless of how many areas or flows that invocation runs — the same
  "once per invocation, not per flow" shape `reset-media.sh` already has. Guarded the same way the existing `adb`
  detection already is: skipped with a message if `adb` is not available.
- Not turned off afterward. It is a harmless, purely visual overlay on a dedicated test emulator/device, and this
  project already accepts test runs leaving state behind for inspection (see `speed-up-tests-with-db-fixtures`) —
  restoring it would be extra machinery for no benefit here.

## Capabilities

### Modified Capabilities
- `test-strategy`: `run.sh` gains this one setup step, documented alongside its existing ones (removing stale test
  photos, uploading a manifest's own test data).

## Impact

- `maestro/run.sh`: one new command near the existing `adb`/`reset-media.sh` setup block.
- `docs/test-strategy.md`: a one-line mention alongside the existing description of what `run.sh` does before a
  manifest's flows run.
- No app code, no change to what any flow checks or how it is scored.
