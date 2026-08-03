package com.cjeme26.ironmile.entity;

import com.cjeme26.ironmile.item.TireItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * The second Iron Mile vehicle prototype: simple road-focused movement.
 *
 * <p>We still inherit BoatEntity temporarily for its proven passenger and
 * keyboard-input plumbing, but the controlling player's movement is calculated
 * here. These constants are deliberately easy to tune after driving tests.</p>
 */
public class CarEntity extends BoatEntity {
	private static final TrackedData<Integer> TIRE_TYPE = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Boolean> HEADLIGHTS_ON = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final String TIRE_NBT_KEY = "IronMileTireType";
	private static final String HEADLIGHTS_NBT_KEY = "IronMileHeadlightsOn";

	// Horizontal speeds are measured in blocks per game tick.
	public static final double MAX_FORWARD_SPEED = 1.95;
	public static final double MAX_REVERSE_SPEED = 0.35;
	public static final double BRAKE_FORCE = 0.021;
	public static final double ROLLING_RESISTANCE = 0.992;
	public static final double LATERAL_VELOCITY_RETAINED = 0.42;
	public static final float MAX_STEERING_PER_TICK = 2.6F;
	private static final double[] GEAR_RATIOS = {0.0, 3.80, 2.40, 1.65, 1.25, 0.95, 0.72};
	private static final double REVERSE_RATIO = 3.20;
	private static final double FINAL_DRIVE_RATIO = 4.00;
	private static final double WHEEL_RADIUS_METRES = 0.34;
	private static final double VEHICLE_MASS_KG = 1500.0;
	private static final double DRIVETRAIN_EFFICIENCY = 0.86;
	private static final double AERODYNAMIC_DRAG = 0.00030;
	private static final double ENGINE_BRAKE_FORCE = 0.00055;
	private static final double LOW_SPEED_COAST_THRESHOLD = 0.35;
	private static final double LOW_SPEED_COAST_BRAKE = 0.0024;
	private static final double REVERSE_COAST_BRAKE_MULTIPLIER = 1.5;
	private static final double AUTOMATIC_STOP_SPEED = 0.025;
	private static final double IDLE_RPM = 800.0;
	private static final double DOWNSHIFT_RPM = 1600.0;
	private static final double KICKDOWN_RPM = 2300.0;
	private static final double UPSHIFT_RPM = 5800.0;
	private static final double REDLINE_RPM = 6500.0;
	private static final double REV_LIMITER_RPM = 6700.0;
	private static final int SHIFT_DURATION_TICKS = 6;
	private static final double GRAVITY = 0.04;
	private static final double GROUNDING_FORCE = 0.08;
	private static final float STEP_HEIGHT = 0.6F;
	private static final double MAX_COLLISION_STEP_DISTANCE = 0.35;
	private static final int MAX_COLLISION_STEPS_PER_TICK = 8;
	private static final double STOP_EPSILON = 0.002;

	private static final double WHEEL_FORWARD_OFFSET = 0.95;
	private static final double WHEEL_SIDE_OFFSET = 0.65;

	private boolean pressingLeft;
	private boolean pressingRight;
	private boolean pressingForward;
	private boolean pressingBack;
	private double currentGrip = 1.0;
	private String currentSurfaceName = "Road";
	private String currentRoadConditionName = "Dry";
	private int currentGear = 1;
	private int shiftTicksRemaining;
	private double engineRpm = IDLE_RPM;
	private double lastForwardSpeed;
	private boolean reverseEngaged;

