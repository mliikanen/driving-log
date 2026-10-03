# Design

## Context

- Two Gradle modules: `shared` (KMP with `com.android.kotlin.multiplatform.library`, source sets `commonMain`,
  `commonTest`, `androidMain`, `androidHostTest`, `iosMain`, `iosTest`) and `androidApp` (`com.android.application`,
  flavors `production` and `fake`). About 220 Kotlin files, 28k lines. Kotlin 2.4.20, AGP 9.4.1, Gradle 9.7.1 with
  the configuration cache on.
- Generated sources: SQLDelight, Metro, Compose resources, all under `build/`.
- `gradle.properties` has `kotlin.code.style=official`. There's no `.editorconfig` and no checked-in IDE code style.
- Line lengths in the existing code: 1136 lines over 120 characters, 106 over 160, 31 over 180 (longest 636, a
  string). The 120 used in docs isn't what the code follows.
- `productionDebug` needs `androidApp/src/production/google-services.json` (gitignored). `add-ci-workflows` uses
  a placeholder copy in CI. Lint on that variant needs the same.
- No `@Suppress` anywhere in the code today.

## Goals / Non-Goals

**Goals:**
- One command, `./gradlew codeQuality`, with the same result locally, in CI and in a herd worker.
- No churn beyond one mechanical reformat: existing findings are baselined, not fixed in this change.
- Configuration-cache compatible, like the rest of the build.

**Non-Goals:**
- Choosing every detekt rule's threshold from scratch. Defaults hold unless they contradict a project convention.
- Speed tuning beyond Gradle's own caching.

## Decisions

### 1. ktlint through the ktlint Gradle plugin, configured by `.editorconfig`

`org.jlleitschuh.gradle.ktlint`, applied to both modules and the root project (for `*.gradle.kts`). It adds
`ktlintCheck`/`ktlintFormat` per source set, KMP included, and excludes `build/` (generated code). The style
lives in a root `.editorconfig`, which IntelliJ/Android Studio also read:
- `ktlint_code_style = intellij_idea`. It matches `kotlin.code.style=official` as the IDE formats it, so
  "Reformat code" and ktlint agree. `ktlint_official` is stricter than what the IDE produces and would fight it.
- `max_line_length = 160`: the existing code's practice. 120 would mean hand-wrapping 1100+ lines in this change.
  The ~106 longer lines are wrapped by hand (mostly long strings and KDoc).
- `ktlint_function_naming_ignore_when_annotated_with = Composable`: Compose functions are PascalCase.

*Alternatives:* Spotless (also formats other file types; more configuration for the same ktlint underneath);
detekt's `formatting` rule set (wraps ktlint, but then formatting fixes go through detekt's `--auto-correct`, and
the IDE doesn't read its config). The user asked for a Kotlin linter alongside detekt, so the two stay separate
tools, and detekt's `formatting` rule set is **not** enabled, to avoid reporting the same finding twice.

### 2. detekt without type resolution, Compose-aware configuration, one baseline

