## 1. Validation without --strict

- [x] 1.1 Replace `openspec validate --all --strict` with `openspec validate --all` in `CLAUDE.md`, the tasks rule in
      `openspec/config.yaml`, `docs/test-strategy.md`, `docs/change-workflow.md`, `.herd/project.yaml` and the
      spec-validation step of `.github/workflows/pr-check.yml`. Verify: outside `openspec/changes/archive/`, no file
      names `validate --all --strict`.

## 2. OpenSpec 1.14.1 and Node 24 in CI

- [x] 2.1 In `pr-check.yml`'s spec-validation job, install `@fission-ai/openspec@1.14.1`, use Node 24, and pin
      `actions/setup-node` to v7.1.0 by its commit. Verify: the spec-validation check passes on the pull request.
- [x] 2.2 Run `openspec update` with 1.14.1 to refresh the generated skills and commands in `.claude/`. Verify: each
      generated file says `generatedBy: "1.14.1"` or was rewritten by it, and nothing project-specific was lost.

## 3. Regression run

- [x] 3.1 Run `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and `openspec validate --all` with
      1.14.1, without Maestro (no app behavior or screen changes). Verify: both pass.
      Done: in the worker container (toolchain image, egress enforced) the Gradle run passed, and `openspec validate
      --all` with 1.14.1 passed 18 of 18.
