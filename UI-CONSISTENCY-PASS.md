# IronMile UI consistency pass

This patch changes presentation only. Vehicle physics, fuel consumption,
transmission behavior, clutch feel, storage, and refueling logic are untouched.

Driving HUD
- Speed remains the dominant centered readout.
- RPM is now always directly beneath speed.
- Manual RPM uses mechanical-condition colors:
  - green: comfortable operating range
  - yellow: consider shifting / approaching an uncomfortable range
  - red: very low loaded RPM or near the rev limiter
- Neutral, clutch-in, starting, and engine-off RPM are muted rather than treated
  as a gear recommendation.
- ENGINE OFF no longer permanently occupies the HUD.
- STARTING..., STALLED, and OUT OF FUEL remain attention states.
- Fuel and automatic P/D/R now use matching subtle translucent backing panels.
- Automatic selected P/D/R position uses the shared amber selection accent.

Manual shifter
- Existing H-pattern geometry and mouse behavior are unchanged.
- Current engaged gear is highlighted.
- The resting selector knob uses the same amber selection accent.
- Reverse lockout still retains its established red R treatment.

Dashboard indicators
- Small pixel-style indicator strip appears only when needed.
- Green headlight indicator when headlights are on.
- Amber low-fuel pump indicator at 15% fuel or less.
- Red engine indicator for a stall or an empty-fuel shutdown.
- No traction-control icon yet because this pass does not add new traction-loss
  mechanics.

Fuel screen
- Player inventory and fuel slot now use a vanilla-Minecraft-inspired light grey
  container palette and familiar recessed slot treatment.
- IronMile's amber segmented fuel gauge remains the vehicle-specific accent.
- Permanent "Coal / Charcoal • 5.0 L each" helper text is removed from the main
  screen so it feels less like a debug/prototype panel.
- Exact litres remain visible while refueling.

Visual language established by this pass
- white: primary driving information
- amber: fuel and selection
- green / yellow / red: mechanical operating condition
- grey: inactive / secondary
- Minecraft container colors: inventories and item handling
- dark translucent panels: in-world vehicle instrumentation
