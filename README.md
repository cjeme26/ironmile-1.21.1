# Iron Mile

## Milestone 9: visual vehicle lighting

- H toggles two front headlights through a server-validated network payload.
- Headlight state is tracked, synchronized, saved with the car, restored after
  world reload, and displayed on the driving HUD.
- Rear brake lamps brighten automatically during active braking.
- White reversing lamps activate when reverse is engaged.
- Lamps follow suspension pitch and roll as parts of the rendered body.
- This milestone is visual only and does not yet cast light onto blocks.

## Milestone 8.3: audible idle and smoother RPM transitions

- The source loop moves into an audible low-frequency range at idle and no
  longer uses amplitude modulation that could resemble microphone flutter.
- Idle and overall volume are restored while throttle remains distinct.
- RPM pitch changes are slower and narrower, and automatic shift dips are
  gentler to reduce occasional resampling chatter during acceleration/coasting.

## Milestone 8.2: stable in-car engine audio

- The driver's engine sound is listener-relative while riding, eliminating
  positional timing jitter between the moving camera and moving sound source.
- Cars heard by other players remain spatial sounds attached to their world
  positions and still fade naturally with distance.
- RPM pitch, throttle volume, shift dips, reverse tone, and fades are unchanged.

## Milestone 8.1: smoother high-RPM engine tone

- The synthetic source loop now emphasizes its low fundamental and uses fewer,
  quieter upper harmonics.
- Maximum pitch is reduced while retaining a clear rise from idle to redline.
- Spatial audio, throttle volume, shift dips, reverse tone, and fades are
  unchanged from Milestone 8.

## Milestone 8: RPM-based engine audio

- Occupied cars emit a spatial looping engine sound audible within 48 blocks.
- Pitch follows engine RPM smoothly, with distinct idle and redline character.
- Throttle increases volume; automatic shifts briefly dip pitch and volume;
  reverse has a slightly lower tone.
- Sounds follow moving cars and fade in and out without abrupt clicks.
- The included engine loop is an original synthetic Iron Mile placeholder.

## Milestone 7.4: short-drop road adhesion

- Before each collision section, five points beneath the physical car search
  for road support within 0.6 blocks.
- After leaving a slab, the car settles onto the nearby lower road instead of
  remaining briefly airborne and striking the next slab during descent.
- The predictive step check now includes the centre of the leading edge and
  can recover a descending car that is slightly below the next slab surface.
- Drops deeper than 0.6 blocks remain real falls; full blocks remain walls.

## Milestone 7.3: predictive road-edge stepping

- The physical car now samples the road height just ahead of both leading
  corners before each short movement section.
- A support rise of at most 0.6 blocks lifts the collision box before impact,
  preserving momentum across bottom slabs at road speed.
- A full block exceeds the clearance limit and is left to ordinary collision,
  so walls still stop the car.
- This replaces the post-impact retry from 7.2; drivetrain tuning is unchanged.

## Milestone 7.2: explicit low-obstacle clearance

- When normal collision stepping rejects a slab, a grounded car now tests the
  blocked remainder of its movement from 0.6 blocks higher.
- Bottom slabs clear that test and the car settles onto them immediately.
- Full blocks remain too tall and still stop the car normally.
- The fallback operates only after a real horizontal collision and does not
  change drivetrain speed, acceleration, braking, grip, or visual suspension.

## Milestone 7.1: reliable slab collision at speed

- Fast movement is resolved in short collision steps instead of one movement
  spanning more than a block.
- Slab step-up detection is therefore evaluated close to the obstacle and
  remains reliable on longer, higher-speed approaches.
- The summed movement per tick is unchanged, so acceleration, maximum speed,
  braking, grip, and suspension rendering retain their Milestone 7 tuning.

## Milestone 7: visual suspension and body movement

- The four virtual wheel positions now sample the height of collision shapes,
  including slabs and other partial blocks.
- The body pitches gradually on hills, rolls over uneven ground, leans subtly
  while cornering, and reacts to acceleration and braking.
- Vertical suspension travel softens the visual jolt when the collision box
  climbs slabs and other low road edges.
- All movement is visual and damped. Collision, drivetrain, braking, tire grip,
  wet weather, automatic stopping, and the 0-100 tune remain unchanged.

## Milestone 6.5: low-speed automatic stopping

- Releasing the controls now produces progressively stronger drivetrain drag
  below roughly 25 km/h while preserving natural coasting at road speeds.
- Reverse receives additional off-throttle resistance and no longer glides for
  an implausibly long distance after releasing S.
- The transmission settles the car to a complete stop below roughly 2 km/h,
  preventing endless near-zero rolling.
- Acceleration, active braking, steering, grip, wet weather, and slab clearance
  are unchanged.

## Milestone 6.4: slab clearance

- Cars can step up 0.6 blocks, allowing them to drive onto slabs, paths, and
  similarly low road edges.
