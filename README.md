# IronMile

IronMile is a vehicle mod focused on making driving in Minecraft more involved. Cars are affected by their transmission, tires, road surface, weather, fuel, and other driving conditions rather than functioning as simple mounts.

The core vehicle systems are playable, but the mod is still in active alpha development.

#### Download and installation

Official downloads are available only through Modrinth. GitHub is used for source code and development. Development files and source code on GitHub may be incomplete, unstable, or changed without notice.

#### Requirements

Minecraft 1.21.1, Fabric Loader and Fabric API

#### What's included?

Manual and automatic cars.

A manual transmission with a functional clutch, individual gears, engine stalling, and a gear stick that can be operated using the mouse.

Automatic transmission behavior for players who prefer simpler driving.

Different tire types, including Summer, All-Season, and Winter Tires.

Surface and weather-dependent grip. Tires perform differently depending on the blocks underneath the vehicle and whether the road is wet or dry.

Individual wheel grip calculations, allowing different wheels to experience different surfaces or weather conditions.

Vehicle fuel and refueling.

18-slot vehicle storage accessible from the rear of the car.

The Mechanic's Workbench, used to construct vehicle bodies, craft parts, and assemble finished cars.

Replaceable Tire Sets that can be changed after a vehicle has been assembled.

Vehicle HUD information including speed, RPM, gear, fuel, surface, and driving conditions.

#### Driving and controls

**General**

W: Accelerate  
S: Brake / Reverse  
A/D: Steer  
Space: Start or stop the engine  
R/F: Change the selected gear

**Automatic cars**

Press Space to start the engine. The transmission changes gears automatically while driving. R and F can also be used to manually change the selected gear.

**Manual cars**

Hold Shift to operate the clutch.

To start the engine, hold Shift and press Space. Keep the clutch held while selecting 1st gear, begin accelerating with W, and release the clutch to start moving.

Hold right-click on the gear stick and move the mouse through the shift pattern to select a gear. Release right-click while the stick is positioned inside the desired gear.

R and F can also be used to change gears.

When changing gears while driving, hold the clutch, select the next gear, and then release the clutch. Allowing the RPM to fall too low can stall the engine.

#### Tires, surfaces and weather

IronMile currently includes Summer, All-Season, and Winter Tires.

Summer Tires provide the highest grip on dry roads. All-Season Tires provide more consistent performance across wet roads and several off-road surfaces. Winter Tires provide substantially more grip on snow and ice.

Grip is calculated separately underneath each wheel. This means a vehicle can react to multiple surfaces at once.

Rain also affects grip. Wheels exposed to rain use wet grip values, while wheels underneath cover remain dry. A vehicle can therefore have both wet and dry wheels at the same time.

Grip affects acceleration, braking, steering, and how easily the vehicle slides.

#### Mechanic's Workbench

Vehicles and their components are constructed using the Mechanic's Workbench.

The workbench contains three pages:

**Body:** Construct the vehicle body.

**Parts:** Craft tires, Tire Sets, and manual or automatic transmissions.

**Assembly:** Combine a vehicle body, transmission, and Tire Set into a finished car.

The workbench includes its own recipe system and can automatically place available materials into recipes.

#### Fuel and storage

Newly constructed vehicles begin with an empty fuel tank.

Shift + right-click the side of the vehicle to access the fuel interface. Coal, charcoal, and coal blocks can currently be used as fuel.

1 Coal = 5 liters of fuel.

Right-click the rear of the vehicle to access its 18-slot storage.

#### Known alpha limitations

IronMile is still in active development.

Vehicle physics, handling, balancing, recipes, models, animations, sounds, interfaces, and other mechanics may change throughout the alpha.

Some mechanics are still being refined and bugs should be expected.

Additional vehicles, vehicle components, customization, and driving mechanics are planned for future development.

## License

All rights reserved. IronMile is proprietary software. See the LICENSE file for the complete terms.
