# Smart dismount positioning

This pass changes only where the driver is placed when leaving the car.

Priority:
1. Driver side
2. Passenger side
3. Behind the car
4. In front of the car
5. Minecraft fallback only if none of those are safe

Safety checks:
- Tests the player's full standing collision box before accepting a position.
- Rejects positions inside walls/blocks.
- Rejects positions overlapping the car's main body.
- Rejects positions overlapping the oriented collision helper cells.
- Also tries one block higher when the first candidate is blocked by a step/slab.
- Does not forcibly rotate the player's camera after exiting.

No drivetrain, fuel, clutch, transmission, storage, HUD, sound, or vehicle-model
changes are included.
