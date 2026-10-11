# Proposal

## Why

The project validates its specs with OpenSpec 1.13.2, pinned in CI, while the current release is 1.14.1. 1.14 turns
the hint about requirements over 500 characters into a warning, and `--strict` fails on warnings: 84 of the 138
requirements in `openspec/specs/` are over that length, so the strict validation fails all 17 specs under 1.14.1. The
validation still passes without `--strict` (17 of 17). Rewriting 84 requirements to fit a length hint would churn every
spec for no change in behavior. The developer chose to upgrade and validate without `--strict`.

## What Changes

- Validate the specs with `openspec validate --all` instead of `openspec validate --all --strict` wherever the
  project runs it: the final regression run (`CLAUDE.md`, the tasks rule in `openspec/config.yaml`,
  `docs/test-strategy.md`, `docs/change-workflow.md`), the CI spec-validation job, and the herd's gate in
  `.herd/project.yaml`. Errors still fail it. Warnings, such as a long requirement or an unknown key in a change's
  `.openspec.yaml`, are shown but no longer fail it.
- Install OpenSpec 1.14.1 in CI, and refresh the generated OpenSpec skills and commands in `.claude/` with
  `openspec update` (1.13.1 → 1.14.1 templates).
- Bump the CI job's Node from 22 to 24, the current LTS, and `actions/setup-node` from v7.0.0 to v7.1.0.

Out of scope:

- Shortening the long requirements. Warnings stay visible in the validation output; splitting them can be its own
  change if wanted.
- Other dependency updates. A separate pull request bumps the build's dependencies.
- The herd's evaluation replays archived changes with OpenSpec 1.13.2 and each base's own gate, which is unaffected.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `test-strategy`: CI's spec validation is the validation of the specs, without `--strict`.

## Impact

- `.github/workflows/pr-check.yml`, `CLAUDE.md`, `openspec/config.yaml`, `docs/test-strategy.md`,
  `docs/change-workflow.md`, `.herd/project.yaml`, `.claude/skills/openspec-*`, `.claude/commands/opsx/*`.
- Anyone validating locally needs OpenSpec 1.14.1 (`npm install -g @fission-ai/openspec@1.14.1`).
