# Tasks

## 1. The license (design.md decisions 1 to 3)

- [x] 1.1 Add `LICENSE` at the repository root with the complete, unmodified Apache License 2.0 text from
      https://www.apache.org/licenses/LICENSE-2.0.txt. Verify: the file matches that text byte for byte
      (`curl -s … | diff - LICENSE`), and after the branch is pushed, GitHub's license API names it
      (`gh api repos/mliikanen/driving-log/license --jq .license.spdx_id` on the branch's ref prints `Apache-2.0`).
      Done: the text from apache.org (sha256 `cfc7749b…`, the canonical file). The license API reads only the default branch (`?ref=` on the branch returns 404), so before merge
      the file was compared with GitHub's own template (`gh api licenses/apache-2.0`): the same text, whitespace aside.
      The API check itself runs after merge.
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

- [x] 3.1 Run the regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `openspec validate --all --strict`. No Maestro manifest: no app behavior or screen changes. Verify: all pass,
      and the PR's checks are green.
      Done: the regression run and `openspec validate --all --strict` (17 items) pass, and PR #7's four checks passed
      on d66317f.

## 4. Review (added during review)

- [x] 4.1 Contributions (Copilot on PR #7): section 5 makes a submission Apache-2.0 unless its submitter states
      otherwise, so the README and the spec now accept contributions only under Apache-2.0, explain that default, and
      say a submission under other terms or marked "Not a Contribution" is not accepted (a new scenario). Verify: the
      README's "License" section and the spec's "Contributions" requirement say the same.
- [x] 4.2 Downloaded dependencies (Copilot on PR #7): `THIRD_PARTY_NOTICES.md` said downloaded dependencies aren't
      listed but kept ONNX Runtime. The complete list is now of checked-in material; a downloaded library is outside it
      and is named only where checked-in material needs it (ONNX Runtime runs the PaddleOCR models). The spec's
      requirement and scenarios say the same. Verify: the notices' intro and the spec agree, and
      `openspec validate --all --strict` passes.
- [x] 4.3 MIT notice (Copilot's second review on PR #7, in its summary): MIT requires its notice to be included with
      copies, so the OpenSpec entry's external link was not enough. Its license text (from the installed
      `@fission-ai/openspec` 1.13.2, the same as upstream's `LICENSE`) is now `.claude/OPENSPEC-LICENSE`, and the entry
      links to it; the PaddleOCR entry links the project's `LICENSE` for its Apache-2.0 text. The spec now requires a
      checked-in copy whenever the license requires its text to accompany copies, with a new scenario. Verify: every
      MIT or Apache entry in `THIRD_PARTY_NOTICES.md` links to a checked-in copy, and `openspec validate --all
      --strict` passes.
- [x] 4.4 The note glyph (Copilot's third review on PR #7, in its summary): the Phosphor entry didn't name
      `note-fill.svg`, which `EventIcons.Note` draws for a log row with a note. It's named now. Verify: each of the 13
      SVGs in `docs/icons/phosphor/` is named in the entry, and each Phosphor glyph the `ui/*Icons.kt` files draw is one
      of them.
- [x] 4.5 The Gradle-generated properties (Copilot's fourth review on PR #7, in its summary): the wrapper entry missed
      `gradle/wrapper/gradle-wrapper.properties`. Copilot found three gaps in turn because task 2.1's listing left out
      text formats, so the whole tree was audited again without that filter (every tracked file, plus a search of the
      code for "ported", "adapted", "copied", copyright and license lines). It found two more: Gradle's
      `gradle/gradle-daemon-jvm.properties` (added to the wrapper entry), and `PpOcr.kt`, a Kotlin port of RapidOCR's
      Apache-2.0 pipeline, which now has its own entry, and whose KDoc says what was changed, as Apache-2.0 section 4(b)
      asks. `Geometry.kt` writes the standard algorithms OpenCV also implements and is the project's own. Verify:
      every tracked file is either listed or the project's own, and `./gradlew codeQuality` passes on the KDoc edit.
