# P/D/R automatic selector + intuitive reverse controls

Automatic selector
- Neutral is removed from the player-facing automatic selector.
- Selector order is P -> D -> R.
- F moves P -> D -> R.
- R moves R -> D -> P.
- One tap of F from Park gets the car into Drive.
- P and R are still blocked above roughly 4 km/h.
- The HUD now shows only P D R.

Reverse controls
- In D / forward manual gears:
  - W = throttle
  - S = brake
- In R, for both automatic and manual:
  - S = reverse throttle
  - W = brake

Compatibility
- The numeric selector IDs from the earlier PRND prototype are preserved so
  existing saved automatic cars do not have their P/R/D positions scrambled.
- If an older saved car was left in the old Neutral position, it migrates to
  Park when loaded.

Everything else from the previous cleanup remains in place:
- Space ignition
- automatic invisible parking hold
- manual clutch + H-pattern
- trunk storage
- CC0 collision-size adjustment
