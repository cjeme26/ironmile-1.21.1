# Neutral slot checkpoints

- Keeps the forgiving side-entry into Neutral.
- Left / center / right Neutral now behave like soft checkpoints.
- One raw mouse movement can move only one Neutral slot at a time.
- Reaching center Neutral discards the leftover sideways movement from that same sample, so a 2 -> 3 shift is much less likely to jump straight to the 5/6 neutral position.
- Moving from center Neutral to right Neutral still works with continued intentional sideways movement.
- The horizontal travel threshold was raised slightly from 0.135 to 0.17 for better control.
- Vertical gear entry and the Reverse / 1st toggle are unchanged.
