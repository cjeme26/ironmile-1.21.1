package com.cjeme26.ironmile.entity;

import java.util.Arrays;

/**
 * Per-vehicle configuration for IronMile.
 *
 * <p>The two prototype specs intentionally use the same current CC0 hatchback
 * model and the same driving values. For now, the only difference is whether
 * the transmission is automatic or manual.</p>
 *
 * <p>The current IronMile car does not have one hard-coded "acceleration"
 * number. Acceleration is calculated from the engine torque curve, gear ratio,
 * final drive, drivetrain efficiency, wheel radius, vehicle mass and grip, so
 * those existing values are preserved here instead.</p>
 */
public record VehicleSpec(
        String id,
        String modelKey,
        TransmissionType transmissionType,
        double maxForwardSpeed,
        double maxReverseSpeed,
        double brakeForce,
        double rollingResistance,
        double lateralVelocityRetained,
        float maxSteeringPerTick,
        double[] gearRatios,
        double reverseRatio,
        double finalDriveRatio,
        double wheelRadiusMetres,
        double vehicleMassKg,
        double drivetrainEfficiency,
        double aerodynamicDrag,
        double engineBrakeForce,
        double lowSpeedCoastThreshold,
        double lowSpeedCoastBrake,
        double reverseCoastBrakeMultiplier,
        double automaticStopSpeed,
        double idleRpm,
        double downshiftRpm,
        double kickdownRpm,
        double economyUpshiftRpm,
        double upshiftRpm,
        double redlineRpm,
        double revLimiterRpm,
        int shiftDurationTicks
) {
    public enum TransmissionType {
        AUTOMATIC,
        MANUAL
    }

    /*
     * The current renderer's CC0 hatchback resources. Both prototype vehicle
     * specs use MODEL_KEY_HATCHBACK, so they can render with this same model
     * while transmission logic is being implemented.
     */
    public static final String MODEL_KEY_HATCHBACK = "hatchback";
    public static final String HATCHBACK_BODY_MESH = "/assets/ironmile/models/entity/hatchback_body.obj";
    public static final String HATCHBACK_WHEEL_MESH = "/assets/ironmile/models/entity/wheel.obj";
    public static final String HATCHBACK_BODY_TEXTURE = "ironmile:textures/entity/hatchback_yellow.png";
    public static final String HATCHBACK_WHEEL_TEXTURE = "ironmile:textures/entity/wheel.png";
    public static final String HATCHBACK_HEADLIGHT_TEXTURE = "ironmile:textures/entity/car3_lights_headlights.png";
    public static final String HATCHBACK_BRAKE_LIGHT_TEXTURE = "ironmile:textures/entity/car3_lights_brake.png";
    public static final String HATCHBACK_REVERSE_LIGHT_TEXTURE = "ironmile:textures/entity/car3_lights_reverse.png";

    /*
     * Same six forward ratios currently used in CarEntity. Unlike CarEntity's
     * old array, this array does not need a dummy element at index 0; use
     * gearRatio(1) through gearRatio(6).
     */
    private static final double[] CURRENT_FORWARD_GEAR_RATIOS = {
            6.25, 3.70, 2.35, 1.78, 1.50, 1.25
    };

    /** Current IronMile hatchback, retaining the existing automatic behavior. */
    public static final VehicleSpec HATCHBACK_AUTOMATIC = currentHatchback(
            "hatchback_automatic",
            TransmissionType.AUTOMATIC
    );

    /** Same CC0 hatchback and physics, intended for manual-shift testing. */
    public static final VehicleSpec HATCHBACK_MANUAL = currentHatchback(
            "hatchback_manual",
            TransmissionType.MANUAL
    );

    public VehicleSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Vehicle id cannot be blank");
        }
        if (modelKey == null || modelKey.isBlank()) {
            throw new IllegalArgumentException("Vehicle model key cannot be blank");
        }
        if (transmissionType == null) {
            throw new IllegalArgumentException("Transmission type cannot be null");
        }
        if (gearRatios == null || gearRatios.length == 0) {
            throw new IllegalArgumentException("A vehicle needs at least one forward gear");
        }

        gearRatios = Arrays.copyOf(gearRatios, gearRatios.length);
    }

    @Override
    public double[] gearRatios() {
        return Arrays.copyOf(gearRatios, gearRatios.length);
    }

    private static VehicleSpec currentHatchback(String id, TransmissionType transmissionType) {
        return new VehicleSpec(
                id,
                MODEL_KEY_HATCHBACK,
                transmissionType,

                // Existing CarEntity road-speed / handling values.
                1.95,       // MAX_FORWARD_SPEED (blocks/tick)
                0.35,       // MAX_REVERSE_SPEED (blocks/tick)
                0.021,      // BRAKE_FORCE
                0.992,      // ROLLING_RESISTANCE
                0.42,       // LATERAL_VELOCITY_RETAINED
                2.6F,       // MAX_STEERING_PER_TICK

                CURRENT_FORWARD_GEAR_RATIOS,
                3.20,       // REVERSE_RATIO
                4.00,       // FINAL_DRIVE_RATIO
                0.34,       // WHEEL_RADIUS_METRES
                1500.0,     // VEHICLE_MASS_KG
                0.86,       // DRIVETRAIN_EFFICIENCY
                0.00030,    // AERODYNAMIC_DRAG
                0.00055,    // ENGINE_BRAKE_FORCE
                0.35,       // LOW_SPEED_COAST_THRESHOLD
                0.0024,     // LOW_SPEED_COAST_BRAKE
                1.5,        // REVERSE_COAST_BRAKE_MULTIPLIER
                0.025,      // AUTOMATIC_STOP_SPEED

                800.0,      // IDLE_RPM
                1600.0,     // DOWNSHIFT_RPM
                2300.0,     // KICKDOWN_RPM
                2600.0,     // throttle-release economy upshift threshold
                5500.0,     // UPSHIFT_RPM (paired with shorter hatchback gearing)
                6500.0,     // REDLINE_RPM
                6700.0,     // REV_LIMITER_RPM
                6           // SHIFT_DURATION_TICKS
        );
    }

    public static VehicleSpec fromId(String id) {
        if (HATCHBACK_MANUAL.id().equals(id)) {
            return HATCHBACK_MANUAL;
        }
        return HATCHBACK_AUTOMATIC;
    }

    public boolean isAutomatic() {
        return transmissionType == TransmissionType.AUTOMATIC;
    }

    public boolean isManual() {
        return transmissionType == TransmissionType.MANUAL;
    }

    public int gearCount() {
        return gearRatios.length;
    }

    /** Forward gears are numbered 1..gearCount(), matching CarEntity's display. */
    public double gearRatio(int gear) {
        if (gear < 1 || gear > gearRatios.length) {
            throw new IllegalArgumentException(
                    "Gear must be between 1 and " + gearRatios.length + ", got " + gear
            );
        }
        return gearRatios[gear - 1];
    }

    /**
     * Exact torque curve currently used by CarEntity.
     */
    public double engineTorqueNm(double rpm) {
        if (rpm < 1200.0) {
            return lerp(112.0, 154.0, (rpm - idleRpm) / 400.0);
        }
        if (rpm < 2500.0) {
            return lerp(154.0, 203.0, (rpm - 1200.0) / 1300.0);
        }
        if (rpm < 4500.0) {
            return lerp(203.0, 210.0, (rpm - 2500.0) / 2000.0);
        }
        if (rpm < redlineRpm) {
            return lerp(210.0, 147.0, (rpm - 4500.0) / 2000.0);
        }
        return 126.0;
    }

    /** Same coupled-RPM calculation currently used by CarEntity. */
    public double calculateCoupledRpm(double speedBlocksPerTick, double ratio) {
        double metresPerSecond = Math.abs(speedBlocksPerTick) * 20.0;
        double wheelRpm = metresPerSecond / (2.0 * Math.PI * wheelRadiusMetres) * 60.0;
        return Math.max(idleRpm, wheelRpm * ratio * finalDriveRatio);
    }

    /**
     * Same drivetrain acceleration formula currently used by CarEntity.
     * The result is in blocks/tick of velocity added per game tick.
     */
    public double drivetrainAcceleration(
            double speedBlocksPerTick,
            double grip,
            int forwardGear,
            boolean reverse
    ) {
        double ratio = reverse ? reverseRatio : gearRatio(forwardGear);
        double coupledRpm = calculateCoupledRpm(speedBlocksPerTick, ratio);
        if (coupledRpm >= revLimiterRpm) {
            return 0.0;
        }

        double torqueNm = engineTorqueNm(coupledRpm);
        /*
         * Fade engine torque between redline and the limiter instead of turning
         * power fully on/off at one exact speed. This prevents a manual car held
         * in one gear from visibly surging back and forth at the limiter.
         */
        if (coupledRpm > redlineRpm) {
            double limiterRange = Math.max(1.0, revLimiterRpm - redlineRpm);
            double torqueScale = (revLimiterRpm - coupledRpm) / limiterRange;
            torqueNm *= Math.max(0.0, Math.min(1.0, torqueScale));
        }
        double wheelTorqueNm = torqueNm * ratio * finalDriveRatio * drivetrainEfficiency;
        double wheelForceNewtons = wheelTorqueNm / wheelRadiusMetres;
        double accelerationMetresPerSecondSquared = wheelForceNewtons / vehicleMassKg;
        return accelerationMetresPerSecondSquared / 400.0 * grip;
    }

    private static double lerp(double start, double end, double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        return start + (end - start) * clamped;
    }
}
