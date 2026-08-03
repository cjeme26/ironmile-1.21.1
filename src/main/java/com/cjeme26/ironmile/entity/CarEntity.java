package com.cjeme26.ironmile.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The second Iron Mile vehicle prototype: simple road-focused movement.
 *
 * <p>We still inherit BoatEntity temporarily for its proven passenger and
 * keyboard-input plumbing, but the controlling player's movement is calculated
 * here. These constants are deliberately easy to tune after driving tests.</p>
 */
public class CarEntity extends BoatEntity {
	// Horizontal speeds are measured in blocks per game tick.
	public static final double MAX_FORWARD_SPEED = 0.75;
	public static final double MAX_REVERSE_SPEED = 0.25;
	public static final double ACCELERATION = 0.012;
	public static final double REVERSE_ACCELERATION = 0.007;
	public static final double BRAKE_FORCE = 0.035;
	public static final double ROLLING_RESISTANCE = 0.985;
	public static final double LATERAL_VELOCITY_RETAINED = 0.42;
	public static final float MAX_STEERING_PER_TICK = 2.6F;
	private static final double GRAVITY = 0.04;
	private static final double STOP_EPSILON = 0.002;

	private boolean pressingLeft;
	private boolean pressingRight;
	private boolean pressingForward;
	private boolean pressingBack;

	public CarEntity(EntityType<? extends BoatEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	public void setInputs(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack) {
		this.pressingLeft = pressingLeft;
		this.pressingRight = pressingRight;
		this.pressingForward = pressingForward;
		this.pressingBack = pressingBack;
	}

	/** Prevents the inherited boat networking from enabling paddle animation/sounds. */
	@Override
	public void setPaddleMovings(boolean leftMoving, boolean rightMoving) {
		// Intentionally empty: Iron Mile cars do not have paddles.
	}

	@Override
	public void tick() {
		/*
		 * Minecraft lets the controlling client simulate a ridden boat and sends
		 * the resulting vehicle position to the server. Keeping the inherited tick
		 * on the other logical side preserves vanilla interpolation and tracking.
		 */
		if (!this.isLogicalSideForUpdatingMovement()) {
			super.tick();
			return;
		}

		this.baseTick();
		this.tickRoadMovement();
	}

	private void tickRoadMovement() {
		Vec3d velocity = this.getVelocity();
		float yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;

		// Minecraft yaw 0 faces positive Z.
		Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);

		double forwardSpeed = velocity.x * forward.x + velocity.z * forward.z;
		double sidewaysSpeed = velocity.x * right.x + velocity.z * right.z;

		if (this.hasControllingPassenger() && this.isOnGround()) {
			forwardSpeed = this.applyThrottleAndBrakes(forwardSpeed);
			this.applySteering(forwardSpeed);

			// Recalculate axes after steering so acceleration follows the new heading.
			yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
			right = new Vec3d(forward.z, 0.0, -forward.x);

			// Tire grip removes most sideways motion without snapping it fully to zero.
			sidewaysSpeed *= LATERAL_VELOCITY_RETAINED;
		} else if (this.isOnGround()) {
			forwardSpeed *= ROLLING_RESISTANCE;
			sidewaysSpeed *= LATERAL_VELOCITY_RETAINED;
		}

		if (Math.abs(forwardSpeed) < STOP_EPSILON && !this.pressingForward && !this.pressingBack) {
			forwardSpeed = 0.0;
		}
		if (Math.abs(sidewaysSpeed) < STOP_EPSILON) {
			sidewaysSpeed = 0.0;
		}

		double horizontalX = forward.x * forwardSpeed + right.x * sidewaysSpeed;
		double horizontalZ = forward.z * forwardSpeed + right.z * sidewaysSpeed;
		double verticalSpeed = this.isOnGround() ? Math.max(velocity.y, 0.0) : velocity.y - GRAVITY;

		this.setVelocity(horizontalX, verticalSpeed, horizontalZ);
		this.move(MovementType.SELF, this.getVelocity());

		if (this.horizontalCollision) {
			this.setVelocity(this.getVelocity().multiply(0.35, 1.0, 0.35));
		}
	}

	private double applyThrottleAndBrakes(double speed) {
		if (this.pressingForward && !this.pressingBack) {
			if (speed < -0.03) {
				speed = Math.min(0.0, speed + BRAKE_FORCE);
			} else {
				speed += ACCELERATION;
			}
		} else if (this.pressingBack && !this.pressingForward) {
			if (speed > 0.03) {
				speed = Math.max(0.0, speed - BRAKE_FORCE);
			} else {
				speed -= REVERSE_ACCELERATION;
			}
		} else {
			speed *= ROLLING_RESISTANCE;
		}

		return MathHelper.clamp(speed, -MAX_REVERSE_SPEED, MAX_FORWARD_SPEED);
	}

	private void applySteering(double forwardSpeed) {
		int steeringInput = (this.pressingRight ? 1 : 0) - (this.pressingLeft ? 1 : 0);
		if (steeringInput == 0 || Math.abs(forwardSpeed) < 0.01) {
			return;
		}

		double speedRatio = Math.min(Math.abs(forwardSpeed) / 0.25, 1.0);
		double highSpeedReduction = 1.0 - 0.55 * Math.min(Math.abs(forwardSpeed) / MAX_FORWARD_SPEED, 1.0);
		double direction = Math.signum(forwardSpeed);
		float yawChange = (float) (steeringInput * direction * MAX_STEERING_PER_TICK * speedRatio * highSpeedReduction);
		this.setYaw(this.getYaw() + yawChange);
	}

	public double getHorizontalSpeedKmh() {
		// One block is treated as one metre; Minecraft runs at 20 ticks per second.
		return this.getVelocity().horizontalLength() * 20.0 * 3.6;
	}

}
