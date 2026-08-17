# IronMile Alpha 2 - Stage 1 Cleanup

Stage 1 establishes the current Yellow Hatchback as the clean gameplay baseline before the Alpha 2 vehicle systems are added.

## Changes

- Removed prototype driving telemetry from the normal in-game HUD:
  - surface name
  - grip percentage
  - wet/dry road condition
  - current tire type
  - headlight on/off debug line
- Kept speed, current gear, and RPM visible because they are core driving information and will be expanded by the transmission work in Stage 2.
- Removed remaining "prototype initialized" wording from runtime logging and the main car class description.
- No handling, drivetrain, camera, tire, lighting, sound, entity, recipe, or control behavior was intentionally changed in this stage.

## Next

Stage 2: shared manual/automatic transmission system and the mouse-controlled H-pattern manual shifter.
