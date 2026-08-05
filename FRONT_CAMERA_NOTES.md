# Iron Mile front camera patch

- First-person view moves to a low camera just beyond the Yellow Hatchback's front bumper.
- Third-person front and rear camera modes remain unchanged.
- Camera position follows the interpolated car transform.
- A block raycast pulls the camera back before it enters a wall.
- The existing hide-driver mixin remains enabled.

Tuning constants are at the top of `CameraMixin.java`:
- `IRONMILE_CAMERA_FORWARD_OFFSET`
- `IRONMILE_CAMERA_HEIGHT`
