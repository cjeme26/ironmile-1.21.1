package com.cjeme26.ironmile.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class VehiclePartItem extends Item {
	private final String descriptionKey;

	public VehiclePartItem(String descriptionKey, Settings settings) {
		super(settings);
		this.descriptionKey = descriptionKey;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.ironmile.brand").formatted(Formatting.BLUE, Formatting.ITALIC));
		tooltip.add(Text.translatable(this.descriptionKey).formatted(Formatting.GRAY));
	}
}
