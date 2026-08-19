package com.cjeme26.ironmile.screen;

import com.cjeme26.ironmile.IronMile;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModScreenHandlers {
	public static final ExtendedScreenHandlerType<FuelScreenHandler, FuelScreenData> FUEL =
			Registry.register(
					Registries.SCREEN_HANDLER,
					IronMile.id("fuel"),
					new ExtendedScreenHandlerType<>(
							FuelScreenHandler::new,
							FuelScreenData.CODEC
					)
			);

	private ModScreenHandlers() {
	}

	public static void initialize() {
		// Static initialization registers the screen handler type.
	}
}
