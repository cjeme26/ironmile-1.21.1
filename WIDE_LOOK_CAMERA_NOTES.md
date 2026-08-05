# Iron Mile wide-look camera adjustment

Apply this patch after the limited-look camera patch.

Changes:
- increases horizontal first-person viewing to 85 degrees left or right,
- increases upward viewing to 20 degrees,
- still prevents all downward viewing,
- keeps the front-mounted camera position and wall clipping unchanged,
- keeps hands and held items hidden through the existing HeldItemRenderer mixin,
- leaves third-person camera behavior unchanged.
