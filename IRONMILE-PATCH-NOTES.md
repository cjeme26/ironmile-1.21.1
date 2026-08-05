# IronMile smooth-driving test patch

This patch changes only:

- `src/main/java/com/cjeme26/ironmile/entity/CarEntity.java`
- `src/client/java/com/cjeme26/ironmile/client/IronMileClient.java`

## Apply from the IronMile project root

```bash
ditto -x -k "$HOME/Downloads/IronMile-smooth-driving-patch.zip" .
git diff -- src/main/java/com/cjeme26/ironmile/entity/CarEntity.java \
  src/client/java/com/cjeme26/ironmile/client/IronMileClient.java
./gradlew clean runClient
```

`ditto -x -k` is required for extracting a ZIP. Plain `ditto <zip> .` copies the ZIP file instead of merging its contents.

## What changed

- The server remains the authoritative movement owner.
- Vanilla boat `VehicleMoveC2SPacket` ownership remains disabled.
- The local driver now runs the same car simulation client-side for immediate movement.
- Routine server position and velocity corrections are ignored only while local prediction is active.
- Remote/unoccupied cars still use server interpolation.
- On dismount, the car hands control back to the latest recorded server state.
- WASD input is applied at the start of the client tick before the client world/entity tick.

## Suggested test sequence

1. Drive straight and check for regular tick-to-tick stutter.
2. Steer left/right while accelerating.
3. Drive slowly and quickly into a wall; the bumper should reach the wall without the old pause-away-from-wall effect.
4. Exit while still moving and watch the car for at least 10 seconds.
5. Check slabs, low road edges, reverse, braking, shifting, suspension, engine sound, and HUD values.
6. In multiplayer, verify that another player's car remains acceptably smooth.

## Roll back

Before committing, Git can restore only these files:

```bash
git restore src/main/java/com/cjeme26/ironmile/entity/CarEntity.java \
  src/client/java/com/cjeme26/ironmile/client/IronMileClient.java
```
