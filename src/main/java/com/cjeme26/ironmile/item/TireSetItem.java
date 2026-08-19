package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class TireSetItem extends Item {
	private final CarEntity.TireType tireType;

	public TireSetItem(CarEntity.TireType tireType, Settings settings) {
		super(settings);
		this.tireType = tireType;
	}

	public CarEntity.TireType getTireType() {
		return this.tireType;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.ironmile.brand").formatted(Formatting.BLUE, Formatting.ITALIC));
		tooltip.add(Text.translatable("tooltip.ironmile.tire_set_four").formatted(Formatting.GRAY));
		tooltip.add(Text.literal(this.tireType.getDisplayName()).formatted(Formatting.WHITE));
	}
}
