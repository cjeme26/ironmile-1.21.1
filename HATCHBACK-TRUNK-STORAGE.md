# Hatchback trunk storage

First storage pass for the existing CC0 hatchback.

Use
- Stand beside the car.
- Hold Sneak / Shift and right-click the car.
- A 9-slot "Hatchback Trunk" inventory opens.
- Plain right-click still enters the car.
- Tire installation still takes priority when holding an IronMile tire.

Behavior
- Trunk contents are stored on the vehicle entity.
- Contents survive world saves/reloads.
- The screen uses Minecraft's normal one-row container UI.
- Walking too far from the car closes access through the normal inventory range check.
- If the vehicle is destroyed through BoatEntity's normal destruction path, trunk
  contents are scattered into the world instead of being silently deleted.

Deliberately not included yet
- hatch animation
- custom trunk GUI art
- locks/ownership
- different storage sizes per vehicle
- passenger logic

The inventory is kept model-independent so it can later be attached to a real
animated rear hatch without replacing the storage system.
