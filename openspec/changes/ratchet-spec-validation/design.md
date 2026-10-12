# Design

## Context

See proposal.md for why. The relevant facts, checked against OpenSpec 1.14.1 (the version CI pins):

- Strict mode fails on any warning (`createReport`: `valid = errors === 0 && warnings === 0`). There is no baseline,
  ignore list or per-requirement suppression, and no configurable limit (`MAX_REQUIREMENT_TEXT_LENGTH = 500` is a
  constant).
- In a change's deltas, only ADDED requirements get the length check. MODIFIED ones are exempt by design (the source
  comment: a MODIFIED block is the existing requirement, kept whole).
- `openspec validate --changes --strict` with no active change prints "No items found to validate." and exits 0, so it
  is harmless on `main`, where `openspec/changes/` holds only `archive/`.
- `openspec validate --specs` passes today (17 of 17) with 86 length warnings.
- `openspec show <change> --json --deltas-only` gives each delta's `spec`, `operation` and `requirement.text`;
  `openspec show <spec> --type spec --json` gives each main requirement's `name` and `text`. Both are OpenSpec's own
  parser, so `text` is exactly what the 500-character check measures.
- Node is on every machine that runs the validation: the OpenSpec CLI is a Node program (CI's job installs Node 24, the
  herd's role layer installs the CLI).

## Goals / Non-Goals

**Goals:**
- No new requirement over 500 characters, and no long requirement getting longer, from this change on.
- Every other warning in a change fails again.
- One command for the whole spec validation, so the later switch to full `--strict` edits one file.

**Non-Goals:**
- Shortening anything (issue #13).
- Checking in-flight branches now. They meet the check on their next push.

## Decisions

### 1. Two OpenSpec runs, not one

`openspec validate --changes --strict` and `openspec validate --specs` together cover what `openspec validate --all`
did, with strictness only where the backlog isn't. *Alternative:* keep `--all` and grep its output for warnings that
are not length warnings on main specs. Rejected: it parses human-readable output that changes between OpenSpec
versions, and it would still need a separate rule for new long requirements.

### 2. The ratchet compares against the main spec, by requirement name

`scripts/check-requirement-length.mjs`:
1. lists the active changes (`openspec list --json`);
2. for each, reads `openspec show <change> --json --deltas-only` and keeps the MODIFIED deltas;
3. for each, reads the main spec once (`openspec show <spec> --type spec --json`) and finds the requirement of the
   same name (whitespace-normalized, the way OpenSpec matches names);
4. fails when the new text is over 500 characters and longer than the main one, printing the change, spec,
   requirement and both lengths.

"Longer than before" rather than "over 500" is what lets #13's batches and ordinary changes touch a long requirement
(add a scenario, fix a word) without being forced to split it first. A MODIFIED requirement with no main counterpart
is already an OpenSpec error, so the script skips it rather than reporting it twice.

*Alternative:* re-implement OpenSpec's requirement-text extraction. Rejected: the JSON output is the same parser, and
a copy would drift. *Alternative:* a Gradle task. Rejected: the spec-validation job deliberately needs only Node and
the CLI, so it reports in under a minute.

### 3. `scripts/validate-specs.sh` is the only entry point

```sh
openspec validate --changes --strict --no-interactive
openspec validate --specs --no-interactive
node scripts/check-requirement-length.mjs
```

Each step runs even if an earlier one failed, and the script fails if any did, so one run shows every problem. CI,
the herd's gate and the docs call the script instead of naming OpenSpec commands. #13's last change replaces the body
with `openspec validate --all --strict` and deletes the `.mjs`.

## Risks / Trade-offs

- [In-flight branches start failing on their next push] → Intended. `add-vehicle-hiding` splits its two new long
  requirements. The others either have no spec deltas or none over the limit; they'll find out on their next CI run.
- [Main-spec warnings stay invisible as failures until #13 is done] → They are still printed, and the ratchet stops
  them growing. #13 brings full `--strict` back.
- [`openspec show` JSON shape changes in a later OpenSpec] → The script fails loudly when an expected field is missing
  rather than passing silently, and the CLI version is pinned in CI and bumped deliberately.

### 4. No spec delta (`skip_specs: true`)

The rules are tooling, so they are written in `docs/test-strategy.md` and tested by `node --test 'scripts/*.test.mjs'`, not
specified in `openspec/specs/`. *Alternative:* add them to the `test-strategy` capability, which already specifies CI.
Rejected: a spec about how specs are validated is a layer of meta nobody reads as app behavior, and `CLAUDE.md` puts
how the project is built in `docs/`. The out-of-date scenario this leaves is noted in the proposal.

## Migration Plan

None: tooling only. Rollback is reverting the PR.
