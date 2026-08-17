package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.IronMile;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {
	public static final ItemGroup IRON_MILE = Registry.register(
			Registries.ITEM_GROUP,
			IronMile.id("iron_mile"),
			FabricItemGroup.builder()
					.displayName(Text.translatable("itemGroup.ironmile"))
					.icon(() -> new ItemStack(ModItems.CAR))
					.entries((displayContext, entries) -> {
						entries.add(ModItems.CAR);
						entries.add(ModItems.CAR_MANUAL);
						entries.add(ModItems.SUMMER_TIRES);
						entries.add(ModItems.ALL_SEASON_TIRES);
						entries.add(ModItems.WINTER_TIRES);
					})
					.build()
	);

	private ModItemGroups() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering the Iron Mile creative inventory tab");
	}
}
