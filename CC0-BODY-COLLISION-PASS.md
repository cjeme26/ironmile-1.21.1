# CC0 hatchback body / collision pass

This patch changes only the physical body approximation of the temporary CC0 car.

Why
Minecraft's normal EntityDimensions box is axis-aligned. Making the hatchback's
single box long enough to cover the full visible model would also make it nearly
four blocks wide whenever the car faces north/south/east/west.

Approach
The existing center CarEntity collision remains the cabin/roof region.
Two invisible collidable body segments now follow the car:
- front/hood: 1.65 blocks wide, 0.76 high
- rear/hatch: 1.65 blocks wide, 0.96 high

They sit roughly 1.08 blocks in front of and behind the entity center. Together
with the center body, this gives the visible hatchback a much more complete
physical footprint without creating one enormous square hitbox.

Expected result
- Hood/front body should be solid instead of letting the player occupy visible car.
- Roof/center remains solid.
- Rear body should be solid at a lower height than the cabin.
- At diagonal angles the three smaller boxes approximate the long rotated car
  much better than one huge axis-aligned box.
- Collision segments are invisible and cannot be targeted, punched, entered, or
  right-clicked. Normal interaction still belongs to the actual car.

No changes to:
- driving physics
- suspension / bumper road collision
- manual or automatic transmission
- clutch
- fuel
- storage
- HUD/UI
- CC0 visual model
