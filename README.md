# Driving Log

Kotlin Multiplatform app for tracking trips and mileage, developed spec-first with OpenSpec.

## Prerequisites

- JDK 17+ (21 recommended)
- Android SDK (set `sdk.dir` in `local.properties`)
- Xcode on macOS for iOS builds
- Node.js 20.19+ and OpenSpec: `npm install -g @fission-ai/openspec@latest`

## Getting started with OpenSpec

```sh
openspec init          # choose Claude Code; adds slash commands/skills, keeps openspec/config.yaml
openspec list --specs
```

Then in Claude Code: `/opsx:propose <first-feature>`, review, `/opsx:apply`, `/opsx:archive`.

## Build

```sh
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

## Distributing a build to testers

See `docs/distribution.md`.

## License

Driving Log is licensed under the [Apache License 2.0](LICENSE). See [`NOTICE`](NOTICE) for the copyright notice and
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the third-party material in this repository and its licenses.

Contributions are accepted under the same license: anything you submit in a pull request is licensed under the Apache
License 2.0 (section 5 of the license). There is no contributor agreement to sign and no sign-off line to add.
