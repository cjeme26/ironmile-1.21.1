package com.cjeme26.ironmile.entity;

import com.cjeme26.ironmile.item.TireItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

/**
 * Iron Mile road vehicle with road-focused movement and drivetrain simulation.
 *
 * <p>We still inherit BoatEntity temporarily for its proven passenger and
 * keyboard-input plumbing, but Iron Mile now calculates authoritative movement
 * on the server. These constants are deliberately easy to tune after tests.</p>
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
	private static final TrackedData<String> VEHICLE_SPEC_ID = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.STRING
	);
	private static final TrackedData<Integer> SYNCED_GEAR = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Integer> SYNCED_ENGINE_RPM = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Boolean> SYNCED_SHIFTING = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Boolean> SYNCED_REVERSE = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Float> SYNCED_FORWARD_SPEED = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.FLOAT
	);
	private static final TrackedData<Float> SYNCED_GRIP = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.FLOAT
	);
	private static final TrackedData<String> SYNCED_SURFACE = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.STRING
	);
	private static final TrackedData<String> SYNCED_ROAD_CONDITION = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.STRING
	);
	private static final TrackedData<Float> SYNCED_STEERING = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.FLOAT
	);
	private static final TrackedData<Boolean> SYNCED_BRAKING = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final TrackedData<Boolean> SYNCED_THROTTLE = DataTracker.registerData(
			CarEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN
	);
	private static final String TIRE_NBT_KEY = "IronMileTireType";
	private static final String HEADLIGHTS_NBT_KEY = "IronMileHeadlightsOn";
	private static final String VEHICLE_SPEC_NBT_KEY = "IronMileVehicleSpec";
	private static final String MANUAL_GEAR_NBT_KEY = "IronMileManualGear";

	// Vehicle-specific driving values live in VehicleSpec so additional cars can
	// share this entity logic without duplicating the drivetrain simulation.
	private VehicleSpec vehicleSpec = VehicleSpec.HATCHBACK_AUTOMATIC;

	private static final double GRAVITY = 0.04;
	private static final double GROUNDING_FORCE = 0.08;
	private static final float STEP_HEIGHT = 0.6F;
	private static final double MAX_COLLISION_STEP_DISTANCE = 0.35;
	private static final int MAX_COLLISION_STEPS_PER_TICK = 8;
	private static final double STOP_EPSILON = 0.002;
	private static final double EXIT_INSTANT_STOP_SPEED = 10.0 / 72.0;
	private static final double EXIT_BRAKE_MIN_FORCE = 0.035;
	private static final double EXIT_BRAKE_SPEED_FRACTION = 0.22;
	private static final int REMOTE_POSITION_INTERPOLATION_STEPS = 2;
	private static final double CLIENT_PREDICTION_HARD_CORRECTION_DISTANCE_SQUARED = 16.0;
	private static final float CLIENT_PREDICTION_HARD_CORRECTION_YAW_DEGREES = 75.0F;
	/*
	 * Keep simulating locally for a few ticks after the driver leaves. This lets the
	 * authoritative server transition into the same no-input/exit-braking state
	 * before normal remote-entity interpolation takes over.
	 */
	private static final int CLIENT_DISMOUNT_HANDOFF_TICKS = 3;
	/* Ignore only tiny residual differences at the end of the handoff. */
	private static final double CLIENT_DISMOUNT_DEAD_ZONE_DISTANCE = 0.18;
	private static final double CLIENT_DISMOUNT_DEAD_ZONE_DISTANCE_SQUARED =
			CLIENT_DISMOUNT_DEAD_ZONE_DISTANCE * CLIENT_DISMOUNT_DEAD_ZONE_DISTANCE;
	private static final float CLIENT_DISMOUNT_DEAD_ZONE_YAW_DEGREES = 4.0F;
	private static final int BUMPER_CONTACT_SEARCH_STEPS = 7;
	private static final double MIN_BUMPER_CONTACT_MOVEMENT = 0.001;
	private static final double BUMPER_CONTACT_RECHECK_DISTANCE = 0.035;

	private static final double WHEEL_FORWARD_OFFSET = 1.23;
	private static final double WHEEL_SIDE_OFFSET = 0.65;
	private static final double BUMPER_FORWARD_OFFSET = 1.82;
	private static final double BUMPER_SIDE_OFFSET = 0.72;
	private static final double BUMPER_SENSOR_RADIUS = 0.10;
	private static final double DRIVER_SEAT_SIDE_OFFSET = 0.32;
	private static final double DRIVER_SEAT_FORWARD_OFFSET = 0.18;
	private static final double DRIVER_SEAT_HEIGHT = 0.20;
	private static final double DISMOUNT_SIDE_OFFSET = 1.38;
	private static final double DISMOUNT_FORWARD_OFFSET = -0.12;
	private boolean pressingLeft;
	private boolean pressingRight;
	private boolean pressingForward;
	private boolean pressingBack;
	private double currentGrip = 1.0;
	private String currentSurfaceName = "Road";
	private String currentRoadConditionName = "Dry";
	private int currentGear = 1;
	private int shiftTicksRemaining;
	private double engineRpm = this.vehicleSpec.idleRpm();
	private double lastForwardSpeed;
	private boolean exitBrakingActive;
	private boolean reverseEngaged;
	/** 1 = front bumper held against an obstacle, -1 = rear, 0 = clear. */
	private int bumperContactDirection;
	private HeadlightMarkerEntity headlightMarker;

	/*
	 * The server always remains authoritative, but the local driver's client also
	 * runs the same road simulation for immediate visual response. Routine server
	 * snapshots are recorded while prediction is active instead of pulling the car
	 * backwards every tick. Remote cars continue to use server interpolation.
	 */
	private boolean clientPredictionActive;
	private int clientPredictionHandoffTicks;
	private boolean clientHasAuthoritativeTransform;
	private double clientAuthoritativeX;
	private double clientAuthoritativeY;
	private double clientAuthoritativeZ;
	private float clientAuthoritativeYaw;
	private float clientAuthoritativePitch;
	private boolean clientHasAuthoritativeVelocity;
	private Vec3d clientAuthoritativeVelocity = Vec3d.ZERO;

	public CarEntity(EntityType<? extends BoatEntity> entityType, World world) {
		super(entityType, world);
	}

	/** Lets the wheels roll onto slabs and similarly low road edges. */
	@Override
	public float getStepHeight() {
		return STEP_HEIGHT;
	}

	/** Places the driver inside the compact hatchback cabin rather than on a boat bench. */
	@Override
	protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
		return new Vec3d(DRIVER_SEAT_SIDE_OFFSET, DRIVER_SEAT_HEIGHT, DRIVER_SEAT_FORWARD_OFFSET);
	}

	/**
	 * Prefer the two doors when leaving the car. Looking toward the bonnet or
	 * hatch must not place the player inside the long visual body.
	 */
	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		float yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		Vec3d driverSide = new Vec3d(forward.z, 0.0, -forward.x);
		Vec3d[] sideChoices = {driverSide, driverSide.multiply(-1.0)};

		for (Vec3d side : sideChoices) {
			Vec3d candidate = new Vec3d(
					this.getX() + side.x * DISMOUNT_SIDE_OFFSET + forward.x * DISMOUNT_FORWARD_OFFSET,
					this.getBoundingBox().minY + 0.05,
					this.getZ() + side.z * DISMOUNT_SIDE_OFFSET + forward.z * DISMOUNT_FORWARD_OFFSET
			);
			if (this.getWorld().isSpaceEmpty(
					passenger,
					passenger.getDimensions(EntityPose.STANDING).getBoxAt(candidate)
			)) {
				return candidate;
			}
		}

		return super.updatePassengerForDismount(passenger);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(TIRE_TYPE, TireType.ALL_SEASON.ordinal());
		builder.add(HEADLIGHTS_ON, false);
		builder.add(VEHICLE_SPEC_ID, VehicleSpec.HATCHBACK_AUTOMATIC.id());
		builder.add(SYNCED_GEAR, 1);
		builder.add(SYNCED_ENGINE_RPM, (int) VehicleSpec.HATCHBACK_AUTOMATIC.idleRpm());
		builder.add(SYNCED_SHIFTING, false);
		builder.add(SYNCED_REVERSE, false);
		builder.add(SYNCED_FORWARD_SPEED, 0.0F);
		builder.add(SYNCED_GRIP, 1.0F);
		builder.add(SYNCED_SURFACE, "Road");
		builder.add(SYNCED_ROAD_CONDITION, "Dry");
		builder.add(SYNCED_STEERING, 0.0F);
		builder.add(SYNCED_BRAKING, false);
		builder.add(SYNCED_THROTTLE, false);
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(TIRE_NBT_KEY, this.getTireType().ordinal());
		nbt.putBoolean(HEADLIGHTS_NBT_KEY, this.areHeadlightsOn());
		nbt.putString(VEHICLE_SPEC_NBT_KEY, this.getVehicleSpec().id());
		if (this.isManualTransmission()) nbt.putInt(MANUAL_GEAR_NBT_KEY, this.currentGear);
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
		if (nbt.contains(VEHICLE_SPEC_NBT_KEY)) {
			this.setVehicleSpec(VehicleSpec.fromId(nbt.getString(VEHICLE_SPEC_NBT_KEY)));
		}
		if (this.isManualTransmission() && nbt.contains(MANUAL_GEAR_NBT_KEY)) {
			this.currentGear = MathHelper.clamp(nbt.getInt(MANUAL_GEAR_NBT_KEY), -1, this.vehicleSpec.gearCount());
			this.reverseEngaged = this.currentGear == -1;
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

	private void clearInputs() {
		this.setInputs(false, false, false, false);
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		this.clearInputs();

		/*
		 * Treat leaving the driver's seat like applying a parking brake. At walking
		 * speed the car stops immediately; faster exits preserve a short, speed-scaled
		 * roll instead of coasting for several seconds.
		 */
		double horizontalSpeed = this.getVelocity().horizontalLength();
		if (horizontalSpeed <= EXIT_INSTANT_STOP_SPEED) {
			this.setVelocity(0.0, this.getVelocity().y, 0.0);
			this.lastForwardSpeed = 0.0;
			this.exitBrakingActive = false;
		} else {
			this.exitBrakingActive = true;
		}
	}

	/** Prevents the inherited boat networking from enabling paddle animation/sounds. */
	@Override
	public void setPaddleMovings(boolean leftMoving, boolean rightMoving) {
		// Intentionally empty: Iron Mile cars do not have paddles.
	}

	/**
	 * Iron Mile deliberately makes the server the only logical movement side.
	 * This also stops the vanilla client from sending VehicleMoveC2SPacket data
	 * for the inherited boat, so dismounting no longer changes who owns position.
	 */
	@Override
	public boolean isLogicalSideForUpdatingMovement() {
		return !this.getWorld().isClient;
	}

	/**
	 * Remote cars use a short interpolation step. The locally controlled car does
	 * not consume routine position snapshots while prediction is active, otherwise
	 * every server update would visibly pull it toward an older position.
	 */
	@Override
	public void updateTrackedPositionAndAngles(
			double x,
			double y,
			double z,
			float yaw,
			float pitch,
			int interpolationSteps
	) {
		if (!this.getWorld().isClient) {
			super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
			return;
		}

		this.clientHasAuthoritativeTransform = true;
		this.clientAuthoritativeX = x;
		this.clientAuthoritativeY = y;
		this.clientAuthoritativeZ = z;
		this.clientAuthoritativeYaw = yaw;
		this.clientAuthoritativePitch = pitch;

		if (this.clientPredictionActive) {
			double positionErrorSquared = this.squaredDistanceTo(x, y, z);
			float yawError = Math.abs(MathHelper.wrapDegrees(yaw - this.getYaw()));
			if (positionErrorSquared > CLIENT_PREDICTION_HARD_CORRECTION_DISTANCE_SQUARED
					|| yawError > CLIENT_PREDICTION_HARD_CORRECTION_YAW_DEGREES) {
				this.setPosition(x, y, z);
				this.setRotation(yaw, pitch);
			}
			return;
		}

		this.lerpPosAndRotation(REMOTE_POSITION_INTERPOLATION_STEPS, x, y, z, yaw, pitch);
	}

	/**
	 * Velocity packets are useful for remote cars, audio, and a newly mounted car.
	 * Once local prediction is running, accepting them every tick would overwrite
	 * the velocity that the client just calculated and recreate the hitching.
	 */
	@Override
	public void setVelocityClient(double x, double y, double z) {
		this.clientHasAuthoritativeVelocity = true;
		this.clientAuthoritativeVelocity = new Vec3d(x, y, z);
		if (this.getWorld().isClient && this.clientPredictionActive) {
			return;
		}
		super.setVelocityClient(x, y, z);
	}

	@Override
	public void tick() {
		this.vehicleSpec = VehicleSpec.fromId(this.dataTracker.get(VEHICLE_SPEC_ID));

		if (!this.hasControllingPassenger()) {
			this.clearInputs();
		}

		if (this.getWorld().isClient) {
			if (this.isLocallyControlledClient()) {
				if (!this.clientPredictionActive) {
					this.beginClientPrediction();
				}
				this.clientPredictionHandoffTicks = 0;

				/*
				 * This mirrors the authoritative server path without making the client a
				 * logical vanilla boat owner. Consequently Minecraft does not send its
				 * VehicleMoveC2SPacket, but steering and acceleration are still immediate.
				 */
				this.baseTick();
				this.tickRoadMovement();
				return;
			}

			if (this.clientPredictionActive) {
				/*
				 * A remote driver taking over should immediately use server tracking. For a
				 * normal dismount, however, keep predicting the unoccupied car briefly so
				 * the server can enter the same exit-braking state without a visible rewind.
				 */
				if (this.hasControllingPassenger()) {
					this.endClientPrediction();
				} else {
					if (this.clientPredictionHandoffTicks <= 0) {
						this.clientPredictionHandoffTicks = CLIENT_DISMOUNT_HANDOFF_TICKS;
					}

					this.clearInputs();
					this.baseTick();
					this.tickRoadMovement();
					this.clientPredictionHandoffTicks--;

					if (this.clientPredictionHandoffTicks <= 0) {
						this.endClientPrediction();
					}
					return;
				}
			}

			/*
			 * Keep BoatEntity's passenger handling for unoccupied and remotely driven
			 * cars. BoatEntity clears velocity on a non-logical side, so restore the
			 * latest server velocity for rendering, suspension, audio, and the HUD.
			 */
			Vec3d synchronizedVelocity = this.getVelocity();
			super.tick();
			this.setVelocity(synchronizedVelocity);
			return;
		}

		this.updateHeadlightMarker((ServerWorld) this.getWorld());
		this.baseTick();
		this.tickRoadMovement();
		this.syncAuthoritativeState();
	}

	private boolean isLocallyControlledClient() {
		return this.getWorld().isClient
				&& this.getControllingPassenger() instanceof PlayerEntity player
				&& player.isMainPlayer();
	}

	private void beginClientPrediction() {
		this.clientPredictionActive = true;
		this.clientPredictionHandoffTicks = 0;

		/* Seed private drivetrain fields from the latest authoritative tracker data. */
		this.currentGear = this.dataTracker.get(SYNCED_GEAR);
		this.engineRpm = this.dataTracker.get(SYNCED_ENGINE_RPM);
		this.shiftTicksRemaining = this.dataTracker.get(SYNCED_SHIFTING) ? 1 : 0;
		this.reverseEngaged = this.dataTracker.get(SYNCED_REVERSE);
		this.lastForwardSpeed = this.dataTracker.get(SYNCED_FORWARD_SPEED);
		this.currentGrip = this.dataTracker.get(SYNCED_GRIP);
		this.currentSurfaceName = this.dataTracker.get(SYNCED_SURFACE);
		this.currentRoadConditionName = this.dataTracker.get(SYNCED_ROAD_CONDITION);
		this.bumperContactDirection = 0;
	}

	private void endClientPrediction() {
		this.clientPredictionActive = false;
		this.clientPredictionHandoffTicks = 0;
		this.clearInputs();

		/*
		 * By the end of the short handoff, the latest server snapshot should be close
		 * to the locally simulated position. Tiny remaining differences are ignored so
		 * the handoff cannot create a several-pixel rewind. Large differences still
		 * reconcile, preserving server authority if the simulations genuinely diverge.
		 */
		boolean needsAuthoritativeCorrection = false;
		if (this.clientHasAuthoritativeTransform) {
			double xError = this.clientAuthoritativeX - this.getX();
			double yError = this.clientAuthoritativeY - this.getY();
			double zError = this.clientAuthoritativeZ - this.getZ();
			double positionErrorSquared = xError * xError + yError * yError + zError * zError;
			float yawError = Math.abs(MathHelper.wrapDegrees(this.clientAuthoritativeYaw - this.getYaw()));

			needsAuthoritativeCorrection = positionErrorSquared > CLIENT_DISMOUNT_DEAD_ZONE_DISTANCE_SQUARED
					|| yawError > CLIENT_DISMOUNT_DEAD_ZONE_YAW_DEGREES;
			if (needsAuthoritativeCorrection) {
				this.lerpPosAndRotation(
						REMOTE_POSITION_INTERPOLATION_STEPS,
						this.clientAuthoritativeX,
						this.clientAuthoritativeY,
						this.clientAuthoritativeZ,
						this.clientAuthoritativeYaw,
						this.clientAuthoritativePitch
				);
			}
		}

		/*
		 * Do not replace a good locally predicted exit-braking velocity with the older
		 * velocity snapshot that arrived while driving. Only use server velocity when
		 * we also had to correct a meaningful transform error.
		 */
		if (needsAuthoritativeCorrection && this.clientHasAuthoritativeVelocity) {
			this.setVelocity(this.clientAuthoritativeVelocity);
		}
	}

	private void syncAuthoritativeState() {
		this.dataTracker.set(SYNCED_GEAR, this.currentGear);
		this.dataTracker.set(SYNCED_ENGINE_RPM, (int) Math.round(this.engineRpm));
		this.dataTracker.set(SYNCED_SHIFTING, this.shiftTicksRemaining > 0);
		this.dataTracker.set(SYNCED_REVERSE, this.reverseEngaged);
		this.dataTracker.set(SYNCED_FORWARD_SPEED, (float) this.lastForwardSpeed);
		this.dataTracker.set(SYNCED_GRIP, (float) this.currentGrip);
		this.dataTracker.set(SYNCED_SURFACE, this.currentSurfaceName);
		this.dataTracker.set(SYNCED_ROAD_CONDITION, this.currentRoadConditionName);
		this.dataTracker.set(SYNCED_STEERING, this.calculateVisualSteeringInput());
		this.dataTracker.set(SYNCED_BRAKING, this.calculateBrakeInputActive());
		this.dataTracker.set(SYNCED_THROTTLE, this.pressingForward || this.pressingBack);
	}

	private void updateHeadlightMarker(ServerWorld world) {
		if (!this.areHeadlightsOn() || this.isRemoved()) {
			if (this.headlightMarker != null) {
				this.headlightMarker.discard();
				this.headlightMarker = null;
			}
			return;
		}

		if (this.headlightMarker == null || this.headlightMarker.isRemoved()) {
			this.headlightMarker = new HeadlightMarkerEntity(ModEntities.HEADLIGHT_MARKER, world);
			this.headlightMarker.setCar(this);
			this.headlightMarker.followCar();
			world.spawnEntity(this.headlightMarker);
		}
	}

	private void tickRoadMovement() {
		if (this.hasPassengers()) {
			this.exitBrakingActive = false;
		}
		Vec3d velocity = this.getVelocity();
		float yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;

		// Minecraft yaw 0 faces positive Z.
		Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);

		double forwardSpeed = velocity.x * forward.x + velocity.z * forward.z;
		double sidewaysSpeed = velocity.x * right.x + velocity.z * right.z;

		this.refreshBumperContact(forward);

		if (this.isOnGround()) {
			this.sampleWheelGrip(forward, right);
		}

		if (this.hasControllingPassenger() && this.isOnGround()) {
			if (this.isThrottleHeldAgainstBumper(forwardSpeed)) {
				forwardSpeed = this.holdAtBumperContact();
			} else {
				forwardSpeed = this.applyThrottleAndBrakes(forwardSpeed, this.currentGrip);
			}
			this.applySteering(forwardSpeed, this.currentGrip);

			// Recalculate axes after steering so acceleration follows the new heading.
			yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
			right = new Vec3d(forward.z, 0.0, -forward.x);

			// Low-grip surfaces retain more sideways velocity and therefore slide.
			sidewaysSpeed *= this.getLateralVelocityRetained();
		} else if (this.isOnGround()) {
			/*
			 * The exit brake must run in the unoccupied branch. The previous
			 * implementation set this flag during dismount, but only consumed it
			 * inside applyThrottleAndBrakes(), which requires a seated driver.
			 */
			if (this.exitBrakingActive) {
				forwardSpeed = this.applyExitParkingBrake(forwardSpeed);
			} else {
				forwardSpeed *= this.vehicleSpec.rollingResistance();
			}
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
		Vec3d attemptedMovement = this.getVelocity();
		if (this.moveWithCollisionSubsteps(attemptedMovement)) {
			/*
			 * Remember which end reached the obstacle. While the driver keeps holding
			 * throttle into that same obstacle, do not rebuild a small velocity and
			 * collide again every server tick. Reverse remains immediately available.
			 */
			this.bumperContactDirection = this.getMovementDirection(attemptedMovement, forward);
			this.setVelocity(0.0, this.getVelocity().y, 0.0);
			this.lastForwardSpeed = 0.0;
		}
	}

	private int getMovementDirection(Vec3d movement, Vec3d forward) {
		double forwardAmount = movement.x * forward.x + movement.z * forward.z;
		return forwardAmount >= 0.0 ? 1 : -1;
	}

	/** Clears a remembered contact once the bumper has room to move again. */
	private void refreshBumperContact(Vec3d forward) {
		if (this.bumperContactDirection == 0) {
			return;
		}

		Vec3d recheckMovement = forward.multiply(
				BUMPER_CONTACT_RECHECK_DISTANCE * this.bumperContactDirection
		);
		if (!this.isBumperBlocked(recheckMovement)) {
			this.bumperContactDirection = 0;
		}
	}

	private boolean isThrottleHeldAgainstBumper(double forwardSpeed) {
		if (this.bumperContactDirection > 0) {
			return this.pressingForward && !this.pressingBack && forwardSpeed >= -0.03;
		}
		if (this.bumperContactDirection < 0) {
			return this.pressingBack && !this.pressingForward && forwardSpeed <= 0.03;
		}
		return false;
	}

	private double holdAtBumperContact() {
		this.lastForwardSpeed = 0.0;
		this.engineRpm += (this.vehicleSpec.idleRpm() - this.engineRpm) * 0.35;
		return 0.0;
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
			if (this.isBumperBlocked(resolvedStep)) {
				this.moveUpToBumperContact(resolvedStep);
				return true;
			}
			this.move(MovementType.SELF, resolvedStep);
			if (this.horizontalCollision) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Finds the final safe fraction of a blocked movement step. Previously the
	 * complete substep was discarded, which could leave up to one substep of air
	 * between the visible bumper and a wall. A short binary search gives precise
	 * contact without requiring many tiny movement steps every tick.
	 */
	private void moveUpToBumperContact(Vec3d blockedStep) {
		double safeFraction = 0.0;
		double blockedFraction = 1.0;

		for (int search = 0; search < BUMPER_CONTACT_SEARCH_STEPS; search++) {
			double candidateFraction = (safeFraction + blockedFraction) * 0.5;
			Vec3d candidateStep = blockedStep.multiply(candidateFraction);
			if (this.isBumperBlocked(candidateStep)) {
				blockedFraction = candidateFraction;
			} else {
				safeFraction = candidateFraction;
			}
		}

		Vec3d safeStep = blockedStep.multiply(safeFraction);
		if (safeStep.horizontalLengthSquared() > MIN_BUMPER_CONTACT_MOVEMENT * MIN_BUMPER_CONTACT_MOVEMENT) {
			this.move(MovementType.SELF, safeStep);
		}
	}

	/**
	 * Minecraft entity dimensions are always axis-aligned, so making the base
	 * box four blocks long would also make the car four blocks wide. These three
	 * small oriented probes instead represent the active front or rear bumper.
	 * Low road edges are ignored here and remain the responsibility of the wheel
	 * step solver; taller collision shapes stop the car at the visible bumper.
	 */
	private boolean isBumperBlocked(Vec3d movementStep) {
		if (movementStep.horizontalLengthSquared() < 0.000001) {
			return false;
		}

		float yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		Vec3d forward = new Vec3d(-MathHelper.sin(yawRadians), 0.0, MathHelper.cos(yawRadians));
		Vec3d side = new Vec3d(forward.z, 0.0, -forward.x);
		double direction = movementStep.x * forward.x + movementStep.z * forward.z >= 0.0 ? 1.0 : -1.0;
		double bodyBottom = this.getBoundingBox().minY;

		for (double sideOffset : new double[] {-BUMPER_SIDE_OFFSET, 0.0, BUMPER_SIDE_OFFSET}) {
			double x = this.getX() + movementStep.x
					+ forward.x * BUMPER_FORWARD_OFFSET * direction + side.x * sideOffset;
			double z = this.getZ() + movementStep.z
					+ forward.z * BUMPER_FORWARD_OFFSET * direction + side.z * sideOffset;
			Box probe = new Box(
					x - BUMPER_SENSOR_RADIUS,
					bodyBottom + 0.08,
					z - BUMPER_SENSOR_RADIUS,
					x + BUMPER_SENSOR_RADIUS,
					bodyBottom + 0.82,
					z + BUMPER_SENSOR_RADIUS
			);

			for (VoxelShape collision : this.getWorld().getBlockCollisions(this, probe)) {
				if (collision.getBoundingBox().maxY > bodyBottom + STEP_HEIGHT + 0.02) {
					return true;
				}
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
		double leadingDistance = WHEEL_FORWARD_OFFSET + horizontalDistance;
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
		if (this.isAutomaticTransmission()) {
			this.updateAutomaticTransmission(speed);
		} else if (this.shiftTicksRemaining > 0) {
			this.shiftTicksRemaining--;
		}
		boolean shifting = this.shiftTicksRemaining > 0;

		if (this.isManualTransmission()) {
			// Manual selector order: R (-1), N (0), 1, 2, 3, 4, 5, 6.
			this.reverseEngaged = this.currentGear == -1;
			if (this.currentGear == 0) {
				// Neutral disconnects the engine from the wheels.
				if (speed > 0.03 && this.pressingBack) speed = Math.max(0.0, speed - this.getBrakeForce(speed, grip));
				else if (speed < -0.03 && this.pressingForward) speed = Math.min(0.0, speed + this.getBrakeForce(speed, grip));
				else speed *= this.vehicleSpec.rollingResistance();
			} else if (this.currentGear == -1) {
				if (speed > 0.03 && this.pressingBack) speed = Math.max(0.0, speed - this.getBrakeForce(speed, grip));
				else if (this.pressingBack && !this.pressingForward && !shifting) speed -= this.getDrivetrainAcceleration(speed, grip, true);
				else if (speed < -0.03 && this.pressingForward) speed = Math.min(0.0, speed + this.getBrakeForce(speed, grip));
				else {
					speed *= this.vehicleSpec.rollingResistance();
					speed = this.moveTowardZero(speed, this.vehicleSpec.lowSpeedCoastBrake() * this.vehicleSpec.reverseCoastBrakeMultiplier());
				}
			} else {
				if (speed < -0.03 && this.pressingForward) speed = Math.min(0.0, speed + this.getBrakeForce(speed, grip));
				else if (this.pressingForward && !this.pressingBack && !shifting) {
					double acceleration = this.getDrivetrainAcceleration(speed, grip, false);
					acceleration *= this.getManualHighGearLaunchScale(speed);
					acceleration *= this.getManualLimiterTorqueScale();
					speed += acceleration;
				}
				else if (speed > 0.03 && this.pressingBack) speed = Math.max(0.0, speed - this.getBrakeForce(speed, grip));
				else {
					speed *= this.vehicleSpec.rollingResistance();
					speed = this.moveTowardZero(speed, this.getManualEngineBrakeForce(speed));
				}
			}
		} else {
			// Existing automatic behavior.
			if (this.pressingForward && !this.pressingBack) {
				if (speed < -0.03) { speed = Math.min(0.0, speed + this.getBrakeForce(speed, grip)); this.reverseEngaged = speed < 0.0; }
				else if (!shifting) { this.reverseEngaged = false; speed += this.getDrivetrainAcceleration(speed, grip, false); }
			} else if (this.pressingBack && !this.pressingForward) {
				if (speed > 0.03) { speed = Math.max(0.0, speed - this.getBrakeForce(speed, grip)); this.reverseEngaged = false; }
				else if (!shifting) { this.reverseEngaged = true; speed -= this.getDrivetrainAcceleration(speed, grip, true); }
			} else {
				speed *= this.vehicleSpec.rollingResistance();
				double ratio = this.reverseEngaged ? this.vehicleSpec.reverseRatio() : this.vehicleSpec.gearRatio(this.currentGear);
				speed = this.moveTowardZero(speed, this.vehicleSpec.engineBrakeForce() * (ratio / this.vehicleSpec.gearRatio(1)));
				double lowSpeedFactor = 1.0 - Math.min(Math.abs(speed) / this.vehicleSpec.lowSpeedCoastThreshold(), 1.0);
				double coastBrake = this.vehicleSpec.lowSpeedCoastBrake() * lowSpeedFactor;
				if (this.reverseEngaged) coastBrake *= this.vehicleSpec.reverseCoastBrakeMultiplier();
				speed = this.moveTowardZero(speed, coastBrake);
			}
		}

		if (Math.abs(speed) < this.vehicleSpec.automaticStopSpeed() && !this.pressingForward && !this.pressingBack) {
			speed = 0.0;
			this.exitBrakingActive = false;
		}
		if (shifting) speed *= 0.998;
		double drag = this.vehicleSpec.aerodynamicDrag() * speed * speed;
		speed = this.moveTowardZero(speed, drag);
		speed = MathHelper.clamp(speed, -this.vehicleSpec.maxReverseSpeed(), this.vehicleSpec.maxForwardSpeed());
		this.lastForwardSpeed = speed;
		this.updateEngineRpm(speed, shifting);
		return speed;
	}

	/**
	 * Applies a strong speed-proportional parking brake after the driver exits.
	 * Cars at or below 10 km/h snap to rest; faster cars shed roughly 22% of
	 * their remaining forward speed each tick and settle within about half a
	 * second even near maximum speed.
	 */
	private double applyExitParkingBrake(double speed) {
		if (Math.abs(speed) <= EXIT_INSTANT_STOP_SPEED) {
			this.exitBrakingActive = false;
			this.lastForwardSpeed = 0.0;
			return 0.0;
		}

		double exitBrake = Math.max(
				EXIT_BRAKE_MIN_FORCE,
				Math.abs(speed) * EXIT_BRAKE_SPEED_FRACTION
		);
		double brakedSpeed = this.moveTowardZero(speed, exitBrake);
		if (Math.abs(brakedSpeed) <= EXIT_INSTANT_STOP_SPEED) {
			this.exitBrakingActive = false;
			this.lastForwardSpeed = 0.0;
			return 0.0;
		}

		this.lastForwardSpeed = brakedSpeed;
		return brakedSpeed;
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

		double coupledRpm = this.calculateCoupledRpm(speed, this.vehicleSpec.gearRatio(this.currentGear));
		if (this.pressingForward) {
			if (coupledRpm >= this.vehicleSpec.upshiftRpm() && this.currentGear < this.vehicleSpec.gearCount()) {
				this.beginShift(this.currentGear + 1);
				return;
			}

			if (coupledRpm < this.vehicleSpec.kickdownRpm() && this.currentGear > 1) {
				double lowerGearRpm = this.calculateCoupledRpm(speed, this.vehicleSpec.gearRatio(this.currentGear - 1));
				if (lowerGearRpm < this.vehicleSpec.redlineRpm()) {
					this.beginShift(this.currentGear - 1);
					return;
				}
			}
		} else if (coupledRpm > this.vehicleSpec.economyUpshiftRpm() && this.currentGear < this.vehicleSpec.gearCount()) {
			// With binary W input, throttle release represents an economy upshift.
			this.beginShift(this.currentGear + 1);
			return;
		}

		if (coupledRpm < this.vehicleSpec.downshiftRpm() && this.currentGear > 1) {
			this.beginShift(this.currentGear - 1);
		}
	}

	private void beginShift(int newGear) {
		this.currentGear = MathHelper.clamp(newGear, 1, this.vehicleSpec.gearCount());
		this.shiftTicksRemaining = this.vehicleSpec.shiftDurationTicks();
	}

	public void manualShift(int direction) {
		if (!this.isManualTransmission() || direction == 0 || this.shiftTicksRemaining > 0) return;
		int targetGear = this.currentGear + Integer.signum(direction);
		if (targetGear < -1 || targetGear > this.vehicleSpec.gearCount()) return;
		this.currentGear = targetGear;
		this.reverseEngaged = targetGear == -1;
		this.shiftTicksRemaining = this.vehicleSpec.shiftDurationTicks();
	}

	public void selectManualGear(int gear) {
		if (!this.isManualTransmission() || this.shiftTicksRemaining > 0) {
			return;
		}
		int targetGear = MathHelper.clamp(gear, -1, this.vehicleSpec.gearCount());
		if (targetGear == this.currentGear) {
			return;
		}
		this.currentGear = targetGear;
		this.reverseEngaged = targetGear == -1;
		this.shiftTicksRemaining = this.vehicleSpec.shiftDurationTicks();
	}



	private double getManualHighGearLaunchScale(double speed) {
		if (!this.isManualTransmission() || this.currentGear <= 1) {
			return 1.0;
		}

		double speedKmh = Math.abs(speed) * 72.0;

		/*
		 * A real manual car will move if you start in a tall gear, but it feels
		 * bogged down. This gives each higher gear a weak low-speed region that
		 * fades away once road speed is appropriate for that gear.
		 */
		double usefulSpeedKmh = switch (this.currentGear) {
			case 2 -> 20.0;
			case 3 -> 38.0;
			case 4 -> 58.0;
			case 5 -> 78.0;
			default -> 95.0;
		};

		double minimumScale = switch (this.currentGear) {
			case 2 -> 0.62;
			case 3 -> 0.40;
			case 4 -> 0.27;
			case 5 -> 0.19;
			default -> 0.14;
		};

		double progress = MathHelper.clamp(speedKmh / usefulSpeedKmh, 0.0, 1.0);
		return minimumScale + (1.0 - minimumScale) * progress;
	}

	private double getManualEngineBrakeForce(double speed) {
		if (!this.isManualTransmission() || this.currentGear <= 0) {
			return 0.0;
		}

		double ratio = this.vehicleSpec.gearRatio(this.currentGear);
		double firstRatio = this.vehicleSpec.gearRatio(1);
		double ratioScale = ratio / firstRatio;

		/*
		 * First and second now slow the car noticeably on throttle lift, while
		 * high gears remain gentler. Speed adds a little extra braking without
		 * turning it into a second brake pedal.
		 */
		double gearScale = 0.48 + ratioScale * 1.55;
		double speedScale = 0.70 + Math.min(0.60, Math.abs(speed) * 0.55);
		return this.vehicleSpec.engineBrakeForce() * gearScale * speedScale;
	}

	private double getManualLimiterTorqueScale() {
		if (!this.isManualTransmission() || this.currentGear <= 0) {
			return 1.0;
		}

		double limiter = this.vehicleSpec.revLimiterRpm();
		double softStart = limiter - 450.0;

		if (this.engineRpm <= softStart) {
			return 1.0;
		}
		if (this.engineRpm >= limiter) {
			return 0.0;
		}

		double t = (this.engineRpm - softStart) / (limiter - softStart);
		return 1.0 - t;
	}

	private double getDrivetrainAcceleration(double speed, double grip, boolean reverse) {
		return this.vehicleSpec.drivetrainAcceleration(speed, grip, this.currentGear, reverse);
	}

	private double calculateCoupledRpm(double speed, double ratio) {
		return this.vehicleSpec.calculateCoupledRpm(speed, ratio);
	}

	private double getBrakeForce(double speed, double grip) {
		double speedKmh = Math.abs(speed) * 72.0;
		double lowSpeedAmount = 1.0 - MathHelper.clamp(speedKmh / 35.0, 0.0, 1.0);
		double lowSpeedBoost = 1.0 + 0.90 * lowSpeedAmount;
		return this.vehicleSpec.brakeForce() * grip * lowSpeedBoost;
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
		double targetRpm;
		if (this.isManualTransmission() && this.currentGear == 0) {
			boolean revving = this.pressingForward || this.pressingBack;
			targetRpm = revving ? Math.min(this.vehicleSpec.revLimiterRpm(), 3200.0) : this.vehicleSpec.idleRpm();
		} else {
			double ratio = (this.isManualTransmission() && this.currentGear == -1) || this.reverseEngaged
					? this.vehicleSpec.reverseRatio() : this.vehicleSpec.gearRatio(Math.max(1, this.currentGear));
			targetRpm = shifting ? Math.max(this.vehicleSpec.idleRpm(), this.engineRpm * 0.86)
					: Math.min(this.vehicleSpec.revLimiterRpm(), this.calculateCoupledRpm(speed, ratio));
		}
		this.engineRpm += (targetRpm - this.engineRpm) * 0.35;
	}

	private void applySteering(double forwardSpeed, double grip) {
		int steeringInput = (this.pressingRight ? 1 : 0) - (this.pressingLeft ? 1 : 0);
		if (steeringInput == 0 || Math.abs(forwardSpeed) < 0.01) {
			return;
		}

		double speedRatio = Math.min(Math.abs(forwardSpeed) / 0.25, 1.0);
		double highSpeedReduction = 1.0 - 0.55 * Math.min(Math.abs(forwardSpeed) / this.vehicleSpec.maxForwardSpeed(), 1.0);
		double direction = Math.signum(forwardSpeed);
		float yawChange = (float) (steeringInput * direction * this.vehicleSpec.maxSteeringPerTick() * speedRatio * highSpeedReduction * grip);
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
		return 1.0 - (1.0 - this.vehicleSpec.lateralVelocityRetained()) * this.currentGrip;
	}

	private boolean shouldUseSyncedVisualState() {
		return this.getWorld().isClient && !this.clientPredictionActive;
	}

	public VehicleSpec getVehicleSpec() {
		String trackedId = this.dataTracker.get(VEHICLE_SPEC_ID);
		VehicleSpec trackedSpec = VehicleSpec.fromId(trackedId);
		this.vehicleSpec = trackedSpec;
		return trackedSpec;
	}

	public void setVehicleSpec(VehicleSpec vehicleSpec) {
		VehicleSpec resolved = vehicleSpec == null ? VehicleSpec.HATCHBACK_AUTOMATIC : vehicleSpec;
		this.vehicleSpec = resolved;
		this.dataTracker.set(VEHICLE_SPEC_ID, resolved.id());
		this.currentGear = resolved.isManual()
				? MathHelper.clamp(this.currentGear, -1, resolved.gearCount())
				: MathHelper.clamp(this.currentGear, 1, resolved.gearCount());
		this.engineRpm = Math.max(resolved.idleRpm(), this.engineRpm);
	}

	public boolean isManualTransmission() {
		return this.getVehicleSpec().isManual();
	}

	public boolean isAutomaticTransmission() {
		return this.getVehicleSpec().isAutomatic();
	}

	public double getHorizontalSpeedKmh() {
		// One block is treated as one metre; Minecraft runs at 20 ticks per second.
		return this.getVelocity().horizontalLength() * 20.0 * 3.6;
	}

	public int getEngineRpm() {
		double rpm = this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_ENGINE_RPM) : this.engineRpm;
		return (int) Math.round(rpm / 50.0) * 50;
	}

	public boolean hasThrottleInput() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_THROTTLE)
				: this.pressingForward || this.pressingBack;
	}

	public boolean isChangingGear() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_SHIFTING)
				: this.shiftTicksRemaining > 0;
	}

	public boolean isReverseEngaged() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_REVERSE)
				: this.reverseEngaged;
	}

	/** Server-synchronized steering input used to animate every client's front wheels. */
	public float getVisualSteeringInput() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_STEERING)
				: this.calculateVisualSteeringInput();
	}

	private float calculateVisualSteeringInput() {
		if (this.pressingLeft == this.pressingRight) {
			return 0.0F;
		}
		return this.pressingLeft ? 1.0F : -1.0F;
	}

	public boolean isBrakeInputActive() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_BRAKING)
				: this.calculateBrakeInputActive();
	}

	private boolean calculateBrakeInputActive() {
		return (this.lastForwardSpeed > 0.03 && this.pressingBack)
				|| (this.lastForwardSpeed < -0.03 && this.pressingForward);
	}

	public boolean areHeadlightsOn() {
		return this.dataTracker.get(HEADLIGHTS_ON);
	}

	public void setHeadlightsOn(boolean headlightsOn) {
		this.dataTracker.set(HEADLIGHTS_ON, headlightsOn);
	}

	public boolean isTransmissionShifting() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_SHIFTING)
				: this.shiftTicksRemaining > 0;
	}

	/**
	 * Returns the selected/target gear even while the transmission is in its
	 * short shift transition. This lets the HUD show where the shift is going.
	 */
	public String getSelectedGearDisplay() {
		boolean reverse = this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_REVERSE)
				: this.reverseEngaged;
		double forwardSpeed = this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_FORWARD_SPEED)
				: this.lastForwardSpeed;
		int gear = this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_GEAR)
				: this.currentGear;

		if (this.isManualTransmission()) {
			if (gear < 0) return "R";
			if (gear == 0) return "N";
			return Integer.toString(gear);
		}
		if (reverse || forwardSpeed < -0.01) return "R";
		return "D" + Math.max(1, gear);
	}

	public String getGearDisplay() {
		boolean shifting = this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_SHIFTING) : this.shiftTicksRemaining > 0;
		boolean reverse = this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_REVERSE) : this.reverseEngaged;
		double forwardSpeed = this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_FORWARD_SPEED) : this.lastForwardSpeed;
		int gear = this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_GEAR) : this.currentGear;
		if (this.isManualTransmission()) {
			if (gear < 0) return "R";
			if (gear == 0 || shifting) return "N";
			return Integer.toString(gear);
		}
		if (shifting) return "N";
		if (reverse || forwardSpeed < -0.01) return "R";
		return "D" + gear;
	}

	public double getCurrentGrip() {
		return this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_GRIP) : this.currentGrip;
	}

	public String getCurrentSurfaceName() {
		return this.shouldUseSyncedVisualState() ? this.dataTracker.get(SYNCED_SURFACE) : this.currentSurfaceName;
	}

	public String getCurrentRoadConditionName() {
		return this.shouldUseSyncedVisualState()
				? this.dataTracker.get(SYNCED_ROAD_CONDITION)
				: this.currentRoadConditionName;
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
