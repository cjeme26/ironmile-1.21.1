package com.cjeme26.ironmile;

import net.fabricmc.api.ModInitializer;

import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.item.ModItems;
import com.cjeme26.ironmile.sound.ModSounds;
import com.cjeme26.ironmile.network.HeadlightTogglePayload;
import com.cjeme26.ironmile.network.CarInputPayload;
import com.cjeme26.ironmile.block.ModBlocks;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IronMile implements ModInitializer {
	public static final String MOD_ID = "ironmile";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		ModSounds.initialize();
		PayloadTypeRegistry.playC2S().register(HeadlightTogglePayload.ID, HeadlightTogglePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CarInputPayload.ID, CarInputPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(HeadlightTogglePayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof com.cjeme26.ironmile.entity.CarEntity car
					&& car.getControllingPassenger() == context.player()) {
				car.setHeadlightsOn(!car.areHeadlightsOn());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(CarInputPayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (!(entity instanceof com.cjeme26.ironmile.entity.CarEntity car)) {
				return;
			}

			boolean isCurrentDriver = car.getControllingPassenger() == context.player();
			boolean isNearbyDismountRelease = payload.inputMask() == 0
					&& !car.hasControllingPassenger()
					&& car.squaredDistanceTo(context.player()) <= 25.0;

			if (isCurrentDriver || isNearbyDismountRelease) {
				car.setInputs(payload.left(), payload.right(), payload.forward(), payload.back());
			}
		});
		LOGGER.info("Iron Mile prototype initialized");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
