# Design

## Context

Why the specs drift, from the changes that made each statement in #31 and #34 stale:

| Statement | Made stale by | What its deltas did |
| --- | --- | --- |
| Kind selector's single-kind rule and two scenarios | `add-refueling-logging` | Modified the requirement but kept them, reworded "(not the case today…)": MODIFIED can't drop a scenario |
| "Logging only adds", "A pending note…", "Photos can be added…", the Purpose | `add-refueling-logging`, `add-event-pictures`, `atomic-log-events` | Not touched: no delta for requirements outside what the change set out to do |
| Field order without the fuel type | `add-vehicle-fuel-type` | Wrote `vehicle-fuel-type` and `refueling-logging` only; `vehicles` never read |
| "The placeholder is a generic car icon" | `add-vehicle-type` | Added a new scenario next to it; renaming it isn't possible through MODIFIED |
| "Open the log distance form" | `update-log-distance-to-log-event` | Kept on purpose; its design says retitling a scenario isn't supported |
| "Log event" not available "until the changes that build it" | the Home screen route in `distance-logging` | `app-shell` not touched |
| `CropStateTest`'s "move button" names | `adjust-picture-crop` | Tests not in any delta; nothing asks a change to look at test names |
| API names in a platform note | `add-vehicle-picture`, extended by `ask-camera-for-camera-app` | Written into the spec although the spec rules keep API choices out |

So there are two causes:

1. **No sweep.** A proposal writes deltas for the requirements it means to change. Nothing asks it to look for the
   other statements its new behavior makes false: a Purpose, a list or an order, an "only" or "the one exception", an
   example, a requirement or scenario name, a reference to a renamed requirement, a test name, a comment. `openspec
   validate` checks structure, not truth, and review sees only the diff, which never contains the stale line.
2. **Scenarios can't be renamed or dropped.** `openspec validate --strict` (CI's `spec-validation`) and `archive`
   refuse a MODIFIED block that lacks a scenario the main spec has, and a delta can't REMOVE and ADD a requirement
   under the same name. A change that hit this kept the stale scenario.

## Goals / Non-Goals

**Goals:** fix every statement #31 and #34 list, and the ones a sweep for the same kinds of statement found
(`vehicle-log`'s "The log is not changed by editing the vehicle" and "…can carry photos"); make proposals do that
sweep from now on.

**Non-Goals:** a mechanical check for stale statements (whether a sentence is still true needs reading, not a
script); changing OpenSpec itself.

## Decisions

### 1. A scenario that must be renamed or dropped: replace the requirement under a new name

REMOVED with a Reason and Migration, plus ADDED under a slightly different name carrying every other rule and scenario
unchanged. It is the one way strict validation accepts, so CI keeps guarding against accidentally dropped scenarios.
Used for "The kind of event is chosen" (→ "…from Distance and Refueling") and "A vehicle can have one optional
picture" (→ "A vehicle has at most one picture"); nothing but archived changes quotes either old name. Where a
scenario's name is still true ("The required fields come before type and color", "Editing a note is the one
exception"), only its body changes.

Alternatives: keep stale names and reword the bodies (rejected: that is how the drift happened); sync by hand at
archive and exclude the change from validation (rejected: weakens the CI check for every later change).

### 2. "Add a vehicle" follows the app

The form returns to the screen that opened it. That is the navigation stack's plain behavior (`onBack`), the Home
screen's first-tile route and `maestro/subflows/add-vehicle.yaml` rely on it, and returning to a list the user never
opened would be the surprise. So the spec changes, not the app.

### 3. The Kind selector's single-kind branch is removed from the code too

With the rule gone from the spec, the `enabled = LogKind.entries.size > 1` branch implements an unspecified state and
its KDoc still says the selector has one item. `KindSelector` becomes always enabled; nothing tests the disabled state.

### 4. The sweep is an artifact rule

`openspec/config.yaml`'s rules are what every proposal, spec and task artifact is written under, so the sweep goes
there: the proposal lists what it found, the specs fix each one, and tasks rename tests and fix comments.
`docs/change-workflow.md`'s Proposing gets one line pointing to it.

## Risks / Trade-offs

- [The two replaced requirements move to the end of their specs at archive (ADDED appends)] → order within a spec
  carries no meaning; accepted.
- [`add-vehicle-hiding` modifies "Vehicle details screen", which keeps "Open the log distance form"] → a PR comment
  asks it to rename the scenario with Decision 1 when it archives.
- [A sweep is judgment, so it can still miss] → the rule names the kinds of statement to search for, which is what
  every item in #31 and #34 was.
