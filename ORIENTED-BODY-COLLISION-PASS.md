# Oriented body collision replacement pass

This replaces the previous three-large-box approach.

Why the old pass looked wrong
Minecraft entity AABBs do not rotate. The previous center/front/rear boxes were
large enough that their square corners extended beyond the visible hatchback,
especially from the side and at diagonal angles.

New approach
The car now uses a grid of small collision cells positioned in the car's local
forward/right coordinate system:
- 4 cabin/roof cells
- 4 hood/front cells
- 4 rear/hatch cells

Each cell is only 0.72 blocks wide. The cells are placed in two side columns and
several front-to-rear rows, and their positions rotate with the car. This is an
AABB approximation rather than a true rotating collision mesh, but the much
smaller cells substantially reduce invisible side/front corners while preserving
solid hood, roof and rear surfaces.

The main CarEntity collision core is also reduced to 0.72 blocks so it no longer
reintroduces the old oversized square body.

No changes to driving, transmission, clutch, fuel, storage, HUD or the visual
CC0 model.
