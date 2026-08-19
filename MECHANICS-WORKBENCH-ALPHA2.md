# Iron Mile Alpha 2 - Mechanic's Workbench

This pass adds the first dedicated Iron Mile vehicle construction system.

## Workbench

The Mechanic's Workbench is crafted in a normal 3x3 crafting table:

    I I I
    P C P
    P P P

I = Iron Ingot
P = any Planks
C = Crafting Table

For Alpha 2 the placed block deliberately reuses the normal crafting-table look.

## Pages

Body
- 3 high x 4 long crafting grid.
- Hatchback Body recipe:

    G I I G
    I F C I
    B I I B

G = Glass Pane
I = Iron Ingot
F = Furnace (engine)
C = Chest (trunk/storage)
B = Iron Bars

Parts
- 3x3 crafting grid.
- Existing Summer / All-Season / Winter Tire recipes live here.
- 4 matching Tires -> 1 matching Tire Set.
- 1 Tire Set -> 4 matching Tires.
- Manual Transmission:

    . I .
    G F G
    . I .

- Automatic Transmission:

    . R .
    G F G
    . D .

G = Gold Ingot
F = Furnace
R = Redstone Dust
D = Diamond

Assembly
- Dedicated Body + Transmission + Tire Set input slots.
- Manual Transmission outputs the manual Yellow Hatchback.
- Automatic Transmission outputs the automatic Yellow Hatchback.
- The chosen Tire Set is stored on the resulting car item.

## Recipe book

Every page has an always-visible Iron Mile recipe catalogue.
There is no recipe discovery/unlock system.
Body and Parts recipe icons can be clicked to auto-fill the grid when the player
has the materials. Missing-material recipes remain visible but dimmed.
The Assembly recipe is informational because transmission and tire type are
intentional choices.

## Tire Sets and car items

- Individual tires now stack to 16.
- Finished car item tooltips show the installed tire type.
- Placing the car transfers that tire type to the CarEntity.
- Right-clicking a placed car with a different Tire Set installs it, drops the
  old Tire Set, consumes the new set in survival, and plays a short mechanical sound.
- A single individual tire no longer replaces all four tires on a placed car.

## Legacy recipes

The old normal-crafting-table tire and direct Yellow Hatchback recipes are kept
as data files only for patch-overwrite compatibility, but are disabled with a
Fabric resource condition. Vehicle-specific crafting therefore lives in the
Mechanic's Workbench.
