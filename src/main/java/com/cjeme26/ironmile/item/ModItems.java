package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.IronMile;
import com.cjeme26.ironmile.entity.CarEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
	public static final Item CAR = Registry.register(
			Registries.ITEM,
			IronMile.id("car"),
			new CarItem(new Item.Settings().maxCount(1))
	);
	public static final Item SUMMER_TIRES = registerTires("summer_tires", CarEntity.TireType.SUMMER);
	public static final Item ALL_SEASON_TIRES = registerTires("all_season_tires", CarEntity.TireType.ALL_SEASON);
	public static final Item WINTER_TIRES = registerTires("winter_tires", CarEntity.TireType.WINTER);

	private ModItems() {
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> {
					entries.add(CAR);
					entries.add(SUMMER_TIRES);
					entries.add(ALL_SEASON_TIRES);
					entries.add(WINTER_TIRES);
				});
	}

	private static Item registerTires(String id, CarEntity.TireType tireType) {
		return Registry.register(
				Registries.ITEM,
				IronMile.id(id),
				new TireItem(tireType, new Item.Settings().maxCount(1))
		);
	}
}
