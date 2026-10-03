# Proposal

## Why

`add-lint-quality-gates` introduced ktlint, detekt and Android lint with baselines for the findings that predated
them: 157 detekt entries, 5 Android lint entries in the app and 5 in `shared`. Baselines only shrink as files are
touched, so the rest would linger for as long as nobody edits those files. Some entries point at real risks rather
than style: a `remember` returning `Unit` in `App.kt`, generic catches that swallow exceptions in image decoding and
OCR, and the Credential Manager call without an explicit `NoCredentialException` path. Clearing them in one
deliberate change, while the list is short and fresh, is cheaper than leaving them to whichever change next touches
those files.

## What Changes

- Every baselined finding is resolved in one of three ways, chosen per finding and recorded in design.md:
  - **fixed** in code, with no behavior change: named constants for magic numbers, KTX calls, narrower catches,
    helpers extracted from long or complex functions;
  - **suppressed** at the narrowest scope, with a comment saying why (for example, a catch that deliberately turns
    any decoding failure into "no picture");
  - **reconfigured** project-wide in `config/detekt/detekt.yml` or a `lint {}` block, with a comment, where a rule
    contradicts a project convention everywhere (for example, guard-clause returns in MVI processors, or test classes
    being large).
- The three baseline files and their `baseline` settings are **removed**, along with `scripts/check-baselines.sh`.
  From then on a finding is fixed or suppressed, never baselined.
- `docs/code-quality.md` drops its baseline section and says there are none, and why.
- Refactors of complex functions (processors' `map`, `PpOcr`, `Geometry`, screen composables) are covered by the
  existing tests. Where a function has no test that would catch a behavior change, one is added first, in the lowest
  kind of test that can check it.

**Out of scope:**
- Bumping `targetSdk` (the app's `OldTargetApi` finding). It changes platform behavior and needs its own change and
  testing. The rule is reconfigured instead: it fires whenever a newer SDK exists, like the version checks already
  disabled.
- Setting the app icon (`MissingApplicationIcon`). That's `update-app-icon`. Until it's applied, the manifest
  suppresses the finding with a reason pointing at that change.
- Supporting ChromeOS x86_64 in release builds (`ChromeOsAbiSupport`). Release builds are arm64-only on purpose
  (testers' phones); the finding is suppressed with that reason.
- Enabling type-resolution detekt rules, or any new rule set.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `code-quality`: findings are never baselined, and the one-command requirement no longer mentions baselines. The requirement that existing findings are baselined and the
  baselines only shrink is replaced by one saying every finding is fixed, suppressed with a reason, or handled by a
  documented project-wide rule configuration, and that no baseline file exists.
- `test-strategy`: the final regression run's "Static analysis fails" scenario no longer mentions a baseline.

## Impact

- Depends on `add-lint-quality-gates` being archived first: this change modifies the `code-quality` spec it creates.
- Code: about 60 files in `shared` (commonMain, androidMain, iosMain, tests) and a few in `androidApp`, without
  behavior change. The largest groups are OCR (`PpOcr.kt`, `Geometry.kt`, `RgbImage.kt`), color (`Contrast.kt`,
  `LerpHct.kt`, `VehicleTones.kt`), the MVI processors and the larger screens.
- Config: `config/detekt/detekt.yml`, the `lint {}` blocks, root `build.gradle.kts` (baseline settings removed).
- Removed: `config/detekt/baseline.xml`, `androidApp/lint-baseline.xml`, `shared/lint-baseline.xml`,
  `scripts/check-baselines.sh`.
- If `add-ci-workflows` hasn't been applied yet, its `code-quality` job drops the baseline-check step (its tasks
  2.3 and 2.5, and its `test-strategy` delta's "A baseline grows" scenario). If it has, this change edits
  `pr-check.yml`.
- Maestro: the manifests of the screens whose code is refactored (`vehicles`, `distance`, `appearance`, and the
  `picture` group).
