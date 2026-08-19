# UI refinement pass 3

This is another presentation-only pass.

Driving HUD
- Fuel gauge was tightened into a smaller dark cut-corner instrument.
- Manual H-pattern is smaller and uses the same dark cut-corner panel language.
- Automatic P/D/R uses the same panel language and a clear amber selected tile.
- Selected manual gear uses the same amber selected-tile treatment.
- Clutch indicator is gone when the clutch is released.
- While Left Shift is held, a simple amber C appears beside RPM with no box.

Status icons
- Replaced the rough rectangle-built symbols with hand-authored 16x16 pixel masks.
- At common Minecraft GUI scale they appear roughly 32x32 physical pixels.
- Headlight, fuel-pump and check-engine silhouettes are always in fixed positions.
- Inactive state is very dark grey.
- Headlights illuminate green.
- Low fuel illuminates amber.
- Stall/out-of-fuel engine warning illuminates red.
- No background panel behind the icon strip.

Fuel screen
- Fuel-item slot moved left so it aligns directly under the Fuel heading.
- Tank/inventory behavior is unchanged.

No drivetrain, clutch, shifter, fuel-consumption, P/D/R, storage or vehicle
physics behavior is changed.
