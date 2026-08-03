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
	private static final TrackedData<Integer> TIRE_TYPE = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final String TIRE_NBT_KEY = "IronMileTireType";

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

	private static final double WHEEL_FORWARD_OFFSET = 0.95;
	private static final double WHEEL_SIDE_OFFSET = 0.65;

	private boolean pressingLeft;
	private boolean pressingRight;
	private boolean pressingForward;
	private boolean pressingBack;
	private double currentGrip = 1.0;
	private String currentSurfaceName = "Road";
	private String currentRoadConditionName = "Dry";

	public CarEntity(EntityType<? extends BoatEntity> entityType, World world) {
		super(entityType, world);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TIRE_TYPE, TireType.ALL_SEASON.ordinal());
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(TIRE_NBT_KEY, this.getTireType().ordinal());
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains(TIRE_NBT_KEY)) {
			this.setTireType(TireType.fromOrdinal(nbt.getInt(TIRE_NBT_KEY)));
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
		double verticalSpeed = this.isOnGround() ? Math.max(velocity.y, 0.0) : velocity.y - GRAVITY;

		this.setVelocity(horizontalX, verticalSpeed, horizontalZ);
		this.move(MovementType.SELF, this.getVelocity());

		if (this.horizontalCollision) {
			this.setVelocity(this.getVelocity().multiply(0.35, 1.0, 0.35));
		}
	}

	private double applyThrottleAndBrakes(double speed, double grip) {
		if (this.pressingForward && !this.pressingBack) {
			if (speed < -0.03) {
				speed = Math.min(0.0, speed + BRAKE_FORCE * grip);
			} else {
				speed += ACCELERATION * grip;
			}
		} else if (this.pressingBack && !this.pressingForward) {
			if (speed > 0.03) {
				speed = Math.max(0.0, speed - BRAKE_FORCE * grip);
			} else {
				speed -= REVERSE_ACCELERATION * grip;
			}
		} else {
			speed *= ROLLING_RESISTANCE;
		}

		return MathHelper.clamp(speed, -MAX_REVERSE_SPEED, MAX_FORWARD_SPEED);
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
