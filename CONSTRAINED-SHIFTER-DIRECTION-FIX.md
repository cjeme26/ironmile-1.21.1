# Constrained shifter direction fix

- Fixed the vertical mouse direction so dragging down moves the lever down and dragging up moves it up.
- Preserves leftover vertical movement when crossing neutral.
- A single continuous gesture can now travel 1 -> N -> 2, 2 -> N -> 1, 3 <-> 4, and 5 <-> 6.
- Slightly reduced the vertical deadzone when leaving neutral.
- The constrained H-track, reverse push-down latch, R/F fallback, HUD, and left-click world-action suppression remain unchanged.
