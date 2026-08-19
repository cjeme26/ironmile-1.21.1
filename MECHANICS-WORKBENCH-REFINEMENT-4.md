# Mechanic's Workbench refinement 4

Fixes three concrete issues from refinement 3.

Output item alignment
- Does NOT move the drawn slot or vanilla arrow.
- Body/Parts now have an actual result Slot at y=58.
- Assembly keeps its actual result Slot at y=65.
- Both page-aware result slots point at the same result inventory, so the
  rendered output item is centered inside the slot shown on that page.

Recipe requirement quantities
- Quantity overlays are explicitly rendered above the item-render depth.
- The existing dark backing remains, but the number can no longer be hidden
  behind the item texture.

Assembly recipe-book autofill
- Clicking Yellow Hatchback now fills the Assembly component slots when the
  required components are available.
- A compatible component already placed by the player is preserved.
- Otherwise it uses the first compatible component found in inventory:
  Hatchback Body, then Manual/Automatic Transmission, then any Tire Set.
- The finished configured car then appears in the output normally.
- Shift-click behaves the same as normal click on Assembly because one car
  assembly uses one of each major component.

No recipe costs, vehicle mechanics, tire behavior, UI page geometry, or
crafting-arrow positions are changed.
