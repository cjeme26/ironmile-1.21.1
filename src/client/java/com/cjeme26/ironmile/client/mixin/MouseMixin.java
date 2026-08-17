package com.cjeme26.ironmile.client.mixin;

import com.cjeme26.ironmile.client.IronMileClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Shadow private double cursorDeltaX;
	@Shadow private double cursorDeltaY;


	@Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
	private void ironmile$captureMouseForManualShifter(double timeDelta, CallbackInfo ci) {
		if (IronMileClient.shouldCaptureMouseForShifter()) {
			IronMileClient.handleShifterMouseDelta(this.cursorDeltaX, this.cursorDeltaY);
			// Cancelling here prevents changeLookDirection(), so neither first-
			// nor third-person camera movement occurs while operating the lever.
			ci.cancel();
		}
	}
}
