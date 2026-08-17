package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.entity.ModEntities;
import com.cjeme26.ironmile.entity.VehicleSpec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

import java.util.List;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CarItem extends Item {
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
}
