# Manual reverse routing fix

Root cause found:

The manual drivetrain checked `pressingBack` before checking which gear was
selected. Since S is Minecraft's `pressingBack` input, pressing S in Reverse
was always caught by the generic brake branch. The actual Reverse acceleration
code was unreachable.

This patch routes the input by selected gear first:

- R: S throttle, W brake
- N: no drivetrain torque, S can brake
- 1-6: W throttle, S brake

The parking-hold release fix from the previous patch remains in place.
