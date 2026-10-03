# app-distribution Specification

## Purpose

Defines how a signed, versioned Android build is cut and uploaded to the project's Firebase App Distribution
testers, automatically from CI on every merge to `main` or by hand from a developer's machine, and how its release
notes are derived from the OpenSpec archive history or the merged pull request.

## Requirements

### Requirement: A distributed build is signed with the release keystore
The system SHALL sign every build produced for distribution with the project's release signing config, sourced from
a keystore and credentials kept outside version control. The system SHALL NOT distribute a build signed with the
debug key or left unsigned.

#### Scenario: Signing config present
- **WHEN** the developer runs the distribution build with the release keystore's location and credentials configured locally
- **THEN** the produced build is signed with the release key

#### Scenario: Signing config missing
- **WHEN** the developer runs the distribution build without the release keystore configured locally
- **THEN** the build fails before producing or uploading any artifact, with an error naming the missing configuration

### Requirement: Version name and version code are both derived automatically, never edited by hand
The system SHALL compute the distributed build's `versionCode` automatically from the number of commits reachable
from the built commit, so it is never edited by hand and never reused by mistake for two different commits. The
system SHALL compute the build's `versionName` automatically too, from the same commit count and the built commit's
short hash, so that no version value anywhere is a manually maintained, hand-edited setting.

#### Scenario: No new commit since the last distributed build
- **WHEN** the developer runs the distribution build from the exact commit already distributed (no new commit since then)
- **THEN** the system refuses to proceed, since it would produce the same `versionCode` and `versionName` as the build already distributed

#### Scenario: A new commit exists since the last distributed build
- **WHEN** the developer runs the distribution build from a commit that has at least one commit after the previously distributed one
- **THEN** the build's `versionCode` is strictly greater than the previously distributed build's `versionCode`, and its `versionName` names the new commit

#### Scenario: Version name traces to an exact commit
- **WHEN** a distributed build's `versionName` is read
- **THEN** it identifies both how many commits precede it and the exact commit it was built from

### Requirement: Release notes MUST be generated from the OpenSpec archive history
The system SHALL generate the distributed build's release notes from the names of the changes archived under
`openspec/changes/archive/` since the previously distributed build (found via that build's own version tag), as the
required source of the notes — not an optional convenience the developer may bypass by typing notes from scratch.
The system SHALL let the developer review and edit the generated draft before it is uploaded with the build. The one
exception is when generation has nothing to generate from: when no change has been archived since the previous
distribution, the system SHALL let the developer supply release notes by hand instead, since generation is not
possible, rather than uploading with empty notes.

#### Scenario: Changes were archived since the last distribution
- **WHEN** the developer cuts a new distribution and one or more OpenSpec changes were archived since the previously distributed build
- **THEN** the release notes are generated from those archived changes' names, and the developer reviews the generated draft before it uploads

#### Scenario: A developer cannot skip generation in favor of a blank draft
- **WHEN** the developer cuts a new distribution and one or more OpenSpec changes were archived since the previously distributed build
- **THEN** the notes offered for review already list those changes; the developer edits that draft rather than starting from an empty one

#### Scenario: Nothing was archived since the last distribution
- **WHEN** the developer cuts a new distribution and no OpenSpec change was archived since the previously distributed build
- **THEN** there is nothing to generate, and the developer is prompted to supply release notes by hand before the upload proceeds

#### Scenario: First distribution ever
- **WHEN** the developer cuts the first distribution and no previous distribution tag exists
- **THEN** the generated release notes list every change currently archived under `openspec/changes/archive/`

### Requirement: A signed build uploads to the configured tester group
The system SHALL upload the signed, versioned build to Firebase App Distribution, made available to the testers of
one configured tester group, authenticated with the developer's own local Firebase CLI login (no service-account
credential file is used for this local flow). On a successful upload, the system SHALL mark the built commit with a
version tag recording what was distributed, so a later distribution can find it as the previous one.

#### Scenario: Successful upload
- **WHEN** the developer runs the distribution command while signed in to the Firebase CLI
- **THEN** the build is uploaded and becomes available to the configured tester group, and the built commit is tagged with the distributed version

#### Scenario: Not signed in to the Firebase CLI
- **WHEN** the developer runs the distribution command without an active Firebase CLI login
- **THEN** the upload fails with an error telling the developer to run the Firebase CLI login, and no version tag is created

### Requirement: One-time Firebase project setup is documented, with only the developer's own login left manual
The system SHALL document the one-time Firebase setup a new developer needs before their first distribution. Only
the developer's own interactive Firebase CLI login is a step the build tooling cannot perform on their behalf — it
requires a browser-based sign-in tied to their own Google account. Registering the Android app in the existing
Firebase project (obtaining its Firebase App ID) and creating a tester group both have Firebase CLI equivalents, so
the documentation SHALL cover both the manual login step and the CLI commands for the rest, letting the developer
choose to run those commands themselves or have them run on their behalf once logged in.

#### Scenario: A new developer sets up distribution for the first time
- **WHEN** a developer who has not distributed a build before logs in to the Firebase CLI and runs the documented
  app-registration and tester-group commands (or has them run on their behalf)
- **THEN** they end up with everything the distribution command needs: a registered Android app, its Firebase App ID
  configured, and a tester group to upload to

#### Scenario: Only the login step is irreducibly manual
- **WHEN** a developer sets up distribution for the first time
- **THEN** the documented steps require their own interactive action only for the Firebase CLI login; app
  registration and tester-group creation are documented as CLI commands, not console-only manual steps
