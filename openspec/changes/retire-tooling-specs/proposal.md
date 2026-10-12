# Proposal

## Why

`CLAUDE.md` draws the line: `openspec/specs/` is the app's behavior, and `docs/` is how the project is built, tested
and released. Three capabilities sit on the wrong side of it: `test-strategy` (what each kind of test is for, Maestro
manifests and fixtures, the final regression run, the CI checks and the merge ruleset), `code-quality` (ktlint,
detekt, Android lint, suppressions, warnings) and `app-distribution` (signing, versioning, release notes, Firebase App
Distribution). Each already has a doc that says the same thing in more detail (`docs/test-strategy.md`,
`docs/test-fixtures.md`, `docs/code-quality.md`, `docs/app-distribution.md`, `docs/distribution.md`,
`docs/change-workflow.md`). Keeping both means every tooling change edits a spec and a doc in step, and the spec layer
reads as meta: specs about how specs and tests are run (`ratchet-spec-validation` hit exactly this).

## What Changes

- **The three capabilities are retired.** Every requirement of `test-strategy`, `code-quality` and
  `app-distribution` is removed, each naming the doc that covers it; archiving deletes the three specs
  (`retire_capabilities: true`).
- **Nothing is lost.** Before the specs go, each requirement and its scenarios are checked against its doc, and any
  rule or case the doc does not state yet is written into it (design.md maps each requirement to its doc).
- **The line is written down for proposals.** `openspec/config.yaml`'s project context gains one sentence: specs
  describe the app's behavior, and how the project is built, tested, checked and released belongs in `docs/`, so a
  future change does not grow a tooling spec again.

Out of scope:

- Changing any rule. The CI checks, the merge ruleset, the gates, signing, versioning and release notes work exactly as
  they do now.
- `ratchet-spec-validation` (#19): independent. Whichever merges second updates the one doc line both touch
  (`docs/test-strategy.md`'s "final regression run" row).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `test-strategy`: retired; every requirement removed (moved to `docs/test-strategy.md`, `docs/test-fixtures.md`,
  `docs/change-workflow.md`).
- `code-quality`: retired; every requirement removed (moved to `docs/code-quality.md`).
- `app-distribution`: retired; every requirement removed (moved to `docs/app-distribution.md`,
  `docs/distribution.md`).

## Impact

- `openspec/specs/{test-strategy,code-quality,app-distribution}/` deleted by the archive.
- The docs named above, where a requirement states something they don't yet.
- `openspec/config.yaml` (the project context sentence).
- No app code, no CI behavior change, no release notes.
- Issue #13: 10 of its 86 long requirements go with the specs (`test-strategy` 5, `app-distribution` 4,
  `code-quality` 1).
- No in-flight change branch has a delta on these specs. Archived changes keep their deltas as history.
