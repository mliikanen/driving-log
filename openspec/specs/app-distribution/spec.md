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

For a CI distribution, the review happens on the pull request before it is merged: the pull request SHALL
show, in an informational check that is not required for merging, the draft that merging it would publish, kept
current when the pull request's description is edited. A CI distribution's notes SHALL cover every pull request merged
since the previously distributed build: each one whose description has a `## Release notes` section contributes that
section's text, which replaces the generated names of the changes that pull request archived, so a reviewed or edited
version replaces the generated draft and a pull request that archives nothing can still supply notes. Every other
change archived since the previous distribution, from a pull request without a section or from a commit that reached
`main` without a pull request, SHALL be listed by name as in the generated draft.

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

#### Scenario: The draft is shown on the pull request
- **WHEN** a pull request targeting `main` archives one or more changes
- **THEN** its release notes preview shows the release notes that merging it would publish, generated from the changes archived since the previous distribution

#### Scenario: The description is edited
- **WHEN** the description of an open pull request is edited, for example to add a `## Release notes` section
- **THEN** the preview is updated to the new notes without the tests and build running again

#### Scenario: The pull request supplies the notes
- **WHEN** a merged pull request's description has a `## Release notes` section
- **THEN** the CI distribution uploads that section's text as the release notes, instead of the generated names of the changes it archived

#### Scenario: One distribution covers several pull requests
- **WHEN** two pull requests are merged before either is distributed (one release replaced the other while waiting), one with a `## Release notes` section and one without
- **THEN** the distribution's notes have the first one's section and the names of the changes the second one archived

#### Scenario: A direct push to main
- **WHEN** a commit that archives a change reaches `main` without a pull request
- **THEN** the CI distribution uses the generated draft as the release notes

### Requirement: A signed build uploads to the configured tester group
The system SHALL upload the signed, versioned build to Firebase App Distribution, made available to the testers of
one configured tester group. A distribution from the developer's machine SHALL be authenticated with the
developer's own local Firebase CLI login (no service-account credential file is used for this local flow); a CI
distribution SHALL be authenticated as a dedicated service account that may only distribute builds, never with the
developer's personal login. On a
successful upload, the system SHALL mark the built commit with a version tag recording what was distributed and
push that tag to the remote repository, so a later distribution, from either place, can find it as the previous
one. Before computing what is new, a distribution SHALL fetch the remote's version tags, so a local and a CI
distribution agree on which build was distributed last.

#### Scenario: Successful upload
- **WHEN** the developer runs the distribution command while signed in to the Firebase CLI
- **THEN** the build is uploaded and becomes available to the configured tester group, and the built commit is tagged with the distributed version and the tag is pushed to the remote

#### Scenario: Not signed in to the Firebase CLI
- **WHEN** the developer runs the distribution command without an active Firebase CLI login
- **THEN** the upload fails with an error telling the developer to run the Firebase CLI login, and no version tag is created

#### Scenario: Successful upload from CI
- **WHEN** a CI distribution uploads a build
- **THEN** the build becomes available to the configured tester group, and the built commit's version tag is pushed to the remote

#### Scenario: A local distribution after a CI one
- **WHEN** the developer runs the distribution command after CI has distributed a build the developer's clone has no tag for yet
- **THEN** the command fetches that tag first, so its release notes cover only what CI did not distribute, and it refuses if `HEAD` is the commit CI already distributed

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

### Requirement: Every push to main publishes from CI when there is something to release
The system SHALL run the distribution automatically in CI for every push to `main`, and SHALL let it be started by
hand as well. A CI distribution SHALL build the signed, versioned `production` release from the newest commit on
`main` whose CI regression run has passed, whatever started it, and upload it to the configured tester group, with no
developer machine involved. When the
release notes would be empty (no change archived since the previous distribution, and no release notes supplied
with any pull request merged since), the system SHALL publish nothing and create no tag, and SHALL say so in the run's
summary. A CI distribution SHALL NOT publish a commit whose CI regression run hasn't passed, including one started by
hand. Two CI distributions SHALL NOT run at the same time; a later one SHALL wait for the earlier one to finish. A CI
distribution SHALL NOT publish a commit that is already part of a distributed build, its own or a later one's.
A CI distribution SHALL get the release keystore, its credentials and its upload identity only from the
repository's CI configuration, available to runs on `main` and to no pull request. When one of them is missing, it
SHALL fail before uploading anything or creating a tag, naming what is missing.

#### Scenario: A change is merged
- **WHEN** a pull request that archives an OpenSpec change is merged into `main`
- **THEN** CI builds the signed release from the merge commit, uploads it to the tester group, and tags that commit with the distributed version

#### Scenario: Nothing to release
- **WHEN** a commit reaches `main` with no change archived since the previous distribution and no release notes supplied with its pull request (for example, a proposal or a documentation edit)
- **THEN** CI publishes nothing, creates no tag, and the run's summary says there was nothing to release

#### Scenario: Two merges in quick succession
- **WHEN** a second commit reaches `main` while the first one's distribution is still running
- **THEN** the second distribution starts only after the first has finished, and its notes cover only what the first did not distribute

#### Scenario: Started by hand before the newest commit's checks pass
- **WHEN** a distribution is started by hand while the newest commit on `main` has a pending or failed regression run
- **THEN** it publishes the newest commit whose regression run passed, if that one isn't distributed yet, and otherwise nothing

#### Scenario: Checks finish out of order
- **WHEN** commits B and then C reach `main`, and C's regression run passes before B's
- **THEN** C is distributed (with B's changes, which it contains), and B's later passing run doesn't publish the older B

#### Scenario: An older commit after a newer distribution
- **WHEN** a distribution runs again for a commit after a later commit has been distributed
- **THEN** it publishes nothing, since that build would be older than what testers already have

#### Scenario: A push whose checks fail
- **WHEN** a commit reaches `main` and the CI regression run fails on it
- **THEN** CI does not distribute it

#### Scenario: A release secret is missing
- **WHEN** a CI distribution runs and the release keystore, its password or the upload identity is not configured
- **THEN** it fails before building or uploading, the failure names what is missing, and no tag is created

#### Scenario: A pull request cannot reach the release secrets
- **WHEN** a pull request's checks run, including from a branch that changes the workflow files
- **THEN** the release keystore, its password and the upload identity are not available to them
