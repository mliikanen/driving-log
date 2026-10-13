# Design

Batch B follows batch A's design (`openspec/changes/archive/2026-10-13-split-long-requirements-a/design.md`): shorten
first, split only what can't fit, keep every requirement's name and every scenario word for word, state every old
SHALL, SHALL NOT and MAY again. Only what differs is written down here.

## Context

- None of the in-flight `change/*` branches has a delta for any of the six specs.
- `vehicle-picture`'s two longest requirements carry a "Platform note" between the description and the scenarios.
  OpenSpec counts everything before the first scenario as the description, so the notes count toward 500.

## Decisions

### 1. Platform notes move below the scenarios

The two notes move, word for word, to the end of their requirement's block, after the last scenario. That's where
`vehicle-log`'s "The log is not changed by editing the vehicle" already keeps its note, and it changes nothing about
what is required. *Alternative:* shorten the notes. Rejected: they record platform facts (which Android and iOS APIs,
why iOS stores PNG), and cutting them loses information.

### 2. Split by what a reader looks for

`vehicle-picture`'s crop requirement (1,933 characters, 17 scenarios) becomes the crop rule itself plus five
requirements, one per thing a reader looks up: the app bar, the non-gesture controls, turning the photo, edge to edge,
surviving a restart. The others split the same way: the stored code out of the two "fixed set" requirements, the
permission rule and the unusable-image rule out of the chooser, the license-copy rule and the dependency rule out of
the third-party list, and the note editor, the shared entry and the refueling's fixed data out of the edit
requirement.

### 3. Each new scenario gets its test

Every new requirement has one new scenario stating its behavior directly. Apply finds the test that checks each and
adds one where none does; adding tests is expected. A scenario the app turns out not to meet is rewritten to what the
old requirement says, never the app changed to fit it. `licensing`'s scenarios are about repository files, not the
app: their check is reading those files, recorded in the task.

## Risks / Trade-offs

- **A reworded sentence quietly changes meaning.** → One task per spec compares the old and the new text and lists
  any statement whose wording could be read differently.
- **Some scenarios can only be checked on a device** (edge to edge, the iOS camera prompt, Android's absent permission
  prompt). → The task names the check that covers each: a Compose UI test, `maestro/check-permissions.sh`, or a
  manual check stated in the task when nothing automated can.
