package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.IronMile;
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

	private ModItems() {
	}

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.add(CAR));
	}
}
