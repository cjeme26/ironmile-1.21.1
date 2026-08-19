# Zero-fuel + fuel persistence fix

New cars
- Newly assembled / newly placed car items now start with 0.0 L fuel.
- The old 11.25 L (25%) prototype starting fuel is removed.
- Existing placed cars that already saved a fuel value keep that saved value.

Breaking and replacing a car
- The current fuel amount is written onto the dropped IronMile car item.
- Placing that item restores the same fuel amount to the new CarEntity.
- Tire type and transmission preservation from the previous pickup patch remain.
- Old car items that do not yet contain a stored-fuel field safely default to 0 L.

Example:
- Car has 17.4 L
- Break with pickaxe
- Pick up car item
- Place it again
- Car returns with 17.4 L
