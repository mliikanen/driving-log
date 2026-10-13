# Design

## Context

`retire-tooling-specs` (`openspec/changes/archive/2026-10-12-retire-tooling-specs/`) retired three capabilities the
same way: every requirement REMOVED with a Reason and a Migration naming the doc, `retire_capabilities: true`, and each
requirement checked against its doc first. That change merged without its archive and needed a second pull request
(#21) to delete the specs; this one archives in its own pull request, as the workflow says.

## Goals / Non-Goals

**Goals:** nothing the spec says is lost; it is said once, in `docs/licensing.md`.

**Non-Goals:** changing any rule, or the `LICENSE`, `NOTICE`, README and `THIRD_PARTY_NOTICES.md` files.

## Decisions

### 1. One new doc, sections mapped to requirements

No doc covers licensing today, so `docs/licensing.md` is new. Each requirement becomes a section, its rules as prose
and its scenarios' cases (the GitHub About panel, the Phosphor, PaddleOCR, OpenSpec and ONNX Runtime entries, Coil as
an unlisted dependency) as examples:

| Requirement | Section |
|---|---|
| The project is licensed under Apache-2.0 | The license |
| Contributions are accepted under the project's license | Contributions |
| Third-party material is listed with its license | Third-party material |
| A license that must accompany copies is checked in | License texts that must accompany copies |
| Downloaded dependencies are outside the list | Downloaded dependencies |

*Alternative:* put it all in the README's License section. Rejected: the README addresses users and contributors;
the checklist for adding third-party material is for whoever changes the repository, like the other docs.

### 2. Point to the doc where it's needed

`CLAUDE.md` lists which doc to read before touching an area; it gains "`docs/licensing.md` before adding third-party
material (an icon, model, font, vendored code)". `openspec/config.yaml`'s specs-versus-docs rule names licensing, so a
later change doesn't grow a repository spec again.

## Risks / Trade-offs

- **A rule only the spec stated is lost.** → The task that writes the doc checks every SHALL, SHALL NOT, MAY and
  scenario against it, and lists the mapping in the task.
