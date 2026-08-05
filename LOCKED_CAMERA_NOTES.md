# Iron Mile locked front-camera adjustment

Apply this patch after the earlier front-camera and hide-driver patches.

Changes:
- moves the front camera slightly backward (2.08 -> 1.78 blocks),
- raises it slightly (1.12 -> 1.30 blocks),
- locks first-person yaw to the car's interpolated yaw,
- locks first-person pitch level at 0 degrees,
- hides both hands and held items while riding the car,
- retains the front-camera wall clipping behavior,
- leaves third-person camera behavior unchanged.
