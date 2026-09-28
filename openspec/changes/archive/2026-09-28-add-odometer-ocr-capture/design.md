# Design

## Context

Confirmed directly against the test photos supplied, now checked in under `maestro/assets/ocr/` (`odo/`, 8 photos;
`trip/`, 4 photos):

- A modern car cluster shows "ODO 71140 km" explicitly labeled, alongside unrelated numbers on the same screen: a
  clock ("7:36"), a fuel range ("917 km"), a consumption figure ("0.0 L/100km"), a speed-limit sign ("30"), a
  temperature ("3°C"). Only one of these is the odometer; nothing about its magnitude alone distinguishes it from,
  say, the fuel range.
- A motorcycle cluster shows both "T" (a small trip-mode icon) with a "0.00"/"0" readout and "ODO 5034 km" or
  "ODO 3056 km" explicitly labeled, sometimes in the same frame, sometimes cycling between screens (one photo shows
  "TRIP 209.1 Km" spelled out fully instead of just "T"). One photo's clock reads "1621" (16:21 with no colon
  rendered) — a 4-digit number shaped exactly like a plausible reading, with no label at all.

This confirms the proposal's premise directly: label text ("ODO"/"TRIP"/"T") is the strongest signal when present,
existing readouts are inconsistently labeled (icon vs. full word) across even two dashboards from the same
manufacturer, and an unlabeled number can look exactly like a real reading (the clock). See proposal.md for what
this drives (label-first, magnitude-fallback classification, human confirmation as the real safety net).

Existing infrastructure this reuses or sits beside:
- `PhotoResult` (`vehicle/picture/PhotoPicker.kt`) — the system chooser/camera-intent flow already used for vehicle
  pictures. Reused as-is for choosing the photo to scan; no new permission.
- `distance-logging`'s existing "previous known odometer" computation — reused as-is for the magnitude heuristic;
  not recomputed here.
- `VehiclePictureStore`/`ImageCodec`/`PictureDraftEditor` — the *pattern* (pending capture, promoted on save,
  swept otherwise) is reused conceptually, but not the implementation: these photos are never cropped to a square,
  never shown to a user, and don't need small/large display-optimized versions, so a parallel, simpler store is
  cleaner than overloading the vehicle-picture one with a second, differently-shaped use case.
- ML Kit is already named in `CLAUDE.md`'s tech stack ("ML Kit for OCR") but not yet a dependency anywhere in the
  project — confirmed by searching `gradle/libs.versions.toml` and the codebase. This change is what wires it in.

### Measured with ML Kit (task 1.3)

ML Kit `text-recognition` 16.0.1 (bundled model) on the `dl34` emulator, through a temporary instrumented harness:

- **Car clusters (6 photos): usable.** `ODO` / `71140km`, `32478 km`, `50961 km`, `16865 em` over `Total distance`.
  The recognizer returns the number and its unit as one word (`71140km`) as often as two; puts the label on the line
  above the number (`ODO` over `71140km`) or below it (`16865` over `Total distance`), not beside it; misreads O as
  zero (`OD0`); and drops digits at native size that it reads at twice the size (`1140knm` becomes `71140km`,
  `OD0` + `1140` becomes `ODO` + `71140`). Two car photos show the odometer with no label at all; the ranges beside
  them carry a gear/mode letter (`P890 km`, `D870 km`).
- **Motorcycle LCD (6 Aprilia Shiver photos): not read.** Seven-segment LCD digits are never recognized, whole photo
  or cropped, at any scale or contrast tried; only the RPM dial's printed numbers and sometimes the `ODO`/`TRIP`
  labels come back. The "1621" clock the classification was first modeled on never reaches classification at all.
  Reading these is `add-seven-segment-ocr`'s job (a second recognizer and a region the user marks); here they end
  in the designed "no reading found" state.

### Detection end to end (task 2.4)

ML Kit at twice the size, then detection and classification, with a known odometer just below each photo's reading, on
the emulator (290–570 ms per photo):

| Photo | Known | Candidates presented |
|---|---|---|
| `odo/20250831_073738`, `…073743`, `…073744` | 71000 | 71140 odometer (label `ODO`/`OD0`); 917 trip (the range, by magnitude) |
| `odo/20251109_193736` | 32400 | 32478 odometer (magnitude); 890 trip (the range, by magnitude) |
| `odo/20251228_190558` | 50900 | 50961 odometer (magnitude) |
| `odo/20260221_193742` | 16800 | 16865 odometer (label "Total distance") |
| the six LCD photos | reading − ~50 | none ("no reading found") |

