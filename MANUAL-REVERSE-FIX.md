# Manual reverse pull-away fix

The engine could respond to S in manual Reverse while the invisible parking hold
still suppressed the car's movement later in the same physics tick.

The parking hold now releases before it is applied whenever the seated driver
has the engine running and presses the valid throttle for the selected direction.

Manual controls remain:
- Forward gear: W throttle, S brake
- Reverse: S throttle, W brake
- Left Shift: clutch

Automatic remains P / D / R.
