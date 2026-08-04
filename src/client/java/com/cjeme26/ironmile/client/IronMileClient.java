package com.cjeme26.ironmile.client;

import com.cjeme26.ironmile.client.render.CarEntityRenderer;
import com.cjeme26.ironmile.client.sound.EngineSoundManager;
import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.network.HeadlightTogglePayload;
import com.cjeme26.ironmile.network.CarInputPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class IronMileClient implements ClientModInitializer {
	private static KeyBinding toggleHeadlightsKey;
	private static int lastControlledCarId = -1;

	@Override
	public void onInitializeClient() {
		toggleHeadlightsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ironmile.toggle_headlights",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"key.category.ironmile"
		));
		EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.HEADLIGHT_MARKER, EmptyEntityRenderer::new);
		ClientTickEvents.END_CLIENT_TICK.register(EngineSoundManager::tick);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) {
				lastControlledCarId = -1;
				return;
			}

			if (client.player.getVehicle() instanceof CarEntity car) {
				int inputMask = 0;
				if (client.options.leftKey.isPressed()) inputMask |= CarInputPayload.LEFT;
				if (client.options.rightKey.isPressed()) inputMask |= CarInputPayload.RIGHT;
				if (client.options.forwardKey.isPressed()) inputMask |= CarInputPayload.FORWARD;
				if (client.options.backKey.isPressed()) inputMask |= CarInputPayload.BACK;

				lastControlledCarId = car.getId();
				ClientPlayNetworking.send(new CarInputPayload(car.getId(), inputMask));
				return;
			}

			/*
			 * Send one explicit neutral state on the first tick after dismounting.
			 * Without this, releasing control while A or D is held can leave the
			 * server with the final steering packet indefinitely.
			 */
			if (lastControlledCarId != -1) {
				ClientPlayNetworking.send(new CarInputPayload(lastControlledCarId, 0));
				lastControlledCarId = -1;
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleHeadlightsKey.wasPressed()) {
				if (client.player != null && client.player.getVehicle() instanceof CarEntity car) {
					ClientPlayNetworking.send(new HeadlightTogglePayload(car.getId()));
				}
			}
		});
		HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && client.player.getVehicle() instanceof com.cjeme26.ironmile.entity.CarEntity car) {
				String speed = String.format(Locale.ROOT, "Iron Mile  |  %.0f km/h", car.getHorizontalSpeedKmh());
				String drivetrain = String.format(
						Locale.ROOT,
						"Gear: %s  |  %,d RPM",
						car.getGearDisplay(),
						car.getEngineRpm()
				);
				String surface = String.format(
						Locale.ROOT,
						"Surface: %s  |  Grip: %.0f%%",
						car.getCurrentSurfaceName(),
						car.getCurrentGrip() * 100.0
				);
				drawContext.drawTextWithShadow(client.textRenderer, Text.literal(speed), 10, 10, 0xFFFFFF);
				drawContext.drawTextWithShadow(client.textRenderer, Text.literal(drivetrain), 10, 22, 0xFFD58A);
				drawContext.drawTextWithShadow(client.textRenderer, Text.literal(surface), 10, 34, 0xD7D7D7);
				drawContext.drawTextWithShadow(
						client.textRenderer,
						Text.literal("Condition: " + car.getCurrentRoadConditionName()),
						10,
						46,
						0xAFC7E8
				);
				drawContext.drawTextWithShadow(
						client.textRenderer,
						Text.literal("Tires: " + car.getTireType().getDisplayName()),
						10,
						58,
						0xB8D8FF
				);
				drawContext.drawTextWithShadow(
						client.textRenderer,
						Text.literal("Headlights: " + (car.areHeadlightsOn() ? "On" : "Off")),
						10,
						70,
						car.areHeadlightsOn() ? 0xFFF2B2 : 0x888888
				);
			}
		});
	}
}
