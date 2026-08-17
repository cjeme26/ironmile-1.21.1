package com.cjeme26.ironmile.client.mixin;

import com.cjeme26.ironmile.client.IronMileClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void ironmile$cancelAttackWhileDriving(CallbackInfoReturnable<Boolean> cir) {
		if (IronMileClient.shouldSuppressCarAttack()) {
			IronMileClient.handleCarAttackInput();
			// Left click is reserved from normal world interaction while driving.
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void ironmile$cancelHeldBlockBreakingWhileDriving(boolean breaking, CallbackInfo ci) {
		if (IronMileClient.shouldSuppressCarAttack()) {
			// Also stop the held-left-click block breaking path.
			ci.cancel();
		}
	}

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
