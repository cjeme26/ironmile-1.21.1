# Space ignition + automatic parking + PRND + collision cleanup

Controls shared by both transmissions
- W: throttle
- S: brake
- A/D: steering
- Space: start/stop engine
- X: exit vehicle
- H: headlights

Manual
- Left Shift: clutch
- Right-click + mouse: H-pattern shifter with R / N / 1-6
- R/F remain fallback shift up/down
- Newly configured manual cars begin in Neutral

Automatic
- Real selector positions: P / R / N / D
- R: selector upward (D -> N -> R -> P)
- F: selector downward (P -> R -> N -> D)
- W is throttle in both D and R
- S is always brake
- D performs the existing automatic 1-6 shifting internally
- N disconnects the wheels and lets the engine free-rev
- P locks the vehicle
- R and P refuse engagement above about 4 km/h
- HUD shows the whole selector, for example: P R N [D]

Parking simplification
- No player-facing handbrake button.
- Getting out automatically begins parking/holding the car.
- Turning the engine off while nearly stopped applies the invisible parking hold.
- A running car releases that hold automatically when the driver starts to drive.
- Automatic P uses the same internal hold.
- The old handbrake network class can remain in the source tree unused; this patch
  no longer registers or sends it.

CC0 collision
- The rendered CC0 shell is approximately 1.85 blocks wide and 1.50 blocks high.
- The old central entity collision core was 1.8 x 0.9.
- It is now 1.85 x 1.45, so the collision roof sits much closer to the visible roof.
- Minecraft entity boxes are axis-aligned, so the existing oriented bumper probes
  still handle the long front/rear body against blocks.

Storage, clutch/stalling, mouse shifter, tires, lights, suspension, audio, and
authoritative networking are otherwise left in place.
