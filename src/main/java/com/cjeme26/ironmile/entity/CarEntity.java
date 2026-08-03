package com.cjeme26.ironmile.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.world.World;

/**
 * The first Iron Mile vehicle prototype.
 *
 * <p>For this milestone we deliberately build on Minecraft's proven boat
 * passenger and input handling. That gives us mounting, dismounting and
 * multiplayer-safe W/A/S/D controls while we establish a working vehicle.
 * Custom road physics will replace these movement rules later.</p>
 */
public class CarEntity extends BoatEntity {
	public CarEntity(EntityType<? extends BoatEntity> entityType, World world) {
		super(entityType, world);
	}
}
