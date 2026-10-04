# Tasks

## 1. The license (design.md decisions 1 to 3)

- [x] 1.1 Add `LICENSE` at the repository root with the complete, unmodified Apache License 2.0 text from
      https://www.apache.org/licenses/LICENSE-2.0.txt. Verify: the file matches that text byte for byte
      (`curl -s … | diff - LICENSE`), and after the branch is pushed, GitHub's license API names it
      (`gh api repos/mliikanen/driving-log/license --jq .license.spdx_id` on the branch's ref prints `Apache-2.0`).
      Done: the text from apache.org (sha256 `cfc7749b…`, the canonical file). The license API check runs once the branch is pushed.
- [x] 1.2 Add `NOTICE` at the root: "Driving Log", "Copyright 2026 Mikko Liikanen", and one line pointing to
      `THIRD_PARTY_NOTICES.md`. Verify: the file holds those three things and nothing else (decision 2).
      Done: "Driving Log", "Copyright 2026 Mikko Liikanen" and a line pointing to `THIRD_PARTY_NOTICES.md`.
- [x] 1.3 Add a "License" section to `README.md`: the project is under the Apache License 2.0, with links to
      `LICENSE`, `NOTICE` and `THIRD_PARTY_NOTICES.md`; contributions are accepted under the same license, with no
      agreement or sign-off. Verify: the section covers each scenario of the spec's "licensed under Apache-2.0" and
      "Contributions" requirements, and the links resolve on GitHub's rendering of the branch.
      Done: the section names the license, links the three files, and says contributions are under the same license with no agreement or sign-off.

## 2. Third-party material (design.md decision 4)

- [x] 2.1 List the tracked files that are not source or docs (`git ls-files`, excluding the text formats) and check
      each group against `THIRD_PARTY_NOTICES.md`, as decision 4 lists them: the Gradle wrapper, the OpenSpec-generated
      `.claude/` files, the app icon resources and anything else the listing finds. Add an entry in the existing format
      for each third-party item, and give the file a short intro sentence saying what it lists and what it doesn't
      (downloaded dependencies). Verify: every group in the listing is accounted for, either as listed or as the
      project's own, and the result is recorded under this task.
      Done: the listing's groups: `docs/icons/phosphor/` and `androidApp/src/main/assets/ocr/` (including the
      `.characters.txt`) were listed; the Gradle wrapper (Apache-2.0, `gradlew`'s header) and the OpenSpec-generated
      `.claude/` skills and commands (MIT, OpenSpec Contributors) are added; `maestro/assets/` and its fixtures, the
      `.fixture` file, `.gitkeep`s, `.gitattributes` and `.git-blame-ignore-revs` are the project's own. There is no app
      icon yet (`update-app-icon` adds one), and the `androidApp` resources are XML values written for the project.
- [x] 2.2 Ask the developer to confirm that the photos in `maestro/assets/` are their own (design.md, open question).
      List any that aren't with their terms, or replace them. Verify: the developer's answer is recorded under this
      task, and `maestro/assets/README.md` says the photos are the project's own (or points to the notices for
      those that aren't).
      Done: the developer confirmed all photos in `maestro/assets/` are their own (2026-10-04); its README now says so.

## 3. Final regression run

- [ ] 3.1 Run the regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `openspec validate --all --strict`. No Maestro manifest: no app behavior or screen changes. Verify: all pass,
      and the PR's checks are green.
