# Proposal

## Why

Typing an odometer or trip-meter reading by hand from a dashboard is slow and error-prone — small digits, glare,
reflections and stylized digital-segment fonts all make it easy to mistype a digit, especially on a phone screen
while standing next to the vehicle. A photo the user already has (or takes on the spot) can be read automatically
instead, with the app confirming what it read before anything is filled in.

Confirmed against real dashboard photos supplied as test data (`~/Downloads/odo/`, `~/Downloads/trip/`): a single
photo commonly shows several numbers at once — a clock, a fuel range, a speed-limit sign, a consumption figure — not
just the reading wanted. One photo's clock ("16:21") is a plausible-looking 4-digit number easily confused with a
reading if only magnitude were checked. This is why detection needs both a label check (an adjacent recognized
"ODO"/"TRIP" token) and a magnitude check (plausible against the vehicle's current known odometer), not either
alone, and why the user picks from labeled candidates rather than the app silently guessing one.

## What Changes

- A new "Scan a reading" action on the log event form, next to the odometer/trip field, reusing the existing system
  photo/camera chooser (`PhotoResult`, already used for vehicle pictures) — no new camera permission is needed for
  this phase, since the system camera app (not this app) holds it.
- Offline, on-device OCR (ML Kit Text Recognition — already the project's stated tech choice, not yet wired in) finds
  every plausible numeric reading in the chosen photo.
- Each candidate is classified as a likely odometer or trip-meter reading using two signals together: an adjacent
  recognized label ("ODO", "ODOMETER", "TRIP", "TRIP A/B", or a lone "T"), and its magnitude compared against the
  vehicle's current known odometer at the entry's date and time (already computed elsewhere in `distance-logging`) —
  close to or above it reads as odometer-like, clearly smaller reads as trip-like. See design.md for exactly how the
  two signals combine when they agree, disagree, or a label is missing.
- A review screen shows the photo full-size with a box drawn around every candidate and its guessed label ("ODO" or
  "TRIP") next to it — grey by default, green once tapped/selected. The user taps a candidate, then confirms; back
  navigation (or no confirm) leaves the log event form exactly as it was, with nothing kept.
- Accepting a candidate sets the odometer/trip field to its value, and switches the form's "Trip distance"/"New
  odometer" toggle to match the candidate's classification (odometer-like switches to "New odometer", trip-like to
  "Trip distance"), whichever it was on before.
- The photo and the full detection metadata (every candidate's box, recognized text and classification — not just
  the accepted one) are stored in the app's private storage and linked to the resulting event, purely to debug
  misdetections later. Neither is ever shown in any user-facing screen or list.

## Assumptions Recorded for Review

- **One generic "Scan" action, not separate "scan odometer"/"scan trip" buttons.** The request's own emphasis on
  automatic sanity-checking ("the application should automatically do a sanity check whether detected values are
  odo or trip meter") only makes sense if the app decides, not the user pre-declaring intent by which button they
  tapped. Available regardless of which way ("Trip distance"/"New odometer") the form is currently on; accepting a
  candidate can switch it.
- The photo and its metadata are stored only when a candidate is **accepted** — cancelling the scan discards the
  photo, matching how leaving the log event form without saving already discards everything else on it.
- A detected number is read directly in the vehicle's configured odometer unit (no unit conversion): the assumption
  is that a dashboard shows readings in the same unit the vehicle was set up with.

## Out of Scope (Follow-ups)

- **Real-time in-app camera scanning** (a live viewfinder with continuous OCR, as opposed to a single photo taken or
  picked once): needs its own camera-permission and CameraX-preview design, explicitly deferred to a future,
  separate proposal per the request.
- **Auto-suggesting which vehicle a reading belongs to**, from the detected odometer value matched against each
  vehicle's own known current odometer (useful once a user has several vehicles and isn't sure which one a stray
  photo was of): noted as a future idea, not designed or built here.
- **One-tap access from the Home screen**: a camera/image icon on the Home screen's action grid that jumps directly
  into this scan flow (today's grid — car/Vehicles, note/Log event, route/Trip, question-mark/Placeholder, per
  `app-shell` — would have Vehicles move to the bottom row, into the Placeholder slot, freeing a more prominent spot
  for the new scan action). The developer wants this prominent given how central photo capture is meant to become,
  but asked for it as its own future proposal rather than folded into this one.
- iOS: ML Kit's on-device text recognizer is Android-only. This change ships Android-only, matching the project's
  existing platform-gap pattern (e.g. picture encoding is WebP on Android, PNG-only on iOS today); an iOS OCR
  backend (e.g. Apple's Vision framework) is future work if iOS becomes a real target for this capability.

## Capabilities

### New Capabilities
- `odometer-ocr-capture`: capturing a photo, running on-device OCR, classifying candidates as odometer- or
  trip-meter-like, letting the user pick one, and storing the photo with its full detection metadata for debugging.

### Modified Capabilities
- `distance-logging`: the log event form gains the "Scan a reading" action, and accepting a candidate from it
  populates the active field and switches the form's way to match.

## Impact

- New Gradle dependency: ML Kit Text Recognition (on-device model), Android-only for now.
- `shared/src/commonMain/kotlin/com/mikonoma/drivinglog/vehicle/ocr/` (new): the OCR abstraction (`expect`/`actual`,
  matching the project's platform-boundary convention), the label+magnitude classification logic (pure, unit-tested
  against the recognized-text shapes the real test photos show), and a new debug-only photo+metadata store (separate
  from `VehiclePictureStore`: these photos are never cropped to a square, never shown to the user, and don't need
  small/large display-optimized versions).
- `shared/src/commonMain/sqldelight/.../VehicleEvent.sq`: a new nullable column linking a saved distance entry or
  odometer anchor to its capture (if any) — a `.sqm` migration, which makes every checked-in Maestro fixture stale
  (`docs/test-fixtures.md`); fixtures are regenerated as part of this change.
- `LogEventContract`/`LogEventProcessor`/`LogEventScreen`: the new action, the candidate-review screen, and the
  accept-flow wiring into the odometer field and the way toggle.
- No change to any other capability's existing behavior; the odometer/trip field can still be typed by hand exactly
  as today.
