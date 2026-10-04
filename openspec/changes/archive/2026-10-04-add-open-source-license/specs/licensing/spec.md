# Spec Delta

## Purpose
Defines how the project is licensed: the license its own work is under, the terms contributions are accepted under,
and how third-party material in the repository is identified with its own license, so anyone can tell what they may
do with each file.

## ADDED Requirements

### Requirement: The project is licensed under Apache-2.0
The project SHALL be licensed under the Apache License, Version 2.0. The repository root SHALL hold the license's
complete, unmodified text in a file named `LICENSE`, which hosting services and license scanners identify as
`Apache-2.0`. The root SHALL also hold a `NOTICE` file, as section 4(d) of the license describes, naming the project
and its copyright holder and pointing to the third-party notices. The README SHALL have a "License" section that
names the license and links to `LICENSE`, `NOTICE` and the third-party notices. Everything in the repository that is
not listed as third-party material is the project's own work and is covered by this license, media included (images,
test photos, fixtures).

#### Scenario: The repository page shows the license
- **WHEN** someone opens the repository on GitHub
- **THEN** its About panel names the license "Apache-2.0", and the `LICENSE` file at the root is the license's
  complete text

#### Scenario: The README states the license
- **WHEN** someone reads the README
- **THEN** a "License" section names the Apache License 2.0 and links to `LICENSE`, `NOTICE` and the third-party
  notices

#### Scenario: The notice names the copyright holder
- **WHEN** someone opens `NOTICE`
- **THEN** it names the project, its copyright holder and year, and points to the third-party notices

#### Scenario: A file of the project's own
- **WHEN** a file in the repository is not listed in the third-party notices (a source file, a doc, a test photo)
- **THEN** it is covered by the Apache License 2.0

### Requirement: Contributions are accepted under the project's license
The project SHALL accept contributions for inclusion only under the Apache License 2.0, the same license it is
under, and SHALL state this in the README's "License" section. Under section 5 of the license, a submission is
under the Apache License 2.0 unless its submitter explicitly states otherwise; a submission offered under other
terms, or marked "Not a Contribution", SHALL NOT be accepted. A contribution SHALL NOT require signing a contributor
license agreement or adding a sign-off line.

#### Scenario: A pull request from someone else
- **WHEN** someone other than the copyright holder opens a pull request and states no other terms
- **THEN** the README's "License" section tells them their contribution is licensed under the Apache License 2.0,
  and nothing asks them to sign an agreement or add a sign-off

#### Scenario: A submission under other terms
- **WHEN** a pull request states that its content is under other terms than the Apache License 2.0, or marks it
  "Not a Contribution"
- **THEN** it is not merged

### Requirement: Third-party material is listed with its license
The project SHALL list in `THIRD_PARTY_NOTICES.md`, at the repository root, every piece of third-party material
checked into the repository or bundled into the app from the repository's own files (icons, models, fonts, images,
vendored code, generated files whose generator imposes terms). Each entry SHALL name what the material is and
where it is, where it comes from, its license, and its copyright line, and SHALL say what was changed when it is not
the original. Where the license requires its text or notice to accompany copies of the material, a copy of that text
SHALL be checked into the repository (next to the material, or the project's own `LICENSE` when it is the same
license), and the entry SHALL link to that copy; a link to an external site alone is not enough. A change that adds third-party material SHALL add its entry in the
same change. Libraries the build downloads as dependencies are not checked into the repository and are outside this
list: they need not be listed, and an entry MAY name one only where checked-in material needs it to be used.

#### Scenario: Material that is already listed
- **WHEN** someone looks up the Phosphor icons or the PaddleOCR models in the third-party notices
- **THEN** each entry names the material, where it is, where it comes from, its license and copyright line, and the
  PaddleOCR entry says how the recognition model was changed

#### Scenario: A change adds a third-party file
- **WHEN** a change checks in an icon, model, font or other file the project did not make
- **THEN** the same change adds its entry to `THIRD_PARTY_NOTICES.md`, and when the license requires its text to
  accompany copies, the same change checks in a copy of that text and the entry links to it

#### Scenario: A license whose notice must accompany copies
- **WHEN** someone opens the entry of checked-in material under MIT, such as the Phosphor icons or the OpenSpec
  skills and commands
- **THEN** the entry links to a copy of the license text checked into the repository

#### Scenario: A downloaded dependency
- **WHEN** the build downloads a library through the version catalog
- **THEN** `THIRD_PARTY_NOTICES.md` need not list it, and lists it only where checked-in material needs it to be
  used, as ONNX Runtime runs the checked-in PaddleOCR models
