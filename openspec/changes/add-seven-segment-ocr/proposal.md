# Proposal

## Why

`add-odometer-ocr-capture` reads dashboard photos with ML Kit's on-device Latin text recognizer. Measured on the real
test photos (now in `maestro/assets/ocr/`), that works for car clusters drawn in ordinary fonts, but it cannot read a
reading shown in seven-segment LCD digits, the display a motorcycle (and many older cars) uses: on all six Aprilia
Shiver photos it finds no reading at all, so the scan always ends in "no reading found" for that vehicle. Neither
preprocessing nor the user pointing at the reading fixes that with ML Kit (see design.md, Context), so the problem
is the recognizer, and this change adds one that can read these digits, together with a way for the user to point at
the reading, which a digit recognizer needs to work well on a small part of a busy photo.

**This depends on `add-odometer-ocr-capture`**: the scan action, the review screen, the classification and the
stored detections it builds on are introduced there. This change is applied and archived after it.

## What Changes

- A second, offline, on-device recognizer for seven-segment (LCD) digits runs alongside ML Kit on the scanned photo,
  so a reading like the Shiver's "ODO 5034 Km" or "TRIP 168.1 Km" becomes a candidate, classified and reviewed exactly
  like one read from a car cluster. Which engine that is gets decided by a measured evaluation against the checked-in
  photos (design.md), not assumed up front.
- A "Mark the reading" action on the review screen and on the "no reading found" state: the user drags a box over the
  reading on the photo, and the system looks for candidates only inside that box (with both recognizers), then shows
  them for review as usual. This is how a user recovers when the automatic pass missed the reading or found only
  unrelated numbers.
- The region the user marked, when there is one, is stored with the rest of the scan's detection result, so a
  misdetection can be reviewed with what the user pointed at.

## Out of Scope

- A live camera viewfinder (still a separate, future proposal, as in `add-odometer-ocr-capture`).
- iOS: the scan action stays hidden on iOS (`add-odometer-ocr-capture`), and so does everything this change adds.
- Displays that are neither ordinary printed text nor seven-segment digits (dot-matrix, analog odometer drums): not
  measured, not a goal; they may or may not read.
- Training or shipping a model of our own beyond what the chosen engine needs to be configured with (for example a
  published trained-data file) — if the evaluation shows only a custom-trained model would meet the bar, that is
  brought back as a decision, not absorbed into this change.

## Capabilities

### New Capabilities

None. `odometer-ocr-capture` is introduced by `add-odometer-ocr-capture`; this change adds requirements to it.

### Modified Capabilities

- `odometer-ocr-capture`: seven-segment readings are detected; the user can mark where the reading is; a marked
  region is kept with the detections. (Added requirements only; the requirements `add-odometer-ocr-capture` introduces
  are unchanged.)

## Impact

- A new on-device recognition dependency (Android only), chosen in the evaluation task; its size adds to the APK.
- `vehicle/ocr/`: a second `TextRecognizer` implementation, a way to recognize one region of the photo, and merging
  both recognizers' results before candidate detection.
- The review screen (from `add-odometer-ocr-capture`): the "Mark the reading" action and a drag-a-box mode.
- The stored detection result gains an optional marked region (a serialized field; no schema migration).
- `maestro/assets/ocr/`: the photos double as the evaluation set; the `distance` manifest gains a case for the
  marked-region path on one LCD photo.
