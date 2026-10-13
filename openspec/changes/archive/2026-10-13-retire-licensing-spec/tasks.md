# Tasks

## 1. Move the rules into a doc

- [x] 1.1 Write `docs/licensing.md` with the five sections of design Decision 1, each stating its requirement's rules
      and its scenarios' cases. Verify: every SHALL, SHALL NOT, MAY and scenario of `openspec/specs/licensing/spec.md`
      is found in the doc (listed here, requirement by requirement), and what the doc says about `LICENSE`, `NOTICE`,
      the README and `THIRD_PARTY_NOTICES.md` matches the files as they are.
      - "The license": Apache-2.0; `LICENSE` complete and unmodified, identified as `Apache-2.0` (GitHub's About panel:
        `apache-2.0`, checked with `gh repo view`); `NOTICE` (4(d)) with project, holder and year, pointing to the
        notices; the README section and its three links; everything unlisted is the project's own, media included.
      - "Contributions": only under Apache-2.0, stated in the README; section 5's default; other terms or "Not a
        Contribution" not accepted (not merged); no agreement or sign-off.
      - "Third-party material": the list, its scope (icons, models, fonts, images, vendored code, generated files with
        terms), the four things each entry names, the PaddleOCR change, the entry added in the same change.
      - "License texts that must accompany copies": the checked-in copy next to the material or as `LICENSE`, the entry
        linking to it, an external link not enough; the Phosphor and OpenSpec (MIT) and PaddleOCR (Apache-2.0)
        entries.
      - "Downloaded dependencies": not listed, the MAY exception (ONNX Runtime), Coil without an entry.
- [x] 1.2 Name the doc in `CLAUDE.md`'s list of docs and licensing in `openspec/config.yaml`'s specs-versus-docs rule
      (design Decision 2). Verify: both read naturally in place.
      - `CLAUDE.md`: "`docs/licensing.md` before adding third-party material (an icon, model, font or vendored
        code)". `openspec/config.yaml`: "How the project is built, tested, checked, released and licensed (…,
        licensing) is documented in `docs/`".

## 2. Retire the capability

- [x] 2.1 Check the REMOVED delta and the archive. Verify: `openspec validate retire-licensing-spec --strict` passes;
      an archive in a scratch copy of `openspec/` deletes `openspec/specs/licensing/` and `openspec validate --all
      --strict` passes there.
      - Strict validation of the change passes; the scratch archive retires `licensing` (5 removed, spec deleted) and
        `openspec validate --all --strict` passes there (13 items).

## 3. Regression run

- [x] 3.1 Run the final regression run: `./gradlew :shared:allTests :androidApp:assembleDebug codeQuality` and
      `scripts/validate-specs.sh` (after the archive, since `licensing` goes with it), without Maestro. Verify: both pass.

      - Gradle part passes; `scripts/validate-specs.sh` is run again on the archive commit.