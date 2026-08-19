package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.entity.VehicleSpec;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class CarItem extends Item {
	private static final String INSTALLED_TIRE_KEY = "IronMileInstalledTireType";
	private static final String STORED_FUEL_KEY = "IronMileStoredFuelMilliliters";

	private final VehicleSpec vehicleSpec;

	public CarItem(VehicleSpec vehicleSpec, Settings settings) {
		super(settings);
		this.vehicleSpec = vehicleSpec;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		tooltip.add(Text.translatable("tooltip.ironmile.brand").formatted(Formatting.BLUE, Formatting.ITALIC));

		MutableText transmission = Text.translatable("tooltip.ironmile.transmission_label").formatted(Formatting.GRAY);
		transmission.append(Text.translatable(
				this.vehicleSpec.isManual() ? "tooltip.ironmile.manual" : "tooltip.ironmile.automatic"
		).formatted(this.vehicleSpec.isManual() ? Formatting.AQUA : Formatting.GOLD));
		tooltip.add(transmission);

		MutableText tires = Text.translatable("tooltip.ironmile.tires_label").formatted(Formatting.GRAY);
		tires.append(Text.literal(getInstalledTireType(stack).getDisplayName()).formatted(Formatting.WHITE));
		tooltip.add(tires);

		MutableText model = Text.translatable("tooltip.ironmile.model_label").formatted(Formatting.GRAY);
		model.append(Text.translatable("tooltip.ironmile.yellow_hatchback").formatted(Formatting.WHITE));
		tooltip.add(model);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		BlockPos spawnPos = context.getBlockPos().offset(context.getSide());

		if (!world.isClient) {
			CarEntity car = new CarEntity(ModEntities.CAR, world);
			car.setVehicleSpec(this.vehicleSpec);
			car.setTireType(getInstalledTireType(context.getStack()));
			car.setFuelMilliliters(getStoredFuelMilliliters(context.getStack()));
			float yaw = player == null ? 0.0F : player.getYaw();
			car.refreshPositionAndAngles(
					spawnPos.getX() + 0.5,
					spawnPos.getY(),
					spawnPos.getZ() + 0.5,
					yaw,
					0.0F
			);

			if (!world.isSpaceEmpty(car)) {
				return ActionResult.FAIL;
			}

			world.spawnEntity(car);
			if (player == null || !player.getAbilities().creativeMode) {
				context.getStack().decrement(1);
			}
		}

		return ActionResult.success(world.isClient);
	}

	public static void setInstalledTireType(ItemStack stack, CarEntity.TireType tireType) {
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt ->
				nbt.putInt(INSTALLED_TIRE_KEY, tireType.ordinal()));
	}

	public static CarEntity.TireType getInstalledTireType(ItemStack stack) {
		NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound nbt = data.copyNbt();
		if (!nbt.contains(INSTALLED_TIRE_KEY)) {
			return CarEntity.TireType.ALL_SEASON;
		}
		return CarEntity.TireType.fromOrdinal(nbt.getInt(INSTALLED_TIRE_KEY));
	}

	public static void setStoredFuelMilliliters(ItemStack stack, int fuelMilliliters) {
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt ->
				nbt.putInt(STORED_FUEL_KEY, Math.max(0, fuelMilliliters)));
	}

	public static int getStoredFuelMilliliters(ItemStack stack) {
		NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		NbtCompound nbt = data.copyNbt();
		if (!nbt.contains(STORED_FUEL_KEY)) {
			// Newly assembled cars and old car items with no stored-fuel field start empty.
			return 0;
		}
		return Math.max(0, nbt.getInt(STORED_FUEL_KEY));
	}
}