Every car reading is found and classified right; the only unwanted candidates are the ranges, the accepted risk below.
The dial numbers, clocks and letter-prefixed ranges are all dropped. No constant needed changing from the values
above.

## Goals / Non-Goals

**Goals:**
- Fill the log event form's odometer/trip field from a photo, entirely offline, with the user confirming what was
  read before anything changes.
- Keep enough of what was detected (photo + every candidate, not just the accepted one) to debug a wrong
  classification later, without ever surfacing it to the user.

**Non-Goals:**
- Real-time/continuous scanning through a live camera preview — a separate future proposal (needs its own
  camera-permission and CameraX design).
- Perfect classification. The label+magnitude heuristic is a convenience, not a guarantee (see Risks) — the user's
  own look at the boxed photo before confirming is the actual correctness safeguard.
- iOS support (ML Kit's on-device text recognizer is Android-only): the "Scan a reading" action is not shown on iOS.
- Reading seven-segment LCD digits (measured above; `add-seven-segment-ocr`).
- Any Home-screen entry point — noted as a follow-up in proposal.md, not designed here.

## Decisions

### OCR engine: ML Kit Text Recognition v2, on-device, Android-only
Already the project's stated choice. The on-device Latin-script model runs fully offline once downloaded (bundled or
fetched once, not per-call) and returns recognized text blocks/lines/elements, each with a bounding box in image
coordinates — exactly what's needed for both classification (reading nearby text as a label) and the review UI
(drawing a box per candidate). No alternative was evaluated: the project context already settled on it.

### The photo is recognized at twice its size when it is small
The photo is decoded as `ImageCodec` decodes it (orientation applied, at most `MAX_DECODE_SIDE`), then, when its
longer side is under 2560 px, scaled up to 2560 before recognition; the boxes are scaled back to the decoded photo's
pixels. Measured above: every car reading that was wrong or missing at native size was right at twice the size, and
nothing that was right got worse. A photo already that large is recognized as it is.

### No network permission: ML Kit's telemetry is cut off
The bundled recognizer needs no network, but ML Kit pulls in Google's usage-telemetry library
(`com.google.android.datatransport`), whose manifest adds `INTERNET` and `ACCESS_NETWORK_STATE`. The project asks for
no system permission a function does not need, so the app's manifest removes both (`tools:node="remove"`); the
telemetry then fails silently, and recognition is offline by construction. Checked with `maestro/check-permissions.sh`
and the `scan-reading` flow on the emulator.

### A candidate is a number on its own, optionally followed by a unit
A recognized word becomes a candidate when it is digits, with at most one decimal separator (`.` or `,`) followed by
one or two digits, optionally followed directly by letters (a unit run into the number: `71140km`, or OCR noise after
it: `1140knm`), and has three to seven digits before any decimal separator (seven being the most the odometer field
holds). A word with a letter *before* the digits
(`P890`, `D870`: a range with its mode letter), or with any other character in it (`7:36`, `19:37`, `3-40`), is not a
candidate. The value is read with the separator as a decimal point.

