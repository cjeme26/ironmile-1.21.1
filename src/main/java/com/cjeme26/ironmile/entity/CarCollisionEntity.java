package com.cjeme26.ironmile.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Invisible physical body segment used to approximate the long CC0 hatchback.
 *
 * <p>Minecraft entity boxes are axis-aligned, so one normal CarEntity box cannot
 * represent a long vehicle without also becoming absurdly wide. Two smaller
 * collision segments follow the front and rear of the car instead.</p>
 */
public final class CarCollisionEntity extends Entity {
	private static final TrackedData<Integer> PARENT_ID = DataTracker.registerData(
			CarCollisionEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final TrackedData<Float> FORWARD_OFFSET = DataTracker.registerData(
			CarCollisionEntity.class,
			TrackedDataHandlerRegistry.FLOAT
	);
	private static final TrackedData<Float> SIDE_OFFSET = DataTracker.registerData(
			CarCollisionEntity.class,
			TrackedDataHandlerRegistry.FLOAT
	);

	public CarCollisionEntity(EntityType<? extends CarCollisionEntity> type, World world) {
		super(type, world);
		this.setNoGravity(true);
	}

	public void attachTo(CarEntity car, float forwardOffset, float sideOffset) {
		this.dataTracker.set(PARENT_ID, car.getId());
		this.dataTracker.set(FORWARD_OFFSET, forwardOffset);
		this.dataTracker.set(SIDE_OFFSET, sideOffset);
		this.follow(car);
	}

	public boolean belongsTo(CarEntity car) {
		return this.dataTracker.get(PARENT_ID) == car.getId();
	}

	private CarEntity getParentCar() {
		Entity entity = this.getWorld().getEntityById(this.dataTracker.get(PARENT_ID));
		return entity instanceof CarEntity car ? car : null;
	}

	private void follow(CarEntity car) {
		float yawRadians = car.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		Vec3d forward = new Vec3d(
				-MathHelper.sin(yawRadians),
				0.0,
				MathHelper.cos(yawRadians)
		);
		Vec3d right = new Vec3d(forward.z, 0.0, -forward.x);
		double forwardOffset = this.dataTracker.get(FORWARD_OFFSET);
		double sideOffset = this.dataTracker.get(SIDE_OFFSET);

		this.setPosition(
				car.getX() + forward.x * forwardOffset + right.x * sideOffset,
				car.getBoundingBox().minY + 0.02,
				car.getZ() + forward.z * forwardOffset + right.z * sideOffset
		);
		this.setYaw(car.getYaw());
		this.setPitch(0.0F);
		this.setVelocity(Vec3d.ZERO);
	}

	@Override
	public void tick() {
		super.tick();

		CarEntity car = this.getParentCar();
		if (car != null && !car.isRemoved()) {
			this.follow(car);
			return;
		}

		/*
		 * On the client the parent spawn packet can arrive a moment after the
		 * collision segment. Wait for tracking to settle. The server owns cleanup.
		 */
		if (!this.getWorld().isClient && this.age > 5) {
			this.discard();
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(PARENT_ID, -1);
		builder.add(FORWARD_OFFSET, 0.0F);
		builder.add(SIDE_OFFSET, 0.0F);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public boolean isCollidable() {
		return true;
	}

	@Override
	public boolean collidesWith(Entity other) {
		int parentId = this.dataTracker.get(PARENT_ID);

		if (other.getId() == parentId || other instanceof CarCollisionEntity) {
			return false;
		}

		Entity vehicle = other.getVehicle();
		if (vehicle != null && vehicle.getId() == parentId) {
			return false;
		}

		return BoatEntity.canCollide(this, other);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canHit() {
		// Never steal right-clicks/punches from the actual CarEntity.
		return false;
	}
}
