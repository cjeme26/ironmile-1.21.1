package com.cjeme26.ironmile.client;

import com.cjeme26.ironmile.client.render.CarEntityRenderer;
import com.cjeme26.ironmile.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;

public class IronMileClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
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
			}
		});
	}
}
