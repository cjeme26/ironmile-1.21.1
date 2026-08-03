package com.cjeme26.ironmile.client.render;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Renders the prototype as a deliberately simple iron-block car body. */
public class CarEntityRenderer extends EntityRenderer<CarEntity> {
	private final BlockRenderManager blockRenderManager;

	public CarEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.blockRenderManager = context.getBlockRenderManager();
		this.shadowRadius = 1.1F;
	}

	@Override
	public void render(
			CarEntity car,
			float yaw,
			float tickDelta,
			MatrixStack matrices,
			VertexConsumerProvider vertexConsumers,
			int light
	) {
		matrices.push();
		matrices.translate(0.0, 0.42, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
		matrices.scale(1.75F, 0.65F, 2.8F);
		matrices.translate(-0.5, -0.5, -0.5);
		blockRenderManager.renderBlockAsEntity(
				Blocks.IRON_BLOCK.getDefaultState(),
				matrices,
				vertexConsumers,
				light,
				OverlayTexture.DEFAULT_UV
		);
		matrices.pop();
		super.render(car, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(CarEntity entity) {
		return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
	}
}
