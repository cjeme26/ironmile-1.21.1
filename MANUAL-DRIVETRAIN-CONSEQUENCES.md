# Manual drivetrain consequences

This patch changes drivetrain behavior, not the mouse shifter UI.

Manual hatchback:
- Never runs the automatic transmission shift routine.
- Starting in 2nd is weaker than 1st.
- Starting in 3rd/4th/5th/6th becomes progressively more bogged down.
- The high-gear launch penalty fades as the car reaches a sensible road speed for that gear.
- Lower gears have stronger engine braking when the accelerator is released.
- Power fades during the last ~450 RPM before the rev limiter instead of behaving like an on/off wall.
- The selected gear remains entirely player-controlled.

Automatic hatchback:
- Full-throttle upshift target is reduced from 5500 RPM to 4200 RPM.
- Downshift target is raised slightly from 1600 RPM to 1750 RPM.
- This is intended to better match the small hatchback's short gearing.

The current mouse shifter, Reverse lockout, HUD, R/F fallback keys, and item presentation are untouched.
