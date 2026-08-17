package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TireItem extends Item {
	private final CarEntity.TireType tireType;

	public TireItem(CarEntity.TireType tireType, Settings settings) {
		super(settings);
		this.tireType = tireType;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.ironmile.brand").formatted(Formatting.BLUE, Formatting.ITALIC));
		switch (this.tireType) {
			case SUMMER -> {
				tooltip.add(Text.translatable("tooltip.ironmile.tire_summer_line1").formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("tooltip.ironmile.tire_summer_line2").formatted(Formatting.DARK_GRAY));
			}
			case ALL_SEASON -> {
				tooltip.add(Text.translatable("tooltip.ironmile.tire_all_season_line1").formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("tooltip.ironmile.tire_all_season_line2").formatted(Formatting.DARK_GRAY));
			}
			case WINTER -> {
				tooltip.add(Text.translatable("tooltip.ironmile.tire_winter_line1").formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("tooltip.ironmile.tire_winter_line2").formatted(Formatting.DARK_GRAY));
			}
		}
	}

	public CarEntity.TireType getTireType() {
		return this.tireType;
	}
}
