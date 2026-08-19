# UI refinement pass 2

Driving HUD
- Fuel remains upper-left.
- Speed and RPM remain centered.
- Manual H-pattern moved to upper-right.
- Automatic P/D/R remains upper-right.
- Status lamps moved to a fixed bottom-left strip with NO background panel.
- Headlight, low-fuel and engine lamps are always present as very dark grey
  silhouettes; they illuminate only when relevant.
- Headlight icon has been redrawn to read more like a conventional headlamp.

Manual RPM guidance
- Idle in first gear is now green rather than red.
- Low-RPM warnings only happen when the drivetrain is actually loaded and moving.
- Yellow/red high-RPM thresholds have more leeway.
- Clutch-held and Neutral low RPM are normal and stay green.
- High RPM can still warn even with the clutch held.
- A tiny C lamp beside RPM lights amber while the clutch is held, making the
  intentional engine/wheel disconnect visible without treating it as a fault.

Fuel screen
- Fuel Tank is no longer followed immediately by another duplicate Fuel label.
- The top vehicle section is now labelled Tank.
- The fuel-item slot is a distinct Fuel section beneath it.
- Inventory remains in the Minecraft-style light container palette.
- Tank gauge and exact litres remain unchanged mechanically.

No driving, clutch, transmission, fuel-consumption or storage mechanics are
changed by this patch.
