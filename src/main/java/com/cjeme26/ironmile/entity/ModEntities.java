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
					.dimensions(1.8F, 0.9F)
					.maxTrackingRange(10)
					.trackingTickInterval(1)
					.build("ironmile:car")
	);

	private ModEntities() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering Iron Mile vehicles");
	}
}
