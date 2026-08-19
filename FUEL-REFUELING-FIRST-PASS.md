# Fuel + refueling first pass

Tank
- Hatchback capacity: 45.0 L
- New / previously-unsaved fuel state starts at 11.25 L (25%)
- Fuel is saved with each car
- Fuel amount is tracked to clients for the HUD and refueling screen

Consumption
- Engine off: zero fuel use
- Idling: small fuel use
- Higher RPM: more fuel use
- Throttle: largest additional fuel use
- Fuel consumption is server-authoritative
- Empty tank shuts the engine off
- Space cannot restart the engine until fuel is added
- HUD shows OUT OF FUEL when empty

Prototype refueling
- Stand beside a rear quarter of the temporary CC0 hatchback
- Hold Shift and right-click the car
- This opens the compact Fuel Tank screen
- Shift + right-click elsewhere still opens the trunk
- Either rear side is accepted for now because the CC0 model has no visible fuel door

Fuel screen
- One fuel-input slot
- Coal and charcoal are accepted as temporary prototype fuels
- 1 coal/charcoal = 5.0 L
- Whole fuel items are consumed only when the tank has room for their full 5.0 L
- Extra items remain in the input slot and are dropped back into the world when
  the screen closes, so a full tank never silently eats fuel
- No output slot, furnace arrow, flame icon, or smelting progress bar
- Exact litres are visible only while refueling
- Player inventory is included so fuel can be dragged into the slot normally

Driving HUD
- Small segmented F-to-E fuel gauge in the top-left
- Exact litres are deliberately not shown while driving

This is still prototype fuel. Coal/charcoal are test inputs so the tank,
consumption, UI, persistence, and empty-tank behavior can be developed before
adding gasoline/diesel items, cans, pumps, or station infrastructure.
