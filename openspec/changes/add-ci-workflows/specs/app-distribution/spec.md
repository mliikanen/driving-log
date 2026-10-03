# Spec Delta

## ADDED Requirements

### Requirement: Every push to main publishes from CI when there is something to release
The system SHALL run the distribution automatically in CI for every push to `main`, and SHALL let it be started by
hand for the current `main` as well. A CI distribution SHALL build the signed, versioned `production` release from
the pushed commit and upload it to the configured tester group, with no developer machine involved. When the
release notes would be empty (no change archived since the previous distribution, and no release notes supplied
with the merged pull request), the system SHALL publish nothing and create no tag, and SHALL say so in the run's
summary. A CI distribution SHALL run only after the CI regression run has passed on the same commit. Two CI distributions
SHALL NOT run at the same time; a later one SHALL wait for the earlier one to finish.
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

#### Scenario: A push whose checks fail
- **WHEN** a commit reaches `main` and the CI regression run fails on it
- **THEN** CI does not distribute it

#### Scenario: A release secret is missing
- **WHEN** a CI distribution runs and the release keystore, its password or the upload identity is not configured
- **THEN** it fails before building or uploading, the failure names what is missing, and no tag is created

#### Scenario: A pull request cannot reach the release secrets
- **WHEN** a pull request's checks run, including from a branch that changes the workflow files
- **THEN** the release keystore, its password and the upload identity are not available to them

## MODIFIED Requirements

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
current when the pull request's description is edited. When the merged pull request's description has a `## Release notes`
section, its text SHALL be the release notes, so a reviewed or edited version replaces the generated draft and a
pull request that archives nothing can still supply notes. Otherwise, the generated draft SHALL be the release
notes. A commit that reaches `main` without a pull request SHALL use the generated draft.

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
- **THEN** the CI distribution uploads that section's text as the release notes, instead of the generated draft

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
