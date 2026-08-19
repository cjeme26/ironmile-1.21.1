package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.IronMile;
import com.cjeme26.ironmile.block.ModBlocks;
import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.entity.VehicleSpec;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	public static final Item MECHANICS_WORKBENCH = Registry.register(
			Registries.ITEM,
			IronMile.id("mechanics_workbench"),
			new BlockItem(ModBlocks.MECHANICS_WORKBENCH, new Item.Settings())
	);

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

	public static final Item HATCHBACK_BODY = Registry.register(
			Registries.ITEM,
			IronMile.id("hatchback_body"),
			new VehiclePartItem("tooltip.ironmile.hatchback_body", new Item.Settings().maxCount(1))
	);
	public static final Item MANUAL_TRANSMISSION = Registry.register(
			Registries.ITEM,
			IronMile.id("manual_transmission"),
			new TransmissionItem(true, new Item.Settings().maxCount(1))
	);
	public static final Item AUTOMATIC_TRANSMISSION = Registry.register(
			Registries.ITEM,
			IronMile.id("automatic_transmission"),
			new TransmissionItem(false, new Item.Settings().maxCount(1))
	);

	public static final Item SUMMER_TIRES = registerTires("summer_tires", CarEntity.TireType.SUMMER);
	public static final Item ALL_SEASON_TIRES = registerTires("all_season_tires", CarEntity.TireType.ALL_SEASON);
	public static final Item WINTER_TIRES = registerTires("winter_tires", CarEntity.TireType.WINTER);

	public static final Item SUMMER_TIRE_SET = registerTireSet("summer_tire_set", CarEntity.TireType.SUMMER);
	public static final Item ALL_SEASON_TIRE_SET = registerTireSet("all_season_tire_set", CarEntity.TireType.ALL_SEASON);
	public static final Item WINTER_TIRE_SET = registerTireSet("winter_tire_set", CarEntity.TireType.WINTER);

	private ModItems() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering Iron Mile items");
	}

	private static Item registerTires(String id, CarEntity.TireType tireType) {
		return Registry.register(
				Registries.ITEM,
				IronMile.id(id),
				new TireItem(tireType, new Item.Settings().maxCount(16))
		);
	}

	private static Item registerTireSet(String id, CarEntity.TireType tireType) {
		return Registry.register(
				Registries.ITEM,
				IronMile.id(id),
				new TireSetItem(tireType, new Item.Settings().maxCount(1))
		);
	}

	public static Item getTireItem(CarEntity.TireType tireType) {
		return switch (tireType) {
			case SUMMER -> SUMMER_TIRES;
			case ALL_SEASON -> ALL_SEASON_TIRES;
			case WINTER -> WINTER_TIRES;
		};
	}

	public static Item getTireSetItem(CarEntity.TireType tireType) {
		return switch (tireType) {
			case SUMMER -> SUMMER_TIRE_SET;
			case ALL_SEASON -> ALL_SEASON_TIRE_SET;
			case WINTER -> WINTER_TIRE_SET;
		};
	}
}
