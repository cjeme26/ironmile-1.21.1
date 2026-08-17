# Constrained-path manual shifter

This pass replaces the old free-moving cursor/wall model with an H-pattern state machine.

- The lever can only move along legal H-pattern paths.
- While inside a gear gate, sideways mouse movement cannot make the knob jump or bounce through a wall.
- The stick must return to neutral before it can travel sideways to another column.
- Large/fast mouse movements are broken into small routed steps, so the game follows the most sensible legal path instead of teleporting the knob.
- When moving vertically from neutral, the game gently chooses the nearest 1/2, 3/4, or 5/6 lane and performs the last tiny alignment automatically.
- Entering deeply enough into a gear gate snaps the final part into the detent and plays a small mechanical click.
- Reverse is still the upper-left gate, but left click now LATCHES the push-down reverse lockout for the entire shift. Button release order no longer matters.
- Pulling an existing Reverse gear back to neutral releases the reverse lockout again.
- The clean 1/2, 3/4, 5/6 HUD layout remains.
- R/F remain fallback sequential shift controls.
