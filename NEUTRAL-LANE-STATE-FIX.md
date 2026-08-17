# Neutral / vertical-lane state fix

- Keeps the smooth forgiving horizontal movement along Neutral.
- Fixes the bug where vertical movement only moved one tiny step and then snapped back.
- A deliberate vertical gesture near 1/2, 3/4, or 5/6 now ENTERS that vertical rail.
- Once a rail is entered, vertical movement accumulates normally until the knob reaches Neutral again.
- Sideways mouse wobble is ignored while inside a gear rail.
- Small diagonal wobble while on Neutral still behaves as horizontal movement unless the vertical gesture is clearly dominant.
- Reverse/1st left-click toggle remains unchanged.
