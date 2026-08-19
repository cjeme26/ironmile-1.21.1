# Trunk storage compile fix

Minecraft/Fabric 1.21.1's BoatEntity does not expose the dropItems(DamageSource)
override used in the first trunk patch.

This patch removes only that invalid override and its unused imports.

The core trunk feature remains:
- 9 slots
- Shift + right-click to open
- persistent vehicle NBT storage
- normal right-click still enters the car

Vehicle-destruction item scattering is intentionally deferred until we hook the
correct 1.21.1 destruction method rather than guessing another superclass API.
