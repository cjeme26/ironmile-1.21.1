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
					.dimensions(1.85F, 1.45F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.build("ironmile:car")
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
