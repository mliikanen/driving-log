# Proposal

## Why

`upgrade-openspec-cli` dropped `--strict` because 84 requirements in the main specs were over OpenSpec's
500-character limit (86 of 143 now). Without `--strict`, nothing stops new long requirements: the backlog grows with
every change (`atomic-log-events` added two, and `add-vehicle-hiding` adds two more). Other warnings, such as an unknown
key in a change's `.openspec.yaml`, stopped failing the check too. OpenSpec has no baseline or allowlist, and nothing
says it will get one, so the project has to stop the growth itself before issue #13 can work the backlog down.

## What Changes

- **The spec validation becomes strict for changes, lenient only for the main specs.** It runs
  `openspec validate --changes --strict` (every warning in an active change fails, including a new requirement over
  500 characters) and `openspec validate --specs` (errors in the main specs fail; their existing length warnings are
  shown but pass).
- **A long requirement can only get shorter.** OpenSpec does not length-check a MODIFIED requirement. A new script
  fails the validation when a change's MODIFIED requirement is over 500 characters and longer than the same
  requirement in the main spec. Unchanged text (a change that only adds a scenario) still passes.
- **The rules live in the docs, not in a spec.** `docs/test-strategy.md` says what the validation checks and why, and
  the scripts' tests hold the exact cases. How long a requirement may be is about how the project is built, which
  `CLAUDE.md` puts in `docs/`, not `openspec/specs/`.
- **One command runs the whole validation:** `scripts/validate-specs.sh`, used by CI's `spec-validation` job, the
  herd's gate, and everywhere the final regression run is written down (`CLAUDE.md`, the tasks rule in
  `openspec/config.yaml`, `docs/test-strategy.md`, `docs/change-workflow.md`). When #13 is done, its last change
  replaces the script's body with `openspec validate --all --strict`, and nothing else has to change.

Out of scope:

- Shortening the existing long requirements, and full `--strict` on the main specs: issue #13, in batches by
  capability after this change.
- In-flight change branches. Each meets the new check when it is next worked on (for example, `add-vehicle-hiding`
  splits its two new long requirements before it can merge).

## Capabilities

None: `skip_specs: true`. The app's behavior does not change, and the validation rules are tooling, documented in
`docs/test-strategy.md`. One existing scenario goes slightly out of date: `test-strategy`'s "A spec is invalid" names
`openspec validate --all`, while CI will run `scripts/validate-specs.sh`, which still fails on every error that
scenario covers. #13's last batch brings `openspec validate --all --strict` back, which makes it literal again.

## Impact

- New `scripts/validate-specs.sh` and `scripts/check-requirement-length.mjs` (Node, no dependencies, reads
  `openspec show --json`).
- `.github/workflows/pr-check.yml` (`spec-validation` job), `.herd/project.yaml` (gate), `CLAUDE.md`,
  `openspec/config.yaml`, `docs/test-strategy.md`, `docs/change-workflow.md`.
- No app code, so no tester-visible change: the PR has no `## Release notes` section.
- Every in-flight change branch is validated more strictly from its next push on. Of the branches with spec deltas,
  `add-vehicle-hiding` is known to fail (two new requirements over 500 characters).
