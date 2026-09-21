# Design

## Context

`PictureField` draws the badge as a 28 dp circle in the primary color in the bottom-right corner of the 96 dp preview, holding
`Icons.Filled.Edit` (16 dp, `onPrimary`). The badge is decorative (`contentDescription = null`); the preview `Box` carries the label and
the click. The app depends only on `material-icons-core`, which has no camera icon (the camera icons are in the large `icons-extended`
artifact). `add-vehicle-type` (pending) adds Phosphor SVGs in `docs/icons/phosphor` and turns them into `ImageVector`s in
`ui/VehicleIcons.kt`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A badge that reads as "photo" at 16 to 20 dp in light and dark themes, on top of both a photo and a type icon.
- No new dependency and no change to the click target, label or semantics of the preview.

**Non-Goals:**
- Changing the placeholder icons, the badge shape, size or position, or the theme colors.
- Showing a different mark for "Add picture" and "Change picture".

## Decisions

1. **Camera icon from Phosphor (`camera-fill`), not from `material-icons-extended`.** `icons-extended` adds several MB of classes to the
   app for one icon, against the project's minimal-footprint stance. Phosphor is already the icon source for `add-vehicle-type`
   (MIT, listed in `THIRD_PARTY_NOTICES.md`), so the file goes next to the vehicle SVGs and is converted the same way. Alternative: hand-draw a
   vector, rejected for effort and inconsistency with the other icons.
2. **One camera for both states.** A camera with a plus would say "add" but is wrong for "Change picture"; a plain camera says "photo" in both
   and the label already tells add from change. Alternative considered: `camera-plus-fill` for add and `camera-fill` for change, rejected as
   extra states for little gain.
3. **Keep the circular primary badge.** Only the glyph changes, so contrast is already covered by the theme's `primary`/`onPrimary` pair
   (tested in the palette contrast tests). The glyph goes to 18 dp inside the 28 dp circle because the filled camera has more mass than the pen.
4. **Where the ImageVector lives.** A separate small file `ui/PhotoIcons.kt` (an `object PhotoIcons { val Camera }`) built by the same helper as
   `VehicleIcons`, if that helper is reusable; otherwise the same construction inline. It is not added to `VehicleIcons`, which is keyed by
   vehicle type.
5. **Test tag `picture_badge`** on the badge so Maestro can assert it. Compose semantics merge: the badge is a child of the clickable
   preview, so a tag on it does not change what a screen reader announces (the preview's label).

## Risks / Trade-offs

- [The camera suggests only "take a photo", while the chooser also offers the gallery and files] → "camera" is the common symbol for a photo
  in avatar pickers, and the chooser text explains the sources; a gallery-and-camera glyph does not exist in the set.
- [Phosphor's `camera-fill` may look heavy at 18 dp] → check on the emulator in light and dark mode (task 3.1) and fall back to the
  `camera` (regular weight) SVG if it does not read well.
- [Overlap with `add-vehicle-type` in the same requirement and the SVG conversion] → apply after it (see proposal Impact); if the type change is
  archived first, the MODIFIED block here already matches its resulting text.
