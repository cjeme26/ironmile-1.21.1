package com.cjeme26.ironmile.client.mixin;

import com.cjeme26.ironmile.client.IronMileClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void ironmile$cancelUseWhileOperatingManualShifter(CallbackInfo ci) {
		if (IronMileClient.shouldSuppressManualUse()) {
			/*
			 * The Use key belongs to IronMile while the player is driving a
			 * manual car, so vanilla must not place blocks / interact / use the
			 * held item at the same time.
			 */
			ci.cancel();
		}
	}
}