	public CarEntity(EntityType<? extends BoatEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Lets the wheels roll onto slabs and similarly low road edges. */
	@Override
	public float getStepHeight() {
		return STEP_HEIGHT;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TIRE_TYPE, TireType.ALL_SEASON.ordinal());
		builder.add(HEADLIGHTS_ON, false);
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(TIRE_NBT_KEY, this.getTireType().ordinal());
		nbt.putBoolean(HEADLIGHTS_NBT_KEY, this.areHeadlightsOn());
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains(TIRE_NBT_KEY)) {
			this.setTireType(TireType.fromOrdinal(nbt.getInt(TIRE_NBT_KEY)));
		}
		if (nbt.contains(HEADLIGHTS_NBT_KEY)) {
			this.setHeadlightsOn(nbt.getBoolean(HEADLIGHTS_NBT_KEY));
		}
	}

	@Override
	public ActionResult interact(PlayerEntity player, Hand hand) {
		ItemStack heldStack = player.getStackInHand(hand);
		if (heldStack.getItem() instanceof TireItem tireItem) {
			if (!this.getWorld().isClient) {
				this.setTireType(tireItem.getTireType());
				player.sendMessage(Text.literal(tireItem.getTireType().getDisplayName() + " installed"), true);
			}
			return ActionResult.success(this.getWorld().isClient);
		}
		return super.interact(player, hand);
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

		if (this.isOnGround()) {
			this.sampleWheelGrip(forward, right);
		}

		if (this.hasControllingPassenger() && this.isOnGround()) {
			forwardSpeed = this.applyThrottleAndBrakes(forwardSpeed, this.currentGrip);
			this.applySteering(forwardSpeed, this.currentGrip);

			// Recalculate axes after steering so acceleration follows the new heading.
			yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
			right = new Vec3d(forward.z, 0.0, -forward.x);

			// Low-grip surfaces retain more sideways velocity and therefore slide.
			sidewaysSpeed *= this.getLateralVelocityRetained();
		} else if (this.isOnGround()) {
			forwardSpeed *= ROLLING_RESISTANCE;
			sidewaysSpeed *= this.getLateralVelocityRetained();
		}

		if (Math.abs(forwardSpeed) < STOP_EPSILON && !this.pressingForward && !this.pressingBack) {
			forwardSpeed = 0.0;
		}
		if (Math.abs(sidewaysSpeed) < STOP_EPSILON) {
			sidewaysSpeed = 0.0;
		}

		double horizontalX = forward.x * forwardSpeed + right.x * sidewaysSpeed;
		double horizontalZ = forward.z * forwardSpeed + right.z * sidewaysSpeed;
		/*
		 * A small downward force keeps collision contact active every tick. With
		 * exactly zero vertical movement, Minecraft can alternate between grounded
		 * and airborne states, causing the drivetrain to apply power only every
		 * other tick.
		 */
		double verticalSpeed = this.isOnGround() ? -GROUNDING_FORCE : velocity.y - GRAVITY;

		this.setVelocity(horizontalX, verticalSpeed, horizontalZ);
		if (this.moveWithCollisionSubsteps(this.getVelocity())) {
			this.setVelocity(this.getVelocity().multiply(0.35, 1.0, 0.35));
		}
	}

	/**
	 * Resolves fast movement in short sections so low obstacles are not treated
	 * as a single wall when the car travels more than a block during one tick.
	 */
	private boolean moveWithCollisionSubsteps(Vec3d movement) {
		double horizontalDistance = movement.horizontalLength();
		int steps = MathHelper.clamp(
				(int) Math.ceil(horizontalDistance / MAX_COLLISION_STEP_DISTANCE),
				1,
				MAX_COLLISION_STEPS_PER_TICK
		);
		Vec3d movementStep = movement.multiply(1.0 / steps);
		for (int step = 0; step < steps; step++) {
			this.settleOntoNearbyRoad();
			boolean raisedForRoadEdge = this.prepareForLowRoadEdge(movementStep);
			Vec3d resolvedStep = raisedForRoadEdge
					? new Vec3d(movementStep.x, 0.0, movementStep.z)
					: movementStep;
			this.move(MovementType.SELF, resolvedStep);
			if (this.horizontalCollision) {
				return true;
			}
		}

		return false;
	}

	private boolean prepareForLowRoadEdge(Vec3d movementStep) {
		double horizontalDistance = movementStep.horizontalLength();
		if (horizontalDistance < 0.001 || this.getVelocity().y > 0.05) {
			return false;
		}

		double directionX = movementStep.x / horizontalDistance;
		double directionZ = movementStep.z / horizontalDistance;
		double sideX = directionZ;
		double sideZ = -directionX;
		double leadingDistance = 0.90 + horizontalDistance;
		double bodyBottom = this.getBoundingBox().minY;

		double leftSupport = this.getRoadSupportHeight(
				this.getX() + directionX * leadingDistance - sideX * WHEEL_SIDE_OFFSET,
				this.getZ() + directionZ * leadingDistance - sideZ * WHEEL_SIDE_OFFSET,
				bodyBottom,
				STEP_HEIGHT
		);
		double rightSupport = this.getRoadSupportHeight(
				this.getX() + directionX * leadingDistance + sideX * WHEEL_SIDE_OFFSET,
				this.getZ() + directionZ * leadingDistance + sideZ * WHEEL_SIDE_OFFSET,
				bodyBottom,
				STEP_HEIGHT
		);
		double centreSupport = this.getRoadSupportHeight(
				this.getX() + directionX * leadingDistance,
				this.getZ() + directionZ * leadingDistance,
				bodyBottom,
				STEP_HEIGHT
		);
		double rise = Math.max(centreSupport, Math.max(leftSupport, rightSupport)) - bodyBottom;
		if (rise <= 0.05 || rise > STEP_HEIGHT + 0.01) {
			return false;
		}

		double beforeLiftY = this.getY();
		this.move(MovementType.SELF, new Vec3d(0.0, rise, 0.0));
		double completedLift = this.getY() - beforeLiftY;
		if (completedLift < rise - 0.01) {
			this.move(MovementType.SELF, new Vec3d(0.0, -completedLift, 0.0));
			return false;
		}
		return true;
	}

	private void settleOntoNearbyRoad() {
		if (this.getVelocity().y > 0.0) {
			return;
		}

		double bodyBottom = this.getBoundingBox().minY;
		double inset = 0.72;
		double support = this.getRoadSupportHeight(this.getX(), this.getZ(), bodyBottom, 0.01);
		support = Math.max(support, this.getRoadSupportHeight(this.getX() - inset, this.getZ() - inset, bodyBottom, 0.01));
		support = Math.max(support, this.getRoadSupportHeight(this.getX() - inset, this.getZ() + inset, bodyBottom, 0.01));
		support = Math.max(support, this.getRoadSupportHeight(this.getX() + inset, this.getZ() - inset, bodyBottom, 0.01));
		support = Math.max(support, this.getRoadSupportHeight(this.getX() + inset, this.getZ() + inset, bodyBottom, 0.01));

		double drop = bodyBottom - support;
		if (drop > 0.05 && drop <= STEP_HEIGHT + 0.01) {
			this.move(MovementType.SELF, new Vec3d(0.0, -drop, 0.0));
		}
	}

	private double getRoadSupportHeight(double x, double z, double bodyBottom, double maximumRise) {
		int highestY = MathHelper.floor(bodyBottom + maximumRise);
		int lowestY = MathHelper.floor(bodyBottom - 1.0);

		for (int y = highestY; y >= lowestY; y--) {
			BlockPos pos = BlockPos.ofFloored(x, y, z);
			BlockState state = this.getWorld().getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(this.getWorld(), pos);
			if (shape.isEmpty()) {
				continue;
			}

			double top = y + shape.getMax(Direction.Axis.Y);
			if (top <= bodyBottom + maximumRise + 0.01) {
				return top;
			}
		}

		return bodyBottom - 1.0;
	}

	private double applyThrottleAndBrakes(double speed, double grip) {
		this.updateAutomaticTransmission(speed);
		boolean shifting = this.shiftTicksRemaining > 0;

		if (this.pressingForward && !this.pressingBack) {
			if (speed < -0.03) {
				speed = Math.min(0.0, speed + this.getBrakeForce(speed, grip));
				this.reverseEngaged = speed < 0.0;
			} else if (!shifting) {
				this.reverseEngaged = false;
				speed += this.getDrivetrainAcceleration(speed, grip, false);
			}
		} else if (this.pressingBack && !this.pressingForward) {
			if (speed > 0.03) {
				speed = Math.max(0.0, speed - this.getBrakeForce(speed, grip));
				this.reverseEngaged = false;
			} else if (!shifting) {
				this.reverseEngaged = true;
				speed -= this.getDrivetrainAcceleration(speed, grip, true);
			}
		} else {
			speed *= ROLLING_RESISTANCE;
			double gearRatio = this.reverseEngaged ? REVERSE_RATIO : GEAR_RATIOS[this.currentGear];
			double engineBrake = ENGINE_BRAKE_FORCE * (gearRatio / GEAR_RATIOS[1]);
			speed = this.moveTowardZero(speed, engineBrake);

			/*
			 * Low gears resist coasting more strongly than high gears. Fade this
			 * effect out by 25 km/h so road-speed coasting remains natural, and
			 * make reverse settle promptly after the driver releases S.
			 */
			double lowSpeedFactor = 1.0 - Math.min(Math.abs(speed) / LOW_SPEED_COAST_THRESHOLD, 1.0);
			double lowSpeedCoastBrake = LOW_SPEED_COAST_BRAKE * lowSpeedFactor;
			if (this.reverseEngaged) {
				lowSpeedCoastBrake *= REVERSE_COAST_BRAKE_MULTIPLIER;
			}
			speed = this.moveTowardZero(speed, lowSpeedCoastBrake);

			if (Math.abs(speed) < AUTOMATIC_STOP_SPEED) {
				speed = 0.0;
			}
		}

		if (shifting) {
			speed *= 0.998;
		}

		// Quadratic drag makes high-speed acceleration taper naturally.
		double drag = AERODYNAMIC_DRAG * speed * speed;
		speed = this.moveTowardZero(speed, drag);
		speed = MathHelper.clamp(speed, -MAX_REVERSE_SPEED, MAX_FORWARD_SPEED);
		this.lastForwardSpeed = speed;
		this.updateEngineRpm(speed, shifting);
		return speed;
	}

	private void updateAutomaticTransmission(double speed) {
		if (this.shiftTicksRemaining > 0) {
			this.shiftTicksRemaining--;
			return;
		}

		if (speed < -0.03 || this.reverseEngaged) {
			this.currentGear = 1;
			return;
		}

		double coupledRpm = this.calculateCoupledRpm(speed, GEAR_RATIOS[this.currentGear]);
		if (this.pressingForward) {
			if (coupledRpm >= UPSHIFT_RPM && this.currentGear < 6) {
				this.beginShift(this.currentGear + 1);
				return;
			}

			if (coupledRpm < KICKDOWN_RPM && this.currentGear > 1) {
				double lowerGearRpm = this.calculateCoupledRpm(speed, GEAR_RATIOS[this.currentGear - 1]);
				if (lowerGearRpm < REDLINE_RPM) {
					this.beginShift(this.currentGear - 1);
					return;
				}
			}
		} else if (coupledRpm > 2600.0 && this.currentGear < 6) {
			// With binary W input, throttle release represents an economy upshift.
			this.beginShift(this.currentGear + 1);
			return;
		}

		if (coupledRpm < DOWNSHIFT_RPM && this.currentGear > 1) {
			this.beginShift(this.currentGear - 1);
		}
	}

	private void beginShift(int newGear) {
		this.currentGear = MathHelper.clamp(newGear, 1, 6);
		this.shiftTicksRemaining = SHIFT_DURATION_TICKS;
	}

	private double getDrivetrainAcceleration(double speed, double grip, boolean reverse) {
		double ratio = reverse ? REVERSE_RATIO : GEAR_RATIOS[this.currentGear];
		double coupledRpm = this.calculateCoupledRpm(speed, ratio);
		if (coupledRpm >= REV_LIMITER_RPM) {
			return 0.0;
		}

		double torqueNm = this.getEngineTorqueNm(coupledRpm);
		double wheelTorqueNm = torqueNm * ratio * FINAL_DRIVE_RATIO * DRIVETRAIN_EFFICIENCY;
		double wheelForceNewtons = wheelTorqueNm / WHEEL_RADIUS_METRES;
		double accelerationMetresPerSecondSquared = wheelForceNewtons / VEHICLE_MASS_KG;
		return accelerationMetresPerSecondSquared / 400.0 * grip;
	}

	private double calculateCoupledRpm(double speed, double ratio) {
		double metresPerSecond = Math.abs(speed) * 20.0;
		double wheelRpm = metresPerSecond / (2.0 * Math.PI * WHEEL_RADIUS_METRES) * 60.0;
		return Math.max(IDLE_RPM, wheelRpm * ratio * FINAL_DRIVE_RATIO);
	}

	private double getEngineTorqueNm(double rpm) {
		if (rpm < 1200.0) {
			return this.lerp(112.0, 154.0, (rpm - IDLE_RPM) / 400.0);
		}
		if (rpm < 2500.0) {
			return this.lerp(154.0, 203.0, (rpm - 1200.0) / 1300.0);
		}
		if (rpm < 4500.0) {
			return this.lerp(203.0, 210.0, (rpm - 2500.0) / 2000.0);
		}
		if (rpm < REDLINE_RPM) {
			return this.lerp(210.0, 147.0, (rpm - 4500.0) / 2000.0);
		}
		return 126.0;
	}

	private double getBrakeForce(double speed, double grip) {
		double speedKmh = Math.abs(speed) * 72.0;
		double lowSpeedAmount = 1.0 - MathHelper.clamp(speedKmh / 35.0, 0.0, 1.0);
		double lowSpeedBoost = 1.0 + 0.90 * lowSpeedAmount;
		return BRAKE_FORCE * grip * lowSpeedBoost;
	}

	private double lerp(double start, double end, double amount) {
		return start + (end - start) * MathHelper.clamp(amount, 0.0, 1.0);
	}

	private double moveTowardZero(double value, double amount) {
		if (value > 0.0) {
			return Math.max(0.0, value - amount);
		}
		if (value < 0.0) {
			return Math.min(0.0, value + amount);
		}
		return 0.0;
	}

	private void updateEngineRpm(double speed, boolean shifting) {
		double ratio = this.reverseEngaged ? REVERSE_RATIO : GEAR_RATIOS[this.currentGear];
		double targetRpm = shifting
				? Math.max(IDLE_RPM, this.engineRpm * 0.86)
				: Math.min(REV_LIMITER_RPM, this.calculateCoupledRpm(speed, ratio));
		this.engineRpm += (targetRpm - this.engineRpm) * 0.35;
	}

	private void applySteering(double forwardSpeed, double grip) {
		int steeringInput = (this.pressingRight ? 1 : 0) - (this.pressingLeft ? 1 : 0);
		if (steeringInput == 0 || Math.abs(forwardSpeed) < 0.01) {
			return;
		}

		double speedRatio = Math.min(Math.abs(forwardSpeed) / 0.25, 1.0);
		double highSpeedReduction = 1.0 - 0.55 * Math.min(Math.abs(forwardSpeed) / MAX_FORWARD_SPEED, 1.0);
		double direction = Math.signum(forwardSpeed);
		float yawChange = (float) (steeringInput * direction * MAX_STEERING_PER_TICK * speedRatio * highSpeedReduction * grip);
		this.setYaw(this.getYaw() + yawChange);
	}

	private void sampleWheelGrip(Vec3d forward, Vec3d right) {
		WheelSample frontLeft = this.getSurfaceAtWheel(forward, right, WHEEL_FORWARD_OFFSET, -WHEEL_SIDE_OFFSET);
		WheelSample frontRight = this.getSurfaceAtWheel(forward, right, WHEEL_FORWARD_OFFSET, WHEEL_SIDE_OFFSET);
		WheelSample rearLeft = this.getSurfaceAtWheel(forward, right, -WHEEL_FORWARD_OFFSET, -WHEEL_SIDE_OFFSET);
		WheelSample rearRight = this.getSurfaceAtWheel(forward, right, -WHEEL_FORWARD_OFFSET, WHEEL_SIDE_OFFSET);

		TireType tires = this.getTireType();
		this.currentGrip = (
				frontLeft.surface.getGrip(tires, frontLeft.wet)
						+ frontRight.surface.getGrip(tires, frontRight.wet)
						+ rearLeft.surface.getGrip(tires, rearLeft.wet)
						+ rearRight.surface.getGrip(tires, rearRight.wet)
		) / 4.0;
		boolean mixedSurface = frontLeft.surface != frontRight.surface
				|| frontLeft.surface != rearLeft.surface
				|| frontLeft.surface != rearRight.surface;
		this.currentSurfaceName = mixedSurface ? "Mixed" : frontLeft.surface.displayName;

		int wetWheels = (frontLeft.wet ? 1 : 0)
				+ (frontRight.wet ? 1 : 0)
				+ (rearLeft.wet ? 1 : 0)
				+ (rearRight.wet ? 1 : 0);
		this.currentRoadConditionName = wetWheels == 0 ? "Dry" : wetWheels == 4 ? "Wet" : "Mixed";
	}

	private WheelSample getSurfaceAtWheel(Vec3d forward, Vec3d right, double forwardOffset, double sideOffset) {
		double x = this.getX() + forward.x * forwardOffset + right.x * sideOffset;
		double z = this.getZ() + forward.z * forwardOffset + right.z * sideOffset;
		double wheelY = this.getBoundingBox().minY - 0.05;
		BlockPos pos = BlockPos.ofFloored(x, wheelY, z);
		BlockState state = this.getWorld().getBlockState(pos);

		if (state.isAir()) {
			pos = pos.down();
			state = this.getWorld().getBlockState(pos);
		}

		boolean wet = this.getWorld().hasRain(pos.up());
		return new WheelSample(RoadSurface.from(state), wet);
	}

	private double getLateralVelocityRetained() {
		return 1.0 - (1.0 - LATERAL_VELOCITY_RETAINED) * this.currentGrip;
	}

	public double getHorizontalSpeedKmh() {
		// One block is treated as one metre; Minecraft runs at 20 ticks per second.
		return this.getVelocity().horizontalLength() * 20.0 * 3.6;
	}

	public int getEngineRpm() {
		return (int) Math.round(this.engineRpm / 50.0) * 50;
	}

	public boolean hasThrottleInput() {
		return this.pressingForward || this.pressingBack;
	}

	public boolean isChangingGear() {
		return this.shiftTicksRemaining > 0;
	}

	public boolean isReverseEngaged() {
		return this.reverseEngaged;
	}

	public boolean isBrakeInputActive() {
		return (this.lastForwardSpeed > 0.03 && this.pressingBack)
				|| (this.lastForwardSpeed < -0.03 && this.pressingForward);
	}

	public boolean areHeadlightsOn() {
		return this.dataTracker.get(HEADLIGHTS_ON);
	}

	public void setHeadlightsOn(boolean headlightsOn) {
		this.dataTracker.set(HEADLIGHTS_ON, headlightsOn);
	}

	public String getGearDisplay() {
		if (this.shiftTicksRemaining > 0) {
			return "N";
		}
		if (this.reverseEngaged || this.lastForwardSpeed < -0.01) {
			return "R";
		}
		return "D" + this.currentGear;
	}

	public double getCurrentGrip() {
		return this.currentGrip;
	}

	public String getCurrentSurfaceName() {
		return this.currentSurfaceName;
	}

	public String getCurrentRoadConditionName() {
		return this.currentRoadConditionName;
	}

	public TireType getTireType() {
		return TireType.fromOrdinal(this.dataTracker.get(TIRE_TYPE));
	}

	public void setTireType(TireType tireType) {
		this.dataTracker.set(TIRE_TYPE, tireType.ordinal());
	}

	public enum TireType {
		SUMMER("Summer Tires"),
		ALL_SEASON("All-Season Tires"),
		WINTER("Winter Tires");

		private final String displayName;

		TireType(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		private static TireType fromOrdinal(int ordinal) {
			TireType[] values = values();
			return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ALL_SEASON;
		}
	}

	private record WheelSample(RoadSurface surface, boolean wet) {
	}

	private enum RoadSurface {
		//                          Summer  All-season  Winter
		ROAD("Road",                 1.00,       0.90,   0.82),
		GRAVEL("Gravel",             0.68,       0.70,   0.68),
		DIRT("Dirt / Grass",         0.58,       0.62,   0.60),
		MUD("Mud",                   0.42,       0.50,   0.48),
		SAND("Sand",                 0.40,       0.45,   0.43),
		SNOW("Snow",                 0.15,       0.38,   0.75),
		ICE("Ice",                   0.06,       0.13,   0.28),
		BLUE_ICE("Blue Ice",         0.04,       0.09,   0.20);

		private final String displayName;
		private final double summerGrip;
		private final double allSeasonGrip;
		private final double winterGrip;

		RoadSurface(String displayName, double summerGrip, double allSeasonGrip, double winterGrip) {
			this.displayName = displayName;
			this.summerGrip = summerGrip;
			this.allSeasonGrip = allSeasonGrip;
			this.winterGrip = winterGrip;
		}

		private double getGrip(TireType tireType, boolean wet) {
			double dryGrip = switch (tireType) {
				case SUMMER -> this.summerGrip;
				case ALL_SEASON -> this.allSeasonGrip;
				case WINTER -> this.winterGrip;
			};
			if (!wet) {
				return dryGrip;
			}

			// Wet-road values are absolute so each tire has a distinct compromise.
			if (this == ROAD) {
				return switch (tireType) {
					case SUMMER -> 0.76;
					case ALL_SEASON -> 0.78;
					case WINTER -> 0.68;
				};
			}

			double wetMultiplier = switch (tireType) {
				case SUMMER -> 0.84;
				case ALL_SEASON -> 0.88;
				case WINTER -> 0.83;
			};
			return dryGrip * wetMultiplier;
		}

		private static RoadSurface from(BlockState state) {
			if (state.isOf(Blocks.BLUE_ICE)) {
				return BLUE_ICE;
			}
			if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.FROSTED_ICE)) {
				return ICE;
			}
			if (state.isOf(Blocks.SNOW) || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.POWDER_SNOW)) {
				return SNOW;
			}
			if (state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND) || state.isOf(Blocks.SOUL_SAND)) {
				return SAND;
			}
			if (state.isOf(Blocks.MUD) || state.isOf(Blocks.MUDDY_MANGROVE_ROOTS)) {
				return MUD;
			}
			if (state.isOf(Blocks.GRAVEL)) {
				return GRAVEL;
			}
			if (state.isOf(Blocks.DIRT)
					|| state.isOf(Blocks.GRASS_BLOCK)
					|| state.isOf(Blocks.COARSE_DIRT)
					|| state.isOf(Blocks.ROOTED_DIRT)
					|| state.isOf(Blocks.PODZOL)
					|| state.isOf(Blocks.MYCELIUM)) {
				return DIRT;
			}
			return ROAD;
		}
	}

}
