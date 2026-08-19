# Forgiving assisted clutch

Controls
- Left Shift: hold clutch
- X: exit IronMile vehicle
- V: ignition
- Space: handbrake
- Right click: manual H-pattern shifter
- R/F: fallback sequential shifting

Important control change
Minecraft normally uses Left Shift to dismount. Manual IronMile cars now consume
Sneak/Shift for the clutch, so X is the explicit remappable Exit Vehicle key.

Clutch
- Pressing Shift disconnects engine and wheels immediately.
- Releasing Shift reconnects them over roughly 0.65 seconds.
- Torque passes through a broad assisted bite point instead of an instant binary
  keyboard clutch.
- Clutch fully down removes propulsion and engine braking from the wheels.
- Neutral or clutch-down operation lets RPM separate from road speed.

Stalling
- Stopping in a selected manual gear with the clutch released and no matching
  throttle can stall the engine.
- The low-speed stall zone has a generous grace period.
- Holding the clutch while stopping prevents the stall.
- HUD shows STALLED.
- V restarts the engine.

Deliberately not included yet
- clutch wear
- clutch temperature
- clutch HUD meter
- harsh gearbox damage

The goal of this pass is to find out whether clutching actually makes the manual
car more satisfying before adding deeper simulation.
