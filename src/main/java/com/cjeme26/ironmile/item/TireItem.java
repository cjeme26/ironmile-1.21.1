package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import net.minecraft.item.Item;

public class TireItem extends Item {
	private final CarEntity.TireType tireType;

	public TireItem(CarEntity.TireType tireType, Settings settings) {
		super(settings);
		this.tireType = tireType;
	}

	public CarEntity.TireType getTireType() {
		return this.tireType;
	}
}
