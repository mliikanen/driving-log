# Test strategy

Every check has one owner: **put it in the lowest kind of test that can check it.** The lower a test sits, the faster it runs and the less it breaks
for reasons that are not the change, so a case belongs in a unit test, a screen's reaction in an integration test, and only the journey a user
follows from start to finish in Maestro. A suite that is fast enough is a suite that gets run.

## The kinds of test

| Kind | What it is for | Example in this project | Where | Today |
|---|---|---|---|---|
| **Pure unit tests** | Many input/output variations of small pieces of code: rules, parsing, formatting, the logic of a processor (Kide `test { dispatch; expectSideEffect }`), repositories on an in-memory database | `OdometerEntryTest` (every key sequence of the odometer field), `LogDistanceRulesTest` (an equal count, a future moment), `AddVehicleProcessorTest` | `shared/src/commonTest`, `kotlin.test`; JVM-only ones (SQLDelight driver) in `shared/src/androidHostTest` | **Yes** |
| **Compose integration tests** | MVI: a screen together with its processor, the intents a user causes and the state and effects that follow (a field shows its error, a choice survives recreation) | none yet | `shared/src/androidHostTest` (Robolectric with Compose UI test) | **Not yet adopted** (`add-compose-integration-tests`) |
| **Screenshot tests** (Roborazzi) | The structure of the UI changes only intentionally: a reference image per screen state, a diff fails the build | none yet | `shared/src/androidHostTest` (Roborazzi on Robolectric) | **Not yet adopted** (`add-screenshot-tests`) |
| **Maestro flows** | The happy path of a feature, end to end, on a real emulator or device with the real app: what the user does from opening the screen to seeing the result | `vehicles/add-and-browse`, `picture/add` | `maestro/` | **Yes** |

**Unit tests use `kotlin.test`**, not JUnit Jupiter: the shared tests run on every platform (the Android host and iOS), and Jupiter is JVM-only. It is not used in the shared source sets.

### Platform split

- Pure unit tests are **shared**: they live in `commonTest` and run on Android and on iOS, so business logic is checked once for both.
- Robolectric, Compose UI test and Roborazzi are **Android/JVM tools**. Tests that use them are Android tests, never `commonTest`. Because the screens and processors are in `shared`, and `shared` already has an
  `androidHostTest` source set, they live in `shared/src/androidHostTest` and reach the shared screens directly. `androidApp` holds only tests of the shell (the activity, the DI graph), if there are any.
- iOS has no counterpart of these tiers yet: its screens are covered by the shared unit tests, and by a tier of its own once an Xcode project exists. Maestro flows are Android flows today.
- The tiers marked "not yet adopted" above are described here as the target. Their dependencies and first tests are added by their own changes; until then a screen's behavior is checked through its
  processor's unit tests, and what only a rendered screen shows (a control that is reachable, a rotation that keeps what was typed) is checked by a Maestro flow. That is a known gap, not a licence to write
  case-by-case flows.

## Maestro flows are happy paths

A flow follows **one journey** of a feature and checks that it works: add a vehicle in each unit, see it in the list, open its details. It does not check cases, error messages or combinations of inputs.
Several flows per feature are fine; two flows that follow the same journey are not (merge them).

- A case idea ("saving without a name is refused", "a future date is refused") is a unit test of the rule or of the processor. Before an assertion is taken out of a flow, the test that owns it exists and passes.
- What only a flow can show may stay: that the app works with the network off and after being closed and reopened, that a screen survives a rotation, the system's photo chooser and the camera app.
- Flows read the screen the way the user does: text, and the test tags of the app's own screens (`id:`). System dialogs and other apps' windows have no tags and are driven by text.

### Manifests and setup

Flows are grouped by feature into **manifests**: Maestro configuration files in `maestro/manifests/<area>.yaml` that list the flows of an area and their order (`flowsOrder`). The flows themselves are in `maestro/<area>/`,
shared steps in `maestro/subflows/`, test photos in `maestro/assets/` (see its README).

A manifest whose flows choose photos starts with `setup.yaml`, which uploads them to the device **once for the whole manifest**; the flows after it do not upload anything (`grep -rn addMedia maestro` lists only setup flows).
A fresh emulator with the debug app installed is all a manifest needs.

```
maestro/run.sh vehicles                # one manifest
maestro/run.sh vehicles distance       # several
maestro/run.sh vehicles edit           # the manifest's setup, then one flow of it
maestro/run.sh --all                   # every plain manifest
maestro/run.sh picture theme clock     # the device-state groups (they change or inspect device state over adb)
```

`run.sh` first removes the test photos earlier runs left on the emulator (each upload adds another copy, and after a few dozen the emulator's `addMedia` starts to fail): see `maestro/reset-media.sh`.
The manifests are Maestro configuration files over the one `maestro/` workspace (Maestro allows media only from inside the workspace), and tags are not used, since Maestro cannot combine them with `flowsOrder`.

`run.sh` also enables the device's "Show taps" setting once per invocation (not restored afterward), so a failing flow's recording shows exactly where each tap landed.

A flow whose subject is how already-stored data renders (not the UI path that builds it) may instead start from a
fixture database seeded directly onto the device, rather than building that state through the UI — see
[`docs/test-fixtures.md`](test-fixtures.md), including how a fixture is kept in sync with the schema. A flow may
likewise declare its starting *auth* state — signed out, or signed in as a specific test account — as a `launchApp`
argument (`testingStartUser: "none" | "A" | "B"`, read only by the `fake` flavor's `AuthRepository`), instead of
building it through the sign-in UI, for the same reason and in the same place: every other flow declares nothing
and gets today's default (already signed in), the same way a flow with no fixture argument gets an empty database
and builds whatever it needs through the UI (`add-firebase-auth`).

## What is run when

| When | What |
|---|---|
| While working on a change | The unit tests that concern the code (`./gradlew :shared:allTests` when in doubt), and **the manifests of the functionality the change touches** (a change to the vehicle picture runs `picture`; one to the add and edit forms runs `vehicles`, and so on). Only flows that were changed, or that exercise the screens changed. |
| The final regression run of a change (before `/opsx:archive`) | `./gradlew :shared:allTests :androidApp:assembleDebug` and `openspec validate --all --strict`. **Maestro is not part of it.** |
| A major refactoring (navigation, storage or theme across every screen, the build, the Maestro suite itself) | Everything, including `maestro/run.sh --all` and the device-state groups. The developer says when a change is a major refactoring; it is not decided by the change's author. |

A change's tasks therefore name the manifests to run, and do not ask for the whole suite unless the change is a major refactoring.
