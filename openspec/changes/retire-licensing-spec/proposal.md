## Why

`openspec/config.yaml` and `CLAUDE.md` draw the line: `openspec/specs/` describes the app's behavior, and how the
project is built, tested, checked and released is documented in `docs/`. The `licensing` capability is on the wrong
side of it. It describes the repository, not anything the app does: the `LICENSE`, `NOTICE` and README files, how
contributions are accepted, and what `THIRD_PARTY_NOTICES.md` lists. `retire-tooling-specs` (#20) moved three
capabilities like it into `docs/` and left this one (issue #30).

## What Changes

- **The capability is retired.** Its five requirements are removed, each naming the section of the new doc that covers
  it, and archiving deletes `openspec/specs/licensing/` (`retire_capabilities: true`).
- **A new `docs/licensing.md` states every rule and case**: the license and its files, contributions, the
  third-party notices, license texts that must accompany copies, and downloaded dependencies. Nothing in the spec is
  left out, the scenarios' examples included; the design maps each requirement to its section.
- **The line names licensing.** `openspec/config.yaml`'s rule ("CI, linting, test tooling, signing, distribution")
  gains licensing, and `CLAUDE.md`'s list of docs names `docs/licensing.md` as the one to read before adding
  third-party material.

Out of scope: changing any rule. The license, `NOTICE`, the README's License section and `THIRD_PARTY_NOTICES.md` stay
exactly as they are.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `licensing`: retired; every requirement removed (moved to `docs/licensing.md`).

## Impact

- `openspec/specs/licensing/` deleted by the archive; `docs/licensing.md` added; `openspec/config.yaml` and
  `CLAUDE.md` updated.
- No app code, no CI change, no release notes (merging it publishes no build).
- No in-flight change branch has a delta on `licensing`. Archived changes keep their deltas as history.
