# Pickaxe car pickup/drop fix

This pass fixes the inherited BoatEntity destruction behavior.

Survival pickup
- The car can no longer be dismantled by punching it with a bare hand or a
  non-pickaxe tool.
- Any item in Minecraft's pickaxes item tag is accepted, including compatible
  modded pickaxes.
- Pickaxe damage still uses Minecraft's normal vehicle damage/wobble behavior,
  so better pickaxes can dismantle the car faster.
- Creative mode retains vanilla quick-removal behavior.

Correct vehicle drop
- Destroying the car no longer drops a vanilla boat.
- Automatic cars drop the Automatic Yellow Hatchback item.
- Manual cars drop the Manual Yellow Hatchback item.
- The currently installed Summer / All-Season / Winter tire type is written
  back onto the dropped car item and remains visible in its tooltip.
- A custom vehicle name is preserved if the car has one.
- Pick-block also returns the proper configured IronMile car item.

Trunk safety
- Items left in the 9-slot trunk are dropped beside the vehicle when it is
  dismantled instead of being silently deleted.

No driving physics, workbench UI, recipes, fuel, transmission logic, collision,
or tire grip values are changed.
