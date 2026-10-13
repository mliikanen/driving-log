# Licensing

How the repository is licensed: the license the project's own work is under, the terms contributions are accepted
under, and how third-party material is listed with its own license, so anyone can tell what they may do with each
file. Read this before adding anything the project didn't make (an icon, a model, a font, vendored code). It was the
`licensing` spec until `retire-licensing-spec` (#30) moved it here; the rules are unchanged.

## The license

The project is licensed under the Apache License, Version 2.0.

- **`LICENSE`**, at the repository root, holds the license's complete, unmodified text. GitHub and license scanners
  identify it as `Apache-2.0`, so the repository's About panel names the license "Apache-2.0".
- **`NOTICE`**, at the root (section 4(d) of the license), names the project, its copyright holder and year, and
  points to the third-party notices.
- **The README's "License" section** names the Apache License 2.0 and links to `LICENSE`, `NOTICE` and
  `THIRD_PARTY_NOTICES.md`.

Everything in the repository that `THIRD_PARTY_NOTICES.md` doesn't list is the project's own work and is under the
Apache License 2.0, media included: a source file, a doc, an image, a test photo, a fixture.

## Contributions

Contributions are accepted for inclusion only under the Apache License 2.0, the same license the project is under,
and the README's "License" section says so. Under section 5 of the license, a submission is under the Apache License
2.0 unless its submitter explicitly states otherwise.

- A pull request from someone other than the copyright holder that states no other terms is licensed under the Apache
  License 2.0; the README tells them so.
- A submission offered under other terms, or marked "Not a Contribution", is not accepted and is not merged.
- Contributing never requires signing a contributor license agreement or adding a sign-off line.

## Third-party material

`THIRD_PARTY_NOTICES.md`, at the repository root, lists all third-party material checked into the repository or
bundled into the app from the repository's files: icons, models, fonts, images, vendored code, and generated files
whose generator imposes terms. Each entry names:

- what the material is and where it is;
- where it comes from;
- its license and its copyright line;
- what was changed, when it is not the original (the PaddleOCR entry, for example, says how the recognition model
  was rewritten).

**A change that adds third-party material adds its entry in the same change**, and, when the license requires its text
to accompany copies, checks in that text too (next section).

## License texts that must accompany copies

When the license of third-party material requires its text or notice to accompany copies, a copy of that text is
checked into the repository, next to the material or as the project's own `LICENSE` when it is the same license, and
the material's entry links to that copy. A link to an external site alone is not enough.

- MIT material keeps its license text next to it: the Phosphor icons' entry links to `docs/icons/phosphor/LICENSE`,
  the OpenSpec skills' and commands' to `.claude/OPENSPEC-LICENSE`.
- Material under the Apache License 2.0, such as the PaddleOCR models, links to the project's own `LICENSE`.

## Downloaded dependencies

Libraries the build downloads as dependencies (through `gradle/libs.versions.toml`) are not checked into the
repository and are not listed in `THIRD_PARTY_NOTICES.md`, with one exception: an entry may name one where
checked-in material needs it to be used, as the ONNX Runtime entry does for running the checked-in PaddleOCR models. A
library nothing checked in needs, such as the Coil image loader, has no entry.
