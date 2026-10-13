## Why

OpenSpec warns about a requirement over 500 characters, so the main specs can't be validated with `--strict` (issue
#13). Batch A brought the count from 76 to 52. This is batch B: the six other specs that no in-flight change branch
has a delta for, `vehicle-picture` (6 long), `vehicle-fuel-type` (3), `vehicle-type` (2), `licensing` (2),
`event-details` (2) and `firebase-auth` (1), 16 requirements in all.

## What Changes

- Every requirement over 500 characters in the six specs is rewritten to 500 characters or fewer, **with no change in
  behavior**: every SHALL, SHALL NOT and MAY in the old text is still stated, by the same requirement or by a new one.
- As in batch A, each keeps its name and every scenario word for word. Most are only shortened.
- Two "Platform note" paragraphs in `vehicle-picture` sit between a requirement's description and its scenarios, so
  OpenSpec counts them as description. They move below the scenarios, word for word, as `vehicle-log` already places
  its own platform note.
- Seven that can't fit are also split, and what moves out becomes 15 new requirements, each with its own scenario:
  - `vehicle-picture`, "A vehicle can have one optional picture": "Tapping the picture preview starts choosing a
    picture".
  - `vehicle-picture`, "The photo comes from an app the user chooses through the system": "An image that can't be used
    changes nothing" and "Permissions are asked for only when needed".
  - `vehicle-picture`, "The photo must be cropped to a square": "The crop is confirmed or cancelled from the top app
    bar", "The crop can be adjusted without gestures", "The photo can be turned on the crop screen", "The crop screen
    is edge to edge" and "The crop survives a rotation and a process restart".
  - `vehicle-fuel-type`, "A vehicle has a fuel type from a fixed set", and `vehicle-type`, "A vehicle has a type from a
    fixed set": the stored codes become "A fuel type is stored as a code that does not depend on the language" and "A
    type is stored as a code that does not depend on the language".
  - `licensing`, "Third-party material is listed with its license": "A license that must accompany copies is checked
    in" and "Downloaded dependencies are outside the list".
  - `event-details`, "A 'Distance' or 'Odometer reading' event's note can be edited": "The edit screen uses the log
    event form's note and photo elements", "Editing changes the entry's shared note and photos" and "A refueling's
    fuel data is never edited".
- Tests: each new scenario gets the test that checks it, existing or new.

Out of scope: the other long requirements (batches C and D of #13), any change in behavior, and fixing wording that is
stale or misplaced but not too long. Those are filed as issues when found.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-picture`: 6 requirements shortened, 3 of them split into 8 new requirements.
- `vehicle-fuel-type`: 3 requirements shortened, 1 of them split into 1 new requirement.
- `vehicle-type`: 2 requirements shortened, 1 of them split into 1 new requirement.
- `licensing`: 2 requirements shortened, 1 of them split into 2 new requirements.
- `event-details`: 2 requirements shortened, 1 of them split into 3 new requirements.
- `firebase-auth`: 1 requirement shortened.

## Impact

- The six main specs once archived. The long-requirement count goes from 52 to 36.
- No app change: merging this publishes no build and needs no release notes.
