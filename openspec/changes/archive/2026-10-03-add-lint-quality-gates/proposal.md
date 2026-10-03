# Proposal

## Why

The project has no static analysis: no Kotlin formatter or linter, no detekt, and Android lint is never run. Style
and code smells are caught only if a reviewer notices them. The planned agent pipeline
(`docs/change-workflow.md`) and the CI PR check (`add-ci-workflows`) both gate on "the final regression run".
Adding static analysis to that run means every change, by a human or an agent, is checked the same way, and
reviewers can spend their attention on what tools can't see.

## What Changes

- **ktlint** (via its Gradle plugin) checks the formatting of every Kotlin source and Gradle Kotlin script, and
  `ktlintFormat` fixes what it can. The existing code is reformatted once, in its own commit, so the gate starts
  clean.
- **detekt** checks every Kotlin source set (common, Android, iOS, tests) for code smells and complexity, with a
  project configuration tuned for Compose. Findings in existing code are recorded in a baseline; new findings fail.
- **Android lint** runs on the app's debug variants (both flavors) and on `shared` (its own task, via AGP's
  `com.android.lint` plugin), with warnings treated as errors. Existing findings go into one baseline per module.
- One root Gradle task, `codeQuality`, runs all three. It joins the **final regression run**:
  `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all --strict`.
  `CLAUDE.md`, `openspec/config.yaml`'s task rules, `docs/test-strategy.md` and the draft herd manifest change
  with it.
- The baselines may only shrink: a change that touches a baselined file fixes that file's findings rather than
  extending the baseline. A suppression in code needs a comment saying why.
- `docs/code-quality.md` (new) says how to run each tool, fix or suppress a finding, and update a baseline.
- CI: `add-ci-workflows` runs `codeQuality` and the baseline check as a merge-blocking `code-quality` check on
  every pull request. That change depends on this one, so **this change is applied first**.

**Out of scope:**
- Fixing all existing detekt and Android lint findings. The baselines hold them; they shrink as files are touched.
- Type-resolution detekt rules (`detektMain`-style tasks). They're slower and tie detekt to the Kotlin compiler
  version; revisit once the plain run is in place.
- Compose-specific third-party rule sets, iOS/Swift linting, and pre-commit hooks.

## Capabilities

### New Capabilities
- `code-quality`: which static analysis the project enforces, what it covers, how existing findings are
  baselined and only shrink, and how a finding may be suppressed.

### Modified Capabilities
- `test-strategy`: the final regression run also includes the static analysis gate.

## Impact

- Build: `gradle/libs.versions.toml` (ktlint, detekt and `com.android.lint` plugins), root `build.gradle.kts`
  (`codeQuality` task, ktlint everywhere, one root detekt task), `androidApp/build.gradle.kts` and
  `shared/build.gradle.kts` (lint options; design.md decisions 3 and 4).
- New files: `.editorconfig`, `config/detekt/detekt.yml`, `config/detekt/baseline.xml`,
  `androidApp/lint-baseline.xml` (and one for `shared` if it gets lint), `docs/code-quality.md`.
- A one-time reformat touching most Kotlin files. Every open change branch will conflict with it, so it goes in as
  a separate commit that is easy to merge past (design.md, Migration Plan).
- Every developer and agent run of the regression gate gets slower by the time the three tools take.
