# Design

## Context

- The repository is public (made so for the CI change), and has no `LICENSE`.
- `THIRD_PARTY_NOTICES.md` already lists three pieces of third-party material:
  - the Phosphor icons (MIT, originals and license text in `docs/icons/phosphor/`);
  - ONNX Runtime (MIT);
  - the PaddleOCR models in `androidApp/src/main/assets/ocr/` (Apache-2.0, one modified).
- Other checked-in material that may be third-party:
  - the Gradle wrapper (`gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`: Gradle's, Apache-2.0);
  - the OpenSpec-generated skills and commands under `.claude/`;
  - the photos and fixtures in `maestro/assets/`;
  - the app icon resources.
- The app's dependencies (from the version catalog and the POMs in the Gradle cache), by license:
  - Apache-2.0: Kotlin, kotlinx, Compose Multiplatform, AndroidX, Kide, Metro, SQLDelight, Coil and material-color-utilities;
  - MIT: ONNX Runtime;
  - proprietary Google terms: ML Kit text recognition ("ML Kit Terms of Service") and Firebase Auth, Credential Manager
    and Google ID ("Android Software Development Kit License").
- Builds are distributed to testers through Firebase App Distribution, which counts as distributing the binary.

## Goals / Non-Goals

**Goals:**
- A license that GitHub and license scanners detect without guessing (an unmodified, standard text).
- A complete, verifiable list of what in the repository is not the project's own.

**Non-Goals:**
- Legal review. The project picks a standard license and applies it as the license itself describes. A dispute or
  a commercial use case would need its own advice.
- A dependency license checker in CI. The dependencies listed above are few and stable. A checker belongs to the
  in-app notices follow-up, which would generate the list anyway.

## Decisions

### 1. Apache-2.0, not MIT or the GPL

**Choice:** Apache License 2.0.

**Why:**
- **A patent grant.** Section 3 grants a patent license from every contributor and ends it for anyone who sues over
  patents in the work. MIT says nothing about patents. The app's distinguishing feature is camera OCR of odometers
  and fuel pumps, an area with patents.
- **It matches the stack.** Kotlin, AndroidX, Compose, Kide, Metro, SQLDelight and Coil are all Apache-2.0, and the
  PaddleOCR models are too. Anyone reusing this code is already used to the license, and code can move between this
  project and those libraries without a license question.
- **It works with the proprietary Google SDKs.** ML Kit and Firebase are under Google's own terms, and Apache-2.0 has
  no condition on what the work is combined with.
- **Contributions without paperwork.** Section 5 makes a contribution Apache-2.0 unless its contributor says
  otherwise, so contributions need no CLA or sign-off (spec "Contributions are accepted under the project's
  license").
- **It names the trademark question.** Section 6 grants no trademark rights, so the license doesn't give away the
  "Driving Log" name or icon.

**Alternatives considered:**
- **MIT.** Simpler and equally permissive, but it has no patent grant and no contribution clause. Its only advantage
  here is brevity.
- **GPL-3.0 (copyleft).** It keeps derivatives open, but the app links Firebase and ML Kit, whose terms are not
  GPL-compatible. The GPL's system-library exception doesn't clearly cover Google Play services SDKs bundled in the
  APK. Distributing the GPL app would then be in doubt from the first build. The same applies to the AGPL.
- **MPL-2.0 (file-level copyleft).** It is compatible with the proprietary SDKs, but adds per-file obligations a
  personal app gains little from.

### 2. A `NOTICE` file, kept short

Apache-2.0 doesn't require a `NOTICE`. Having one means redistributors must keep it (section 4(d)), which is how the
copyright line and the pointer to the third-party notices travel with forks.

The file holds only the project name, "Copyright 2026 Mikko Liikanen", and one line pointing to
`THIRD_PARTY_NOTICES.md`. Section 4(d) notices may not modify the license, so nothing else goes there.

The third-party list stays in its own file, because it changes with every icon or model added, and `NOTICE` should not.

### 3. No per-file license headers

The license's appendix suggests a header in each file. GitHub, scanners and the license itself rely on the root
`LICENSE`, so headers add noise to a few hundred files, and a lint rule would be needed to keep new files consistent.

Revisit if code from this repository starts being copied file by file into other projects. A header travels with
the file; the root `LICENSE` doesn't.

### 4. Audit what's checked in, by file type

To check the "Third-party material is listed" requirement, list the tracked files that are not source or docs
(`git ls-files`, excluding Kotlin, SQL, Gradle scripts, Markdown, YAML and other text formats). Then check each group:
- `docs/icons/phosphor/`: listed.
- `androidApp/src/main/assets/ocr/`: listed.
- `gradle/wrapper/` with `gradlew` and `gradlew.bat`: Gradle's, Apache-2.0. Add an entry.
- `.claude/` skills and commands generated by OpenSpec: check the OpenSpec license (MIT). Add an entry if its terms
  cover generated output, otherwise note why not.
- `maestro/assets/` and its fixtures: the photos must be the author's own (open question below). Fixtures are
  generated by the project.
- The app icon resources: check whether they are drawn for the project or derived from a template or icon set.

Anything found goes into `THIRD_PARTY_NOTICES.md` in the existing format.

### 5. In-app notices are a follow-up, with a reason

Apache-2.0 section 4 requires that a redistributed binary carry the license and the `NOTICE` files of the Apache
libraries in it (AndroidX ships some). Builds already go to testers, so the APK should list its open-source
components.

This change doesn't do that:
- it needs a screen, and the settings screen it would live on is still in flight;
- it needs a build step that collects the dependencies' licenses (a Gradle plugin such as AboutLibraries or the
  OSS Licenses plugin).

The proposal records the gap as out of scope, so the follow-up is proposed when `add-settings-screen` lands.

## Risks / Trade-offs

- [The test photos include other people's cars and licence plates] → The `maestro/assets/README.md` already says
  they have no location data. Licensing the photos is the author's call, and plates are visible on public streets.
  The open question asks the author to confirm before applying.
- [A choice made now is hard to take back] → Apache-2.0 is permissive: the copyright holder can still re-license
  later (dual-license, or move to another license for new versions). Copies already received under Apache-2.0 keep
  it. Moving to a stricter license later would need every contributor's consent, which is easy while there is one.
- [The APK lacks dependency notices until the follow-up] → The window is short and the audience is a few invited
  testers. Decision 5 names the follow-up.

## Migration Plan

Add the files on the change's branch and merge as usual. Nothing to roll back beyond reverting the commit. Copies
made while the license was in effect keep it.

## Open Questions

- Are all photos in `maestro/assets/` (the car photos, the dashboard and fuel pump photos) the author's own, taken
  by them? If one isn't, task 2.2 lists it with its own terms in `THIRD_PARTY_NOTICES.md` or replaces it. Either way
  the specs and the task list stay the same.