- Full blocks remain walls, so the car cannot unrealistically climb terrain.
- Milestone 6.3 drivetrain, braking, steering, grip, and ground-contact tuning
  are unchanged.

An experimental Fabric vehicle mod for Minecraft 1.21.1.

## Milestone 10: animated PSX hatchback

- Replaced the iron-block prototype body with GGBotNet's CC0 Car 03 hatchback.
- Added a lightweight OBJ mesh reader for the original low-poly geometry.
- Removed the model's embedded wheels and render four standalone wheel meshes
  around independent animation pivots.
- Every wheel rotates from the car's signed road speed, including reverse.
- The front pair smoothly follows the driver's steering input up to 28 degrees.
- Wheel animation is interpolated between game ticks for smooth motion.
- Existing suspension pitch, body roll, dynamic headlight illumination, visible
  lamps, engine audio, grip, weather, tires and automatic transmission remain.
- Lamp geometry was repositioned onto the hatchback's front and rear clusters.
- The model is normalized to approximately 1.85 blocks wide and 3.9 blocks long.
- An editable separated Blockbench source is retained under
  `development_assets/hatchback` alongside the original CC0 source files.
- Third-party source and licensing information is recorded in
  `ASSET_CREDITS.md`.

### Milestone 10.0.1 render correction

- OBJ triangles are emitted as degenerate quads to match Minecraft's entity
  cutout render layer, preventing unrelated faces from being joined into large
  black and yellow shards.
- Front, brake and reversing lamp overlays were reduced and moved closer to the
  hatchback's original textured lamp clusters.

### Milestone 10.1 model integration

- Replaced the temporary block-shaped lamps with emissive texture overlays
  made for the Car 3 UV map.
- Headlights, brake lights and reversing lights can illuminate independently
  or in combination.
- Kept LambDynamicLights responsible for seamless environmental illumination.
- Added fixed door-side dismount candidates so looking toward the bonnet no
  longer places the player inside it.
- Moved the driver attachment point into the compact hatchback cabin.
- Centred the rendered chassis between four sampled wheel-support heights to
  reduce slab hovering while retaining restrained pitch and roll.

### Milestone 10.1.1 collision and synchronization

- Added three orientation-aware collision probes across the active front or
  rear bumper, matching the long visual body without creating a four-block-wide
  square hitbox.
- Bumper probes ignore climbable road edges but stop at full-height walls.
- Moved road-support sampling to the hatchback's visible wheelbase.
- Added explicit driver-state synchronization so the server retains the latest
  validated car position and yaw before dismounting.

## Milestone 9.2: smooth dynamic headlights

- Headlight illumination now uses LambDynamicLights instead of temporary
  light blocks in the world.
- An invisible, non-colliding light anchor follows the front bumper whenever
  the headlights are switched on.
- Lighting moves smoothly with the rendered car and continues illuminating a
  wall when the bumper is close to it.
- Switching the headlights off or removing the car removes the light anchor.
- The anchor cannot be obtained, summoned, saved, attacked, or seen.
- The previous temporary-light block remains registered only so worlds opened
  after Milestone 9.1 can safely clean up any old light positions.

LambDynamicLights is a client-side recommended dependency. The development
client downloads version 4.8.10 for Minecraft 1.21.1 automatically. Players
using a built Iron Mile jar should install LambDynamicLights separately.

### Milestone 9.2.1 correction

- The light anchor is no longer marked with Minecraft's invisible entity flag,
  because LambDynamicLights excludes entities carrying that flag.
- It remains completely unseen because its registered renderer draws nothing.

## Milestone 9.1: road illumination

- Switched-on headlights now cast real Minecraft light onto the road ahead.
- Three invisible light points follow the car to give the beam useful reach.
- Walls and other occupied blocks stop the projected light positions.
- Previous light points remove themselves shortly after the car moves, the
  headlights are switched off, or the car is removed.
- The temporary light has no collision, outline, item, or drops and cannot
  replace road blocks, walls, fluids, or other occupied spaces.
- Milestone 9's visible front, brake, and reversing lamps remain unchanged.

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
- A 210 Nm engine now uses an RPM-dependent torque curve.
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

### Milestone 6.2 tuning

- Closer ratios and a shorter final drive target shifts near 49, 78 and 113 km/h.
- Corrected wheel force to divide wheel torque by wheel radius. This missing
  conversion was the reason unrealistically large engine torque still felt slow.
- Retuned to a believable 210 Nm, 1,500 kg everyday automatic car, targeting
  roughly 9.5 seconds from 0–100 km/h on dry road with summer tires.
- Base braking is about 17% stronger at every speed and assistance progressively
  increases below 35 km/h, reaching a 90% boost close to a stop.

### Milestone 6.3 ground-contact correction

- Added a small downward grounding force so Minecraft maintains tire contact on
  consecutive ticks. Previously, zero vertical movement could alternate the car
  between grounded and airborne states, roughly halving drivetrain updates.

## Development

Launch the development client on macOS with `./gradlew runClient`.

Build the distributable mod with `./gradlew build`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
