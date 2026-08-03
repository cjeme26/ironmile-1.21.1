# Iron Mile

An experimental Fabric vehicle mod for Minecraft 1.21.1.

## Prototype milestone 1

- The prototype vehicle appears as a long iron box.
- Find **Iron Mile Prototype Car** in the Tools & Utilities creative tab, or use
  `/give @s ironmile:car`.
- Right-click a block with the item to place the car.
- Right-click the car to enter it.
- Drive with W/A/S/D and dismount with Left Shift.

The first milestone intentionally uses Minecraft's boat movement internally.
This validates spawning, mounting, input, rendering and multiplayer tracking.
Custom road handling will replace it in the next physics milestone.

## Development

Launch the development client on macOS with `./gradlew runClient`.

Build the distributable mod with `./gradlew build`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