### Classification: label next to it first, magnitude against the known odometer second
A label is a whole recognized line that is exactly one of "ODO", "ODOMETER", "TOTAL DISTANCE" (odometer-like) or
"TRIP", "TRIP A", "TRIP B", "T" (trip-like), compared case-insensitively with zero read as O; or such a label as the
first word of the candidate's own line, directly before it. A lone "T" counts only on the candidate's own line: on
another line it is as likely a mode icon beside some other figure (on `odo/20220911_162029`, read at twice the size, the
clock 16:21 comes back as `1521` with the trip-mode "T" right under it). A label line counts when it is directly above or below the
candidate (a vertical gap of at most one and a half of the candidate's height) and overlaps it horizontally or is
within the candidate's width of it. Requiring the whole line keeps "Trip Average" (the label of the consumption
figure under the car's range) from labeling the range as a trip. A label decides regardless of magnitude.

Without a label, a candidate is classified only if a distance unit follows it (in the same word or as the next word on
its line: a word starting with "k", like `km` or the misread `knm`, or "mi"/"mile"/"miles"). That drops the dial
numbers (`120`, `160`, `RPMx 1000`), which carry no unit. With a unit, its value is compared with the known odometer
*K* at the entry's time, in the vehicle's odometer unit:
- `K ≤ value ≤ K + max(5000, K / 2)` → odometer-like.
- `value ≤ 2000` and `value < K` → trip-like.
- Otherwise → not a candidate.

When no odometer is known at that time, a value above 2000 is odometer-like and one at or below it trip-like. The
three constants live together in the classifier. The car's range `917 km` still classifies as trip-like (see Risks),
and the review screen is where the user passes it over.

A known odometer is not used to "repair" a partial reading (`1140` into `71140` by its last digits): once the vehicle
has been driven the digits no longer match, and the twice-size recognition above already recovered every such case
measured.

### Storage: a new, dedicated capture store — not `VehiclePictureStore`
A new `CaptureStore` (interface and a file implementation in commonMain on kotlinx-io, the way `FilePictureStore`
is), storing one full-size encoded photo (not cropped, not split into small/large — this
is a debug artifact, not something rendered in a UI) and its detection result as a structured, serialized record
(every candidate's box, recognized text and classification). Mirrors the pending/promote shape
(`putPending`/`promote`/`discardPending`/`sweep`) `PictureDraftEditor` already established, for the same reason: an
accepted scan is provisional until the form is actually saved, at which point it's promoted under the new event's
id; leaving the form sweeps it away like an unpromoted vehicle-picture crop already does.

### Schema: a new side table, not columns on `vehicle_event`
A capture's detection result is a list (however many candidates were found), not a scalar — awkward as several new
`vehicle_event` columns, and irrelevant to every event that isn't from a scan. A new table instead:
```sql
CREATE TABLE event_capture (
    event_id    TEXT NOT NULL PRIMARY KEY,  -- not a foreign key: a capture outlives its event
    photo_id    TEXT NOT NULL,
    detections  TEXT NOT NULL  -- serialized: every candidate's box, recognized text and classification
);
```
One row per event that came from an accepted scan; no row otherwise. `event_id` is deliberately not a foreign key of
`vehicle_event`: removing an event (a future change) must neither cascade into its capture nor be blocked by it. The
capture is debugging evidence and stays until the capture itself is removed; the capture store's sweep keeps every
photo whose `event_capture` row exists, whether or not the event does. This is a `.sqm` migration, which makes every
checked-in Maestro fixture stale (`docs/test-fixtures.md`) — regenerating them is a task below, same as any other
migration.

### The review screen draws boxes around the numbers and names what was read
On a phone the photo is shown a few hundred dp wide, so a dashboard number is a few dp tall: a box drawn on the
number's own bounds covers it (checked on the emulator). Each box is drawn 4 dp outside the number, and its label
says the kind and the value ("ODO 71140"), so the user sees what will fill the field without zooming.

### The review screen's colors: reuse the theme's existing driving-distance accent, don't hardcode green
The project's own color convention (`CLAUDE.md`) forbids hardcoded colors — everything comes from
`MaterialTheme.colorScheme` or `DrivingLogTheme.domain`. Distance-related UI already has a domain accent for exactly
this purpose ("Road Trip Emerald" — used for logged distances elsewhere in the app), which becomes the "selected"
candidate's box/text color; the neutral, unselected state uses `colorScheme.outline`/`onSurfaceVariant`, matching how
other neutral/inactive UI in the app is drawn. This satisfies "grey for valid, green for selected" from the request
without introducing a new, unthemed color.

## Risks / Trade-offs

- **[Risk]** The label+magnitude heuristic is not airtight — an unlabeled number that happens to fall in the
  plausible-trip magnitude range (the car photo's "917 km" fuel range, or a lower-magnitude clock reading on a
  vehicle with a large known odometer) can still surface as a false candidate → **Mitigation**: accepted. The
  candidate is shown boxed on the actual photo before anything is accepted — a box drawn over the fuel-range text
  looks visibly wrong to the user, who can simply not select it (or select nothing and leave). Perfect
  classification was never a goal (see Non-Goals); a good-enough shortlist with a human confirming is.
- **[Risk]** ML Kit's Latin text model is tuned for ordinary printed/handwritten text, not small, stylized digital
  seven-segment or dot-matrix dashboard fonts, glare, or an off-angle photo (several test photos have real screen
  reflections and dust/water spots) → **Mitigation**: accepted as an inherent limitation of any OCR-based approach;
  "no candidates found, try another photo" is an explicit, designed-for outcome, not a crash or a silent failure.
- **[Risk]** The digit-count and magnitude thresholds above are starting points, not measured — **Mitigation**: a
  task below validates classification against every supplied test photo before considering this done, and the
  thresholds live in one place (the classifier), not scattered, so tuning them later is cheap.
- **[Risk]** A `.sqm` migration invalidates every checked-in Maestro fixture → **Mitigation**: known, standard
  process (`docs/test-fixtures.md`); regenerating them is one Gradle command, included as a task.
