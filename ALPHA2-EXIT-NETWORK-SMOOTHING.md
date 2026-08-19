# Alpha 2 exit + network smoothing pass

Dismount
- Removes the +1 block vertical fallback from smart dismount positioning.
- Driver-side, passenger-side, rear, and front candidates must now be valid at
  normal ground-level exit height.
- This prevents a blocked side candidate from being accepted on top of the car.

Local driving / client-server reconciliation
- Keeps the server authoritative.
- Replaces the old fixed 4-block one-frame hard correction for routine drift.
- Correction tolerance widens modestly with vehicle speed.
- Routine out-of-tolerance error must be seen on two consecutive server snapshots.
- Routine corrections move only 20% toward the authoritative transform instead
  of teleporting the local car in one frame.
- Truly large desyncs (12+ blocks or extreme yaw error) still snap immediately.

Goal
Reduce the occasional split-second left/right visual pop at higher road speeds
without rewriting the BoatEntity foundation before Alpha 2.

No drivetrain tuning, collision-shape tuning, fuel, storage, transmission, HUD,
or sound changes are included.
