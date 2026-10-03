# Tasks

## 1. Tool versions and feasibility (nothing after this starts until both tools parse the code)

- [x] 1.1 Add the ktlint Gradle plugin and the detekt Gradle plugin to `gradle/libs.versions.toml`, at versions whose
      compatibility tables cover Kotlin 2.4.20 and Gradle 9.7.1 (design.md decisions 1 and 2), and apply both to the
      root project, `shared` and `androidApp` with no rules configured yet. Verify: `./gradlew ktlintCheck detekt
      --continue` finishes with findings but no parse/compile errors from either tool on any source set (common,
      Android, iOS, tests) and no configuration-cache problems. If a tool can't parse the code, stop and record the
      gap in design.md.
- [x] 1.2 Find out how Android lint reaches `shared` (design.md decision 4): run `:androidApp:lintFakeDebug` with
      `checkDependencies = true` and check the report includes findings located in `shared/src/`. If it doesn't,
      enable lint in `shared`'s `androidLibrary {}` block instead. Record the outcome in design.md. Verify: the
      lint HTML report lists at least one `shared/src/` location, or `shared` has its own lint task that runs.

## 2. Configuration

- [x] 2.1 Add the root `.editorconfig` (`ktlint_code_style = intellij_idea`, `max_line_length = 160`,
      `ktlint_function_naming_ignore_when_annotated_with = Composable`) and exclude `build/` from ktlint. Verify: a
      scratch file with a 170-character line and a PascalCase non-Composable function fails `ktlintCheck`, and a
      PascalCase `@Composable` function doesn't. Remove the scratch file afterwards.
- [x] 2.2 Add `config/detekt/detekt.yml` (`buildUponDefaultConfig = true`) with the overrides from design.md
      decision 2, each with a comment giving its reason, `formatting` not enabled, and `build/` excluded. Verify:
      `./gradlew detekt` uses the file (a deliberately broken key in it makes the run fail; revert), and no detekt
      finding duplicates a ktlint one (`MaxLineLength` and `WildcardImport` off).
- [x] 2.3 Configure lint in `androidApp` (`warningsAsErrors`, `abortOnError`, `checkDependencies`,
      `baseline = file("lint-baseline.xml")`), and register the root `codeQuality` task depending on every project's
      `ktlintCheck` and the root `detekt`, plus `:androidApp:lintFakeDebug`, `:androidApp:lintProductionDebug` and
      `:shared:lintAndroidMain` (1.2 required it). Verify: `./gradlew codeQuality --dry-run` lists all of those tasks.

## 3. Reformat and baselines

- [x] 3.1 Run `./gradlew ktlintFormat`, then wrap by hand what it can't fix (the ~106 lines over 160 characters),
      and commit only that as "Reformat with ktlint" (design.md decision 7). Add the commit's SHA to a new
      `.git-blame-ignore-revs`. Verify: `./gradlew ktlintCheck` passes, `./gradlew :shared:allTests
      :androidApp:assembleDebug` still passes, and the commit contains no non-formatting edits (review its diff
      with `git diff --ignore-all-space --stat` for anything beyond wrapping).
- [x] 3.2 Generate `config/detekt/baseline.xml` (`./gradlew detektBaseline`) and `androidApp/lint-baseline.xml`
      (run lint once with the baseline file absent; AGP writes it). Record each baseline's entry count in
      `docs/code-quality.md` (task 5.1). Verify: `./gradlew codeQuality` passes.
- [x] 3.3 Add `scripts/check-baselines.sh <base-ref>`: fails, naming the file, when any baseline has more entries
      than at `<base-ref>` (design.md decision 6). Verify: it passes against `HEAD`, and fails after adding a dummy
      entry to a working copy of `config/detekt/baseline.xml` (then revert).

## 4. Gate checks the spec asks for

- [x] 4.1 Check the code-quality spec's failure scenarios against the real gate: in a scratch commit, add a
      misformatted line, a detekt finding (e.g. an empty `catch`), and an Android lint warning (e.g. a hard-coded
      string in a layout resource or an `@SuppressLint`-worthy API call in `androidMain`). Verify: `./gradlew
      codeQuality --continue` fails and names each one's file, line and rule; `ktlintFormat` fixes the formatting
      one. Drop the scratch commit.

## 5. Docs, project rules and CI

- [x] 5.1 Write `docs/code-quality.md`: the three tools and what each covers, `codeQuality` and `ktlintFormat`,
      the style decisions (code style, 160 columns, Compose naming) and why, how to suppress (narrowest scope plus
      a reason comment) or reconfigure a rule (in the config, with a comment), the baseline-only-shrinks rule and
      `scripts/check-baselines.sh`, baseline sizes at introduction, and the type-resolution detekt rules left for
      later. Add it to `CLAUDE.md`'s `docs/` list. Verify: each spec requirement in `code-quality` has a section
      that covers it.
- [x] 5.2 Change the final regression run to `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `openspec validate --all --strict` in `CLAUDE.md`, `openspec/config.yaml` (the tasks rule and the context's
      test-strategy note), `docs/test-strategy.md`'s table, and `.herd/project.yaml`'s `gate`. Verify:
      `grep -rn "allTests :androidApp:assembleDebug"` over those files shows `codeQuality` in each.
- [x] 5.3 Check that `add-ci-workflows` is still unapplied (it depends on this change and adds the merge-blocking
      `code-quality` check). If it was applied first anyway, add its `code-quality` job here as its task 2.3
      describes. Verify: `openspec list` shows `add-ci-workflows` unarchived, or `pr-check.yml` has the
      `code-quality` job.

## 6. Final regression run

- [x] 6.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all
      --strict` (no Maestro: the reformat changes no behavior, and no screen or flow changes). Verify: all pass.
