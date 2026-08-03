package com.cjeme26.ironmile.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Invisible client-visible anchor used by LambDynamicLights.
 * It never collides, renders, saves, drops, or changes the world.
 */
public final class HeadlightMarkerEntity extends Entity {
	private static final double FORWARD_OFFSET = 1.35;
	private static final double HEIGHT_OFFSET = 0.55;

	private CarEntity car;

	public HeadlightMarkerEntity(EntityType<? extends HeadlightMarkerEntity> type, World world) {
		super(type, world);
		this.noClip = true;
		this.setNoGravity(true);
		// Do not set Minecraft's invisible flag here. LambDynamicLights deliberately
		// ignores invisible entities. EmptyEntityRenderer still draws nothing.
	}

	public void setCar(CarEntity car) {
		this.car = car;
	}

	public void followCar() {
		if (this.car == null) {
			return;
		}
		float yawRadians = this.car.getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double forwardX = -MathHelper.sin(yawRadians);
		double forwardZ = MathHelper.cos(yawRadians);
		this.setPosition(
				this.car.getX() + forwardX * FORWARD_OFFSET,
				this.car.getBoundingBox().minY + HEIGHT_OFFSET,
				this.car.getZ() + forwardZ * FORWARD_OFFSET
		);
		this.setYaw(this.car.getYaw());
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.getWorld().isClient) {
			if (this.car == null || this.car.isRemoved() || !this.car.areHeadlightsOn()) {
				this.discard();
				return;
			}
			this.followCar();
		}
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}

	@Override
	public boolean isCollidable() {
		return false;
	}
}
