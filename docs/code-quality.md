# Code quality: ktlint, detekt and Android lint

Static analysis is part of the final regression run (`docs/test-strategy.md`): `./gradlew codeQuality` must pass
before a change is archived. It was introduced by the `add-lint-quality-gates` change, whose design (archived
under `openspec/changes/archive/`) has the measurements behind the choices below.

## The tools

| Tool | Checks | Covers | Report |
|---|---|---|---|
| **ktlint** (1.8.0, Gradle plugin `org.jlleitschuh.gradle.ktlint`) | Formatting | Every Kotlin source set of `shared` (common, Android, iOS, tests) and `androidApp` (both flavors, tests), and every `*.gradle.kts`. Generated code under `build/` is excluded. | `<module>/build/reports/ktlint/` |
| **detekt** (2.0.0-alpha.6, plugin `dev.detekt`) | Code smells, complexity, potential bugs | One root `detekt` task over every module's `src/` (all source sets) and the `*.gradle.kts` scripts, without type resolution | `build/reports/detekt/` |
| **Android lint** (AGP's) | Android API use, resources, manifest, Compose | `:androidApp:lintFakeDebug` and `:androidApp:lintProductionDebug` (the app's own code, both flavors), `:shared:lintAndroidMain` (`shared`'s Android and common code) | `<module>/build/reports/lint-results-*.html` |

Each finding is reported by one tool only: detekt's `MaxLineLength` and `WildcardImport` are off because ktlint
reports those, detekt's `formatting` rule set (which wraps ktlint) isn't enabled, and the app's lint doesn't
re-analyze `shared` (`checkDependencies = false`), which `shared`'s own lint task covers.

None of them needs macOS: iOS source sets are only read, never compiled.

## Commands

- `./gradlew codeQuality`: all three, as the gate runs them. Add `--continue` to see every tool's findings at once.
  CI runs it on every pull request as the `code-quality` check (`.github/workflows/pr-check.yml`), which blocks
  merging once the ruleset on `main` exists (`docs/change-workflow.md`, Merging).
- `./gradlew ktlintFormat`: fixes formatting in place. Run it first when ktlint fails; what it leaves (for example a
  line too long to wrap automatically) needs a manual fix.
- `./gradlew ktlintCheck`, `./gradlew detekt`, `./gradlew :androidApp:lintFakeDebug` (and the others): one tool.

## Style

The style is in the root `.editorconfig`, which ktlint and IntelliJ/Android Studio both read, so the IDE's
"Reformat code" produces what ktlint accepts:
- `ktlint_code_style = intellij_idea`, which matches `kotlin.code.style=official` as the IDE formats it.
  `ktlint_official` is stricter than the IDE and the two would fight.
- `max_line_length = 160`, the existing code's practice.
- Compose functions are PascalCase (`ktlint_function_naming_ignore_when_annotated_with = Composable`; detekt's
  `FunctionNaming` ignores `@Composable` the same way).

The one-time reformat that introduced ktlint is listed in `.git-blame-ignore-revs`. Run
`git config blame.ignoreRevsFile .git-blame-ignore-revs` once so `git blame` skips it (GitHub's blame view
already does).

detekt's configuration is `config/detekt/detekt.yml`. It builds on detekt's defaults and lists only overrides, each
with its reason: Compose functions may have long parameter lists and inline numbers; tests may have many functions,
literal numbers, large classes and destructured boxes; `@Preview` functions may look unused; guard-clause returns
don't count towards `ReturnCount` (up to three others are allowed); a `when` over intents with one call per branch
isn't counted as complexity; and empty overrides of interface callbacks are allowed.

## Suppressing a finding, or changing a rule

When a finding is wrong for one place, suppress it **at the narrowest scope** (an expression, a declaration, at most
a file), with a comment saying why the rule doesn't apply there:

```kotlin
// PascalCase because SwiftUI calls it like a view controller type (iosApp/README.md); renaming it
// means changing the Xcode shell too, which needs a Mac.
@Suppress("ktlint:standard:function-naming", "FunctionNaming")
fun MainViewController(): UIViewController = ...
```

- ktlint: `@Suppress("ktlint:standard:<rule>")` (or `@file:Suppress(...)` for a file-level rule such as `filename`).
- detekt: `@Suppress("<RuleName>")`.
- Android lint: `@SuppressLint("<IssueId>")` in Kotlin, `tools:ignore="<IssueId>"` in XML.

When a rule is wrong for the whole project (it contradicts a convention everywhere), change it in the tool's
configuration, never by suppressing it file by file: `.editorconfig` for ktlint, `config/detekt/detekt.yml` for
detekt, the `lint {}` block of `androidApp/build.gradle.kts` or `shared/build.gradle.kts` for Android lint.
Always with a comment naming the convention. Both modules' lint already disables `NewerVersionAvailable`,
`GradleDependency` and `AndroidGradlePluginVersion`: they compare against versions published online, so they'd fail
the gate on an upstream release with no code change. The app also disables `OldTargetApi` (it fires whenever a newer
SDK exists; a `targetSdk` bump is a change of its own) and `ChromeOsAbiSupport` (release builds are arm64-only on
purpose), and `shared` disables `OldTargetApi` too (`targetSdk` is the app's setting).

Shared helpers keep suppressions in one place: `undoOnFailure` (`shared/.../util/UndoOnFailure.kt`) is the one
place that catches `Throwable`, to undo work and rethrow, so callers that need cleanup on failure or cancellation use
it instead of their own `catch (Throwable)`.

## No baselines

There are no baselines: every finding is fixed, suppressed in code with a reason, or handled by a commented rule
setting (above). `add-lint-quality-gates` introduced the gate with baselines for the findings that predated it (157
detekt, 5 + 5 Android lint), and `clean-up-lint-baselines` resolved all of them and removed the baselines.

- **Don't add one** (no `baseline` setting, no `detektBaseline`, no `lint-baseline.xml`), not even to land a
  toolchain update. When a detekt, ktlint or Android Gradle Plugin update reports findings in unchanged code, the
  update's own change fixes them, suppresses each with a reason, or reconfigures the rule with a comment.
- A reviewer, human or agent, rejects a change that adds a baseline file or setting.

## Not enabled (yet)

- detekt's type-resolution tasks (`detektMain`-style, per compilation). They're slower, need each KMP target's
  compiler classpath, and tie detekt to the Kotlin compiler version. They add rules such as unreachable-code and
  unnecessary-safe-call checks.
- Compose-specific rule sets (for ktlint or detekt), iOS/Swift linting, and pre-commit hooks.
