# UI icon + padding correction

Status icons
- Replaces the Java rectangle/mask drawings with actual transparent 16x16 PNG
  texture assets.
- At common Minecraft GUI scaling these appear around 32x32 physical pixels.
- Headlights: dark grey off, green on.
- Low fuel: dark grey normal, amber at <=15%.
- Check engine: dark grey normal, red when stalled or out of fuel.
- No background behind the status strip.

Fuel HUD
- Adds 2 px of top padding above F/E without moving the bottom edge.

Manual H-pattern
- Adds 2 px of panel padding above 1/3/5.
- Adds 2 px of panel padding below 2/4/6.
- Gate geometry and shifting behavior are unchanged.

Fuel Tank GUI
- Moves the fuel input slot one pixel left so it lines up with the Fuel heading
  and the normal inventory left margin.

No vehicle mechanics are changed.
