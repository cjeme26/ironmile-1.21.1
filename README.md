# Iron Mile

An experimental Fabric vehicle mod for Minecraft 1.21.1.

## Prototype milestone 3

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

The car still temporarily inherits Minecraft's boat passenger/input plumbing,
but the controlling player's acceleration, braking, steering, rolling
resistance and lateral grip are now calculated by Iron Mile. Paddle movement
and sound are disabled. Surface grip now limits acceleration, braking and
steering while controlling how much sideways velocity the tires retain.

## Development

Launch the development client on macOS with `./gradlew runClient`.

Build the distributable mod with `./gradlew build`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
