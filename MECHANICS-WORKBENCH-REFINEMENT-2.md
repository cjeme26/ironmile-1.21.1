# Mechanic's Workbench refinement 2

This pass keeps the vanilla-style workbench layout and refines the recipe catalogue/assembly UX.

## Recipe catalogue
- Removes the old prose material-description paragraph from the selected recipe area.
- Selecting a recipe now shows its name plus actual ingredient icons.
- Required counts are shown on ingredient icons when greater than one.
- Ingredients the player has enough of use a normal gray slot.
- Missing ingredients use a red-tinted slot and a dimmed item icon.
- Hovering a requirement shows `Have: X / Need: Y`.
- Uncraftable recipe tiles are slightly darker than before.
- Hovering an uncraftable recipe says that the required materials are missing.
- Each page remembers its selected recipe when switching tabs.

## Recipe autofill
- Normal click still loads one craft's worth of ingredients.
- Shift-click loads the maximum number of complete crafts that the player's current materials and slot stack limits allow.

## Tire readability
- Summer tire icons now carry a small `S` in the lower-right.
- All-Season tire icons carry `A`.
- Winter tire icons carry `W`.
- Tire Set icons also carry a `4` in the lower-left.
- These are item texture changes, so the markings appear in the inventory as well as the workbench catalogue.

## Assembly
- Assembly labels are now above the slots.
- The middle label is shortened to `Trans.` so it no longer collides with Body/Tires.
- The temporary sideways-T output marker is replaced with the actual vanilla crafting-table arrow graphic.

No driving, fuel, collision, transmission behavior, storage, or world/runtime files are changed.
