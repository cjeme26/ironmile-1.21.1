package com.cjeme26.ironmile.item;

import com.cjeme26.ironmile.entity.CarEntity;
import com.cjeme26.ironmile.entity.ModEntities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CarItem extends Item {
	public CarItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		BlockPos spawnPos = context.getBlockPos().offset(context.getSide());

		if (!world.isClient) {
			CarEntity car = new CarEntity(ModEntities.CAR, world);
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
