package com.cjeme26.ironmile.screen;

import com.cjeme26.ironmile.IronMile;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

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

	public static final ScreenHandlerType<MechanicsWorkbenchScreenHandler> MECHANICS_WORKBENCH =
			Registry.register(
					Registries.SCREEN_HANDLER,
					IronMile.id("mechanics_workbench"),
					new ScreenHandlerType<>(
							MechanicsWorkbenchScreenHandler::new,
							FeatureFlags.VANILLA_FEATURES
					)
			);

	private ModScreenHandlers() {
	}

	public static void initialize() {
		// Static initialization registers the screen handler types.
	}
}
