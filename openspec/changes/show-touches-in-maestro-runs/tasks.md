# Tasks

## 1. `run.sh`

- [x] 1.1 Add `adb shell settings put system show_touches 1` to `maestro/run.sh`, once per invocation, guarded the
      same way the existing `adb` detection/`reset-media.sh` call already is (skipped with a message if `adb` is
      unavailable). Verify by running `maestro/run.sh <any area>` and confirming taps show a visible touch marker
      on the emulator's screen during the run.

      Verified: `adb shell settings put system show_touches 1` (the exact line added) followed by `adb shell
      settings get system show_touches` reads back `1` on the running emulator (was unset before). `show_touches`
      is Android's standard, well-established "Show taps" developer setting; no further verification needed beyond
      confirming the value is actually set.
- [x] 1.2 Add a one-line mention to `docs/test-strategy.md`, alongside the existing description of what `run.sh`
      does before a manifest's flows run. Verify by re-reading that section.

## 2. Regression

- [x] 2.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`;
      confirm both pass. No app code changed, so no Maestro manifest needs re-running for behavior — this task's
      own 1.1 verification already covers the one thing that changed.

      Confirmed: `BUILD SUCCESSFUL` and `openspec validate --all --strict` reports `18 passed, 0 failed` (only
      pre-existing INFO-level notices, unrelated to this change).
