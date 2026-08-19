package com.cjeme26.ironmile.entity;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<CarEntity> CAR = Registry.register(
			Registries.ENTITY_TYPE,
			IronMile.id("car"),
			EntityType.Builder.<CarEntity>create(CarEntity::new, SpawnGroup.MISC)
					/*
					 * CC0 rendered shell is about 1.85 blocks wide and 1.50 high.
					 * The previous 0.9-high core sat far inside the visible body.
					 */
					.dimensions(0.72F, 1.45F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.build("ironmile:car")
	);
	public static final EntityType<CarCollisionEntity> CAR_CABIN_COLLISION = Registry.register(
			Registries.ENTITY_TYPE,
			IronMile.id("car_cabin_collision"),
			EntityType.Builder.<CarCollisionEntity>create(CarCollisionEntity::new, SpawnGroup.MISC)
					.dimensions(0.72F, 1.45F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.disableSaving()
					.disableSummon()
					.build("ironmile:car_cabin_collision")
	);

	public static final EntityType<CarCollisionEntity> CAR_HOOD_COLLISION = Registry.register(
			Registries.ENTITY_TYPE,
			IronMile.id("car_hood_collision"),
			EntityType.Builder.<CarCollisionEntity>create(CarCollisionEntity::new, SpawnGroup.MISC)
					/*
					 * Low front body / hood. Kept narrower than the visible shell
					 * so players do not stand on invisible air beside the car.
					 */
					.dimensions(0.72F, 0.76F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.disableSaving()
					.disableSummon()
					.build("ironmile:car_hood_collision")
	);

	public static final EntityType<CarCollisionEntity> CAR_REAR_COLLISION = Registry.register(
			Registries.ENTITY_TYPE,
			IronMile.id("car_rear_collision"),
			EntityType.Builder.<CarCollisionEntity>create(CarCollisionEntity::new, SpawnGroup.MISC)
					/*
					 * Rear hatch/body sits a little higher than the hood.
					 */
					.dimensions(0.72F, 0.96F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.disableSaving()
					.disableSummon()
					.build("ironmile:car_rear_collision")
	);

	public static final EntityType<HeadlightMarkerEntity> HEADLIGHT_MARKER = Registry.register(
			Registries.ENTITY_TYPE,
			IronMile.id("headlight_marker"),
			EntityType.Builder.<HeadlightMarkerEntity>create(HeadlightMarkerEntity::new, SpawnGroup.MISC)
					.dimensions(0.1F, 0.1F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.disableSaving()
					.disableSummon()
					.build("ironmile:headlight_marker")
	);

	private ModEntities() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering Iron Mile vehicles");
	}
}
