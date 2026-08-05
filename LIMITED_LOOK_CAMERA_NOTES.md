# Iron Mile limited-look camera adjustment

Apply this patch after the locked front-camera patch.

Changes:
- allows up to 30 degrees of view to the left or right,
- allows up to 12 degrees upward,
- prevents all downward viewing,
- clamps the player's actual look rotation so input cannot build up beyond the limit,
- keeps the front camera position and wall clipping unchanged,
- keeps hands and held items hidden through the existing HeldItemRenderer mixin,
- leaves third-person camera behavior unchanged.
