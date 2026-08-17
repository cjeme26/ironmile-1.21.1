package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.IronMile;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.entity.VehicleSpec;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	/* Keep the original registry id for backwards compatibility. */
	public static final Item CAR = Registry.register(
			Registries.ITEM,
			IronMile.id("car"),
			new CarItem(VehicleSpec.HATCHBACK_AUTOMATIC, new Item.Settings().maxCount(1))
	);
	public static final Item CAR_MANUAL = Registry.register(
			Registries.ITEM,
			IronMile.id("car_manual"),
			new CarItem(VehicleSpec.HATCHBACK_MANUAL, new Item.Settings().maxCount(1))
	);
	public static final Item SUMMER_TIRES = registerTires("summer_tires", CarEntity.TireType.SUMMER);
	public static final Item ALL_SEASON_TIRES = registerTires("all_season_tires", CarEntity.TireType.ALL_SEASON);
	public static final Item WINTER_TIRES = registerTires("winter_tires", CarEntity.TireType.WINTER);

	private ModItems() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering Iron Mile items");
	}

	private static Item registerTires(String id, CarEntity.TireType tireType) {
		return Registry.register(
				Registries.ITEM,
				IronMile.id(id),
				new TireItem(tireType, new Item.Settings().maxCount(1))
		);
	}
}
