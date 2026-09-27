# Design

## Context

Confirmed directly against the test photos supplied (`~/Downloads/odo/`, 7 photos; `~/Downloads/trip/`, 4 photos —
paths as given had a `Download`/`Downloads` typo, resolved to the real `~/Downloads/` location):

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
- iOS support (ML Kit's on-device text recognizer is Android-only).
- Any Home-screen entry point — noted as a follow-up in proposal.md, not designed here.

## Decisions

### OCR engine: ML Kit Text Recognition v2, on-device, Android-only
Already the project's stated choice. The on-device Latin-script model runs fully offline once downloaded (bundled or
fetched once, not per-call) and returns recognized text blocks/lines/elements, each with a bounding box in image
coordinates — exactly what's needed for both classification (reading nearby text as a label) and the review UI
(drawing a box per candidate). No alternative was evaluated: the project context already settled on it.

### A candidate is a recognized numeric token that plausibly reads as a distance
Not every digit ML Kit finds is a candidate. A recognized text element becomes a candidate only if, after normalizing
punctuation, it parses as a number with at least three significant digits (before any decimal point) — enough to
exclude a bare speed ("0"), a gear indicator, or a single-digit temperature, while still accepting a plausible trip
distance as low as three digits and any real odometer reading. This threshold is a starting point to validate and
tune against the real test photos during implementation (task below), not a value to treat as final without doing
so.

### Classification: label adjacency first, magnitude against the known odometer second
For each candidate, look at the recognized text elements near it in the photo (the same line, or immediately above
it) for one of "ODO", "ODOMETER", "TRIP", "TRIP A", "TRIP B", "T" (case-insensitive). A match classifies the
candidate directly (ODO-family → odometer-like, TRIP-family/"T" → trip-like) regardless of magnitude — labels are
the strongest signal the real photos actually provide.

Without a matching label, fall back to magnitude against the vehicle's current known odometer at the entry's date
and time (`distance-logging`'s existing computation, already unit-converted to the vehicle's odometer unit):
- At or within a generous margin above the known odometer (a plausible long day's or week's driving, tunable) →
  odometer-like.
- Small enough to be a plausible trip distance (tunable upper bound, e.g. low thousands) *and* clearly below the
  known-odometer-minus-margin band above → trip-like.
- Neither → not a candidate at all; dropped rather than shown with a guess. (This is what keeps the car photo's fuel
  range "917" from ever reaching the user as a candidate labeled either way, assuming it clears the digit-count
  filter at all — it's plausible as a trip distance by magnitude alone, so the label-and-magnitude combination, not
  magnitude alone, is what's load-bearing here.)

### Storage: a new, dedicated capture store — not `VehiclePictureStore`
A new `OcrCaptureStore` (interface in commonMain, Android implementation in androidMain, matching
`VehiclePictureStore`'s split), storing one full-size encoded photo (not cropped, not split into small/large — this
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
    event_id    TEXT NOT NULL PRIMARY KEY REFERENCES vehicle_event(id),
    photo_id    TEXT NOT NULL,
    detections  TEXT NOT NULL  -- serialized: every candidate's box, recognized text and classification
);
```
One row per event that came from an accepted scan; no row otherwise. This is a `.sqm` migration, which makes every
checked-in Maestro fixture stale (`docs/test-fixtures.md`) — regenerating them is a task below, same as any other
migration.

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
