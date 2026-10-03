# Proposal

## Why

The repository is public on GitHub but has no license. Without one, all rights are reserved by default: nobody may
legally reuse, modify or redistribute the code, and contributions arrive with no stated terms. The project should say
plainly what others may do with it, and do so before more contributors or forks appear.

## What Changes

- License the project under the **Apache License 2.0** (`Apache-2.0`). The design records why it was chosen over MIT
  and the GPL family.
- Add the license text as `LICENSE` at the repository root, unmodified, so GitHub detects it and shows it on the
  repository page.
- Add a short `NOTICE` at the root naming the project and its copyright holder ("Copyright 2026 Mikko Liikanen"), and
  pointing to `THIRD_PARTY_NOTICES.md`.
- Add a "License" section to `README.md` that names the license and links to both files.
- Make `THIRD_PARTY_NOTICES.md` the place for every piece of third-party material checked into the repository
  (it already lists the Phosphor icons, ONNX Runtime and the PaddleOCR models). Check that nothing checked in is
  missing from it.
- State that contributions are accepted under the same license (Apache-2.0 section 5, inbound = outbound). There is
  no contributor license agreement (CLA) and no Developer Certificate of Origin (DCO) sign-off.

Out of scope:

- An in-app "Open source licenses" screen listing the libraries the APK bundles (AndroidX, Kotlin, Kide, Metro,
  SQLDelight, Coil and others). This is app behavior, and its natural home is the settings screen, which is still
  in flight (`change/add-settings-screen`). It is a follow-up change; the design explains why it matters for the
  builds distributed to testers.
- License headers in every source file. Apache-2.0 recommends them but doesn't require them; the root `LICENSE`
  covers the repository.
- Re-licensing or replacing any dependency. Every dependency's license is compatible with Apache-2.0 for an app
  (design.md).
- Trademarks: Apache-2.0 grants no trademark rights (section 6), so the name "Driving Log" and the icon stay
  unlicensed without needing anything else.

## Capabilities

### New Capabilities

- `licensing`: how the project is licensed. The license file, the notice, the README statement, the terms
  contributions are accepted under, and the inventory of third-party material checked into the repository.

### Modified Capabilities

(none)

## Impact

- New files: `LICENSE`, `NOTICE`. Edited: `README.md`, `THIRD_PARTY_NOTICES.md` (only if the audit finds something
  missing).
- No code, build, dependency or app behavior changes. GitHub shows "Apache-2.0" in the repository's About panel.
- The checked-in test photos (`maestro/assets/`) and other media become Apache-2.0 too, which requires that the author
  owns them (design.md, open question).
