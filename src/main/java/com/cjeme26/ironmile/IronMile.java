package com.cjeme26.ironmile;

import net.fabricmc.api.ModInitializer;

import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.item.ModItems;
import com.cjeme26.ironmile.item.ModItemGroups;
import com.cjeme26.ironmile.sound.ModSounds;
import com.cjeme26.ironmile.network.HeadlightTogglePayload;
import com.cjeme26.ironmile.network.CarInputPayload;
import com.cjeme26.ironmile.network.GearShiftPayload;
import com.cjeme26.ironmile.network.GearSelectPayload;
import com.cjeme26.ironmile.network.IgnitionTogglePayload;
import com.cjeme26.ironmile.network.ExitVehiclePayload;
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
		ModItemGroups.initialize();
		ModSounds.initialize();
		PayloadTypeRegistry.playC2S().register(HeadlightTogglePayload.ID, HeadlightTogglePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CarInputPayload.ID, CarInputPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GearShiftPayload.ID, GearShiftPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(GearSelectPayload.ID, GearSelectPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(IgnitionTogglePayload.ID, IgnitionTogglePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ExitVehiclePayload.ID, ExitVehiclePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(IgnitionTogglePayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof com.cjeme26.ironmile.entity.CarEntity car
					&& car.getControllingPassenger() == context.player()) {
				car.toggleIgnition();
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(ExitVehiclePayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof com.cjeme26.ironmile.entity.CarEntity car
					&& context.player().getVehicle() == car
					&& car.getControllingPassenger() == context.player()) {
				context.player().stopRiding();
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(HeadlightTogglePayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof com.cjeme26.ironmile.entity.CarEntity car
					&& car.getControllingPassenger() == context.player()) {
				car.setHeadlightsOn(!car.areHeadlightsOn());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(GearSelectPayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (entity instanceof com.cjeme26.ironmile.entity.CarEntity car
					&& car.getControllingPassenger() == context.player()
					&& car.isManualTransmission()) {
				car.selectManualGear(payload.gear());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(GearShiftPayload.ID, (payload, context) -> {
			var entity = context.player().getWorld().getEntityById(payload.entityId());
			if (!(entity instanceof com.cjeme26.ironmile.entity.CarEntity car)
					|| car.getControllingPassenger() != context.player()) {
				return;
			}

			if (car.isManualTransmission()) {
				car.manualShift(payload.direction());
			} else if (car.isAutomaticTransmission()) {
				/*
				 * Existing fallback controls are reused:
				 * R (direction +1) moves D -> N -> R -> P.
				 * F (direction -1) moves P -> R -> N -> D.
				 */
				car.automaticSelectorStep(-Integer.signum(payload.direction()));
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
				car.setInputs(payload.left(), payload.right(), payload.forward(), payload.back(), payload.clutch());
			}
		});
		LOGGER.info("Iron Mile initialized");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
