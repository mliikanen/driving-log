# Design

## Context

See proposal.md for why. Each of the three capabilities has docs that already describe the same rules, usually in
more detail. A pass over every requirement (backticked names and the key plain-language rules) found the docs
covering all of them except three statements of `test-strategy`'s CI requirement, listed in Decision 2. No
in-flight change branch has a delta on these specs, and nothing outside them refers to them as capabilities (`code-quality`
also names a CI check, which stays).

## Goals / Non-Goals

**Goals:**
- The three specs are gone, and every rule they state is in a doc.
- New proposals don't create tooling specs again.

**Non-Goals:**
- Any change to a rule, a check, a workflow or a script.
- Rewriting the docs beyond adding what is missing.
- Editing archived changes: their deltas against these specs stay as history.

## Decisions

### 1. Retire by REMOVED deltas, not by deleting the files by hand

Every requirement is listed under REMOVED with its Reason and the doc that takes it over (its Migration), and
`.openspec.yaml` declares `retire_capabilities: true`, so `/opsx:archive` deletes the specs. *Alternative:* delete the
three directories in a `chore/` PR. Rejected: `openspec/specs/` changes only through an archived change
(`CLAUDE.md`), and the deltas record where each rule went.

### 2. Where each requirement goes

| Requirement | Doc |
|---|---|
| `test-strategy`: Each kind of test has one job | `docs/test-strategy.md`, "The kinds of test", "Platform split" |
| `test-strategy`: Maestro flows are happy paths | `docs/test-strategy.md`, "Maestro flows are happy paths" |
| `test-strategy`: Flows are organized into manifests with one setup | `docs/test-strategy.md`, "Manifests and setup" |
| `test-strategy`: A test run shows where each tap lands | `docs/test-strategy.md`, "Manifests and setup" |
| `test-strategy`: the two fixture requirements | `docs/test-fixtures.md` |
| `test-strategy`: The final regression run does not include Maestro | `docs/test-strategy.md`, "What is run when"; `CLAUDE.md` |
| `test-strategy`: CI runs the final regression run… | `docs/change-workflow.md`, "Merging"; `CLAUDE.md` |
| `test-strategy`: A pull request cannot be merged until its checks pass | `docs/change-workflow.md`, "Merging" |
| `code-quality`: all five | `docs/code-quality.md`, one section each |
| `app-distribution`: all six | `docs/app-distribution.md`, `docs/distribution.md` |

The three statements no doc makes yet, all from the CI requirement, go into `docs/change-workflow.md`'s "Merging"
paragraph on the required checks:
- CI does not build or test iOS targets.
- The `spec-validation` check reports without waiting for the build.
- A failing `tests-and-build` check names the failing step (shared tests or the debug build).

Anything else the requirement-by-requirement check turns up during apply is added the same way: to the doc in the
table, in that doc's own voice, without SHALL.

### 3. Keep the line from being crossed again

One sentence in `openspec/config.yaml`'s `context` (it is shown to whoever writes a proposal): specs describe the
app's behavior; how the project is built, tested, checked and released is documented in `docs/`, and a change to it
uses `skip_specs: true`. *Alternative:* a rule in `rules.specs`. Rejected: the rule is about what becomes a
capability at all, which the proposal decides before any spec is written.

## Risks / Trade-offs

- [A rule exists only in a spec and is lost] → The requirement-by-requirement check in the tasks, against the table
  above; the REMOVED deltas name the doc for each, so review can follow every one.
- [Docs aren't validated, so they can drift from what CI does] → The same was true of the specs (nothing tested them
  against `pr-check.yml` either); `docs/` is already where people look first.
- [`ratchet-spec-validation` (#19) and this change both touch `docs/test-strategy.md`] → One line; whichever merges
  second merges `main` in and keeps both edits.

## Migration Plan

Archive deletes the three specs. Rollback is reverting the merge.
