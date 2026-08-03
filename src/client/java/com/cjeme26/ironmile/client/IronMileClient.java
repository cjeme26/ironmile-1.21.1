package com.cjeme26.ironmile.client;

import com.cjeme26.ironmile.client.render.CarEntityRenderer;
import com.cjeme26.ironmile.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class IronMileClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
	}
}
