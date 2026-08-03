# Iron Mile

An experimental Fabric vehicle mod for Minecraft 1.21.1.

## Prototype milestone 6

- The prototype vehicle appears as a long iron box.
- Find **Iron Mile Prototype Car** in the Tools & Utilities creative tab, or use
  `/give @s ironmile:car`.
- Right-click a block with the item to place the car.
- Right-click the car to enter it.
- Drive with W/A/S/D and dismount with Left Shift.
- W accelerates, releasing it coasts, and S brakes before selecting reverse.
- Steering becomes less sensitive at higher speed.
- A small HUD readout shows approximate speed in km/h.
- Four virtual wheel points sample the blocks beneath the vehicle.
- Gravel, dirt, mud, sand, snow, ice and blue ice have different grip.
- The HUD temporarily displays the detected surface and blended grip.
- Cars spawn with all-season tires.
- Hold summer, all-season or winter tires and right-click a car to install them.
- Tire items are reusable during testing, and each car saves its fitted tires.
- The HUD displays the fitted tire type.
- Each virtual wheel independently checks whether active rain can reach it.
- Roofed roads and tunnels stay dry; partial shelter produces mixed conditions.
- Wet grip differs by tire type and is blended across all four wheels.
- The HUD displays Dry, Wet or Mixed road condition.
- A 450 Nm engine now uses an RPM-dependent torque curve.
- A six-speed automatic gearbox shifts up, down and performs kickdown.
- Reverse has its own ratio, and S remains the brake until the car stops.
- Shifts briefly interrupt drive force and show N in the development HUD.
- Engine braking depends on gear, while aerodynamic drag limits top speed.
- The HUD displays automatic gear and engine RPM.

The car still temporarily inherits Minecraft's boat passenger/input plumbing,
but the controlling player's acceleration, braking, steering, rolling
resistance and lateral grip are now calculated by Iron Mile. Paddle movement
and sound are disabled. Surface grip now limits acceleration, braking and
steering while controlling how much sideways velocity the tires retain.
Summer tires favor dry roads, winter tires favor snow and ice, and all-seasons
provide a compromise.

Wetness is intentionally immediate in this milestone: exposed roads are wet
during rain and return to dry as soon as the rain ends. Lingering moisture and
hydroplaning can be added after the exposure and grip model is proven.

The drivetrain targets a heavy road car rather than a sports car. Engine force
is converted through the active gear and final drive, divided by vehicle mass,
then limited by the existing tire, surface and weather grip calculation.

### Milestone 6.1 tuning

- Closer ratios and a shorter final drive target shifts near 49, 78 and 113 km/h.
- Peak torque increased from 300 Nm to 450 Nm, targeting roughly 13 seconds
  from 0–100 km/h on dry road with summer tires.
- Braking is unchanged above 35 km/h but progressively strengthens below that
  speed, reaching its highest assistance close to a stop.

## Development

Launch the development client on macOS with `./gradlew runClient`.

Build the distributable mod with `./gradlew build`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
