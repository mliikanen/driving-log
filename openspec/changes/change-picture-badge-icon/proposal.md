# Proposal

## Why

The picture preview on the add and edit vehicle screens carries a small pen badge to tell the user the preview can be tapped. A pen
says "edit text", not "set a photo", and it sits on top of a vehicle icon, so it is easy to read as "edit the vehicle type" or "edit the
vehicle". Tapping the preview is the only way to set a picture, so the mark on it should say what happens: a photo is chosen or taken.

## What Changes

- Replace the pen badge on the picture preview of the add and edit vehicle screens with a camera badge, the same in both states
  ("Add picture" and "Change picture"). The badge stays a decorative mark in the corner of the preview: the preview itself keeps its
  accessibility label ("Add picture" / "Change picture") and its click action.
- Draw the camera from the Phosphor set the app already uses for the vehicle type icons (`camera-fill`, MIT, added to `docs/icons/phosphor` and to
  `THIRD_PARTY_NOTICES.md`), so no icon library dependency is added.
- Give the badge a test tag so the Maestro flows can assert it, and update the picture flows.

Out of scope:
- The type-based placeholder icon for vehicles without a photo. That is specified and implemented by the pending `add-vehicle-type`
  change and is not touched here; the camera badge sits on top of whatever placeholder or picture the preview shows.
- The pen on the vehicle details screen ("Edit vehicle" action): it edits the vehicle, so it stays.
- Any change to how a picture is chosen, cropped or stored, and to the "Remove picture" action.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `vehicle-picture`: the requirement "A vehicle can have one optional picture" says the preview shows "a small edit mark"; it now says
  the preview shows a small camera mark, with a scenario for it.

## Impact

- Code: `PictureField` (badge icon and tag), a new generated camera `ImageVector` next to the vehicle icons in `shared/.../ui`, and a
  small test that the icon has path data. The `material-icons-core` `Edit` import in `PictureField` goes away.
- Docs: `docs/icons/phosphor/camera-fill.svg`, `THIRD_PARTY_NOTICES.md`.
- Tests: Maestro flows 15 to 18 and `maestro/picture/*.yaml` gain an assertion of the badge; no unit-test behavior changes.
- Ordering: this change modifies the same requirement as `add-vehicle-type`'s `vehicle-picture` delta. Apply and archive
  `add-vehicle-type` first; this delta is written on top of the text that change produces. It also reuses the Phosphor SVG-to-`ImageVector`
  approach that `add-vehicle-type` introduces.
