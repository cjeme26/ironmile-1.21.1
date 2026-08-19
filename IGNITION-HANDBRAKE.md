# Ignition + handbrake

Controls
- I: toggle ignition
- Space: toggle handbrake
- Both are normal remappable Minecraft keybindings.

Ignition
- Cars spawn/old unsaved-state cars default to engine OFF.
- Starting takes 14 ticks (~0.7 seconds).
- HUD shows STARTING... during the starter delay.
- Engine RPM is 0 while off/starting.
- W/S cannot provide engine power while the engine is off.
- Steering and normal braking still work while rolling with the engine off.
- Leaving a running car keeps its engine running and its engine sound audible.
- Engine state is saved with the vehicle.

Handbrake
- Cars default to handbrake ON.
- Space is consumed while the player is the controlling driver, so Minecraft's normal Space action does not also trigger.
- Handbrake state belongs to the car and is saved.
- It works with the engine on or off.
- At low speed it locks the car to rest; while moving it applies a strong speed-proportional brake.
- It also strongly damps sideways motion.
- Acceleration does NOT automatically release it.
- The previous magic auto-parking behavior on dismount is removed. If you leave the handbrake off, the car is allowed to roll.

HUD
- ENGINE OFF while stopped/off.
- STARTING... while starting.
- (P) whenever the handbrake is engaged.

Existing manual shifter, reverse toggle, R/F fallback, headlights, drivetrain tuning, and CC0 model are otherwise left alone.
