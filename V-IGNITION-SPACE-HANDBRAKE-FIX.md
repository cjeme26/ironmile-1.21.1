# V ignition + direct Space handbrake fix

- Ignition default moved from I to V.
- Removed the separate Fabric handbrake keybinding.
- Handbrake now directly reuses Minecraft's actual Jump key.
- While controlling an IronMile car:
  - Space toggles the handbrake once per press.
  - Holding Space does not repeatedly toggle.
  - Vanilla jump / mounted Space behavior is consumed.
- Outside the driver's seat, Space behaves normally again.
- Ignition/handbrake networking and saved vehicle state are unchanged.
