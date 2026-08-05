package com.cjeme26.ironmile.client.mixin;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides both first-person hands and held items while driving an Iron Mile car. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ironmile$hideFirstPersonHandsWhileDriving(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        if (player.getVehicle() instanceof CarEntity) {
            ci.cancel();
        }
    }
}
