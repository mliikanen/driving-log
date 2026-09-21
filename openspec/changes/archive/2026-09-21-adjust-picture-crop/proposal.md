# Proposal

> **Decided.** The open questions of the stub were settled by the developer before apply (see the design's Decisions): a fixed square with the photo moving behind it, single-tap buttons next to the gestures, a scrim without a preview, a rotate control, and no soft-picture warning. It builds on the archived `add-vehicle-picture`
> and is independent of `add-vehicle-color` (see "Why now").

## Why

The crop screen is where the user decides what a vehicle's picture is, and today that decision is hard to make well:

- The photo is already **moved under a fixed square and pinch-zoomed** (`CropState`: zoom from the whole shorter side down to a 128 px frame, frame always inside the photo), so
  the request to "select a user-defined subsection, zoom in and out, move it around" is **mostly there for touch**. What is missing is what the user asked around it:
  **no way to do it without pinching** (no buttons, no keyboard, nothing for assistive technology), **no view of what is outside the frame** (the rest of the photo is black,
  so it is hard to know where to move to), and **the frame does not survive a rotation** (the crop state is a plain `remember`, so rotating with the crop screen open resets zoom and position).
- The result matters more once the picture also gives the vehicle its color (`add-vehicle-color` takes the color from the confirmed crop): in tests on street photos, the photos
  where the vehicle is a small part of the frame gave the asphalt or the sky, and a **tight crop fixes that**. A user who can crop precisely gets a better picture *and* a better color.
- The downscale from the crop to the stored 256 px and 1024 px versions happens once, at save time; its quality (no aliasing from a single big step) is worth checking on both platforms.

## What Changes

- **Controls that do not need a gesture:** zoom-in and zoom-out buttons, "Rotate photo" (a quarter turn clockwise) and "Reset" (back to the largest centered square), each acting once per tap and with a
  label for assistive technology; the photo also moves with the arrow keys and zooms with plus and minus when a hardware keyboard is used. Pinch and drag keep working, and dragging is the way to move (**no move buttons**, decided by the developer).
- **Edge to edge:** the crop screen fills the whole window behind the system bars (black background and photo), its buttons kept clear of them.
- **Context outside the frame:** the parts of the photo outside the square are drawn **dimmed** instead of black, so the user sees what is being left out and can move it in.
- **The crop survives rotation and process death** while the crop screen is open (zoom and position are saved with the screen state; they are pixels of the photo, so they mean the same after a rotation).
- **The photo moves and zooms behind a fixed square** (what exists; the alternative, a movable and resizable square over a fixed photo, was considered and not chosen: see the design).
- The output is unchanged: a **square scaled to the stored sizes** (small at most 256 px, large at most 1024 px, never enlarged). The scaling quality is made explicit and checked (see the design): it is a save-time
  quality matter, not a render-time speed matter.

Out of scope: free-angle rotation (only quarter turns; the photo's own orientation is still applied on decode), free aspect ratios, filters, several crops of one photo, a preview of the list tile, a warning about soft pictures, buttons that repeat while held, changing the stored formats or sizes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-picture`: "The photo must be cropped to a square" gains the controls, the dimmed context and the saved crop state (MODIFIED, full block); a new requirement makes the scaling quality explicit (ADDED).

## Impact

- `shared/` commonMain: `CropScreen` (buttons, keyboard handling, dimmed context, labels), `CropState` (step functions for the buttons, a quarter turn, a `Saver` for saving it), `CropState` tests. The confirmed crop carries the quarter turns (`CropConfirmed`, `PictureDraftEditor.cropConfirmed`, `ImageCodec.encodeSquare`, and a turned `DecodedImage` for the screen). Platform: both codecs turn the decoded photo before cropping; the iOS codec sets a high interpolation
  quality when it draws the scaled versions; Android already halves stepwise and finishes with a smooth step (`AndroidImageCodec.scaledTo`).
- **No storage or schema change, no new dependency, no permission.** The stored files, the sizes and the formats are exactly the current ones.
- `maestro/`: the crop screen is another window without test tags, so flows use its text ("Use photo", "Cancel") and the new button labels.
- Interplay with `add-vehicle-color`: the color is extracted from the confirmed crop's small version, so a better crop gives a better color; nothing in either change depends on the other, and their spec deltas touch different requirements.