The detekt Gradle plugin version that supports Kotlin 2.4 (the 2.x line; the 1.23 line is built against Kotlin
2.0 and doesn't parse newer syntax reliably). **Measured (task 1.1):** `dev.detekt` 2.0.0-alpha.6, the newest
release, embeds Kotlin compiler 2.4.10 and parses every source set without errors. It's an alpha; there is no
stable 2.x yet. It's applied **to the root project only**: one `detekt` task whose source is every module's `src/`
(all KMP source sets and both flavors) plus the `*.gradle.kts` scripts. The plugin's per-module default looks only
at `src/main` and `src/test`, and reported `NO-SOURCE` for `shared`. One task also means one baseline. ktlint
(1.8.0 via the 14.2.0 plugin, embedding Kotlin compiler 2.2.21) also parses everything (task 1.1).

The plain tasks have no type resolution. The type-resolution tasks are per compilation, slower, and need the
compiler classpath, which on KMP means configuring every target. The rules lost that way are few and are listed
in `docs/code-quality.md` as a later option.

`config/detekt/detekt.yml` starts from `buildUponDefaultConfig = true`, overriding only what clashes with project
conventions, each with a comment:
- `FunctionNaming`: `ignoreAnnotated: [Composable]`.
- `LongParameterList`: `ignoreAnnotated: [Composable]` (Compose screens take many parameters by design, MVI
  state plus callbacks).
- `MagicNumber`: ignore in Compose code (`dp`/`sp` values, tones) and in tests. Theme and HCT tone tables are
  data, not magic.
- `TooManyFunctions`: off for test classes.
- `MaxLineLength`: off. ktlint's `max-line-length` (160) reports the same lines. At first this was set to 160 too, but
  then both tools reported every long line (task 2.2).
- `UnusedPrivateMember`/`UnusedParameter`: `@Preview` functions are allowed.
- `WildcardImport`: left to ktlint (`no-wildcard-imports`), disabled in detekt to avoid duplicates.

One baseline, `config/detekt/baseline.xml`, generated once (`detektBaseline`) after the reformat.

### 3. Android lint on both debug variants, warnings as errors, baseline

In `androidApp`: `lint { warningsAsErrors = true; abortOnError = true; checkDependencies = false; baseline =
file("lint-baseline.xml") }`. `shared` is linted by its own task (decision 4), so the app's lint covers only the
app's own code. The gate runs `:androidApp:lintFakeDebug` and `:androidApp:lintProductionDebug`:
the flavors differ in their auth code, so linting only one would miss the other's.

`productionDebug` lint needs a `google-services.json`. Locally the developer has the real one. In CI, and in herd
workers, the placeholder from `add-ci-workflows` (`.github/ci/google-services.placeholder.json`) is copied first.
That step is already in the PR check before any Gradle task.

*Alternative:* lint only the `release` variants. Rejected: release needs the keystore (`checkReleaseSigning`),
which CI's PR check deliberately doesn't have.

### 4. `shared` is linted on its own

**Measured (task 1.2):** the app's `checkDependencies` does **not** reach the KMP library's sources. A probe
issue planted in `shared/src/androidMain` was missing from `:androidApp:lintFakeDebug`'s report. The KMP library
plugin also has no lint task for its main component by default; applying AGP's `com.android.lint` plugin to
`shared` adds `:shared:lintAndroidMain`. That task reported the probe, and also analyzes `commonMain`. So `shared`
gets that plugin, `lint {}` options in its `androidLibrary {}` block matching the app's, its own
`shared/lint-baseline.xml`, and `codeQuality` depends on `:shared:lintAndroidMain` too.

**Measured (task 4.1):** once `shared` has `com.android.lint`, the app's `checkDependencies` *does* reach `shared`,
and both app variants reported a planted `shared` finding again, besides `:shared:lintAndroidMain`. So the app sets
`checkDependencies = false`. Each `shared` finding is reported once, by `shared`'s task, and the app baseline holds
only app findings (5 entries, against 11 with `checkDependencies` on).

Both modules disable `NewerVersionAvailable`, `GradleDependency` and `AndroidGradlePluginVersion`, with a comment.
They compare against versions published online, so they'd fail the gate on an upstream release with no code
change (the first run already flagged Compose 1.12.1, Metro 1.4.5 and Gradle 9.8.0). `shared` also disables `OldTargetApi`.
`targetSdk` is the app's setting and the app's lint reports it; in `shared`'s baseline it was recorded against
`$HOME/depot/driving-log/gradle/libs.versions.toml`, a machine-specific path that wouldn't match on CI.

### 5. `codeQuality` aggregates; `check` is left alone

Root `build.gradle.kts` registers `codeQuality`, depending on every project's `ktlintCheck` (root included), the
root `detekt`, and the three lint tasks (`:androidApp:lintFakeDebug`, `:androidApp:lintProductionDebug`,
`:shared:lintAndroidMain`). It's a separate task instead of hooking into `check`, because `check` on a KMP
module also runs every target's tests, including iOS ones that can't run on Linux. The regression run names
`codeQuality` explicitly:
`./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` plus `openspec validate --all --strict`.

### 6. Baselines only shrink: enforced by review, made visible by a check

Spec: "Existing findings are baselined and the baselines only shrink". A small script,
`scripts/check-baselines.sh <base-ref>`, fails if any baseline file has more entries than at `<base-ref>`.
`codeQuality` doesn't run it, since it needs a base ref. The PR check runs it against the PR's base, and
`docs/code-quality.md` tells reviewers (and the herd reviewer, via `CLAUDE.md`) to reject a growing baseline.
Fixing baselined findings in touched files is a review rule: a tool can't reliably tell "touched" from "fixed
nearby".

### 7. The reformat is its own commit, first

`ktlintFormat` over everything, plus hand-wrapping what it can't fix, lands as one commit containing only
formatting, before any baseline is generated. Its SHA goes into a root `.git-blame-ignore-revs`, so `git blame` and
GitHub skip it.

## Risks / Trade-offs

- [The reformat conflicts with every open change branch and the 10 in-flight proposals' future work.] → Proposals
  are only planning artifacts (no code yet), so they're unaffected. A code branch merges `main` and runs
  `ktlintFormat` to resolve conflicts mechanically. Land it when no implementation is in flight.
- [detekt or ktlint lag behind Kotlin 2.4 syntax (a parse failure, not a finding).] → Task 1.1 runs both on the
  whole codebase before anything else. If a tool can't parse the code, the change stops there and the gap is
  recorded rather than worked around.
- [Lint on two variants plus `checkDependencies` is slow (minutes).] → Gradle's cache makes unchanged runs fast.
  The cost lands on every regression run, which is accepted for a gate.
- [Baselines hide real issues indefinitely.] → They shrink as files are touched. `docs/code-quality.md` lists the
  baseline sizes at introduction, so shrinkage is visible.
- [`warningsAsErrors` turns a new lint version's new warnings into failures on an AGP bump.] → Intended. The AGP
  bump's own change fixes them, or disables that lint check in `lint {}` with a reason. It doesn't extend the
  baseline.

## Migration Plan

1. Land the tool configuration and `codeQuality` with the reformat commit first (decision 7), then the baselines,
   then the docs and gate changes.
2. Apply `add-ci-workflows` after this change. Its `code-quality` check runs `codeQuality` and
   `scripts/check-baselines.sh` and blocks merging (its design.md decision 8).

Rollback: remove `codeQuality` from the regression run in `CLAUDE.md`/`openspec/config.yaml`. The tools can stay
configured without gating anything.
