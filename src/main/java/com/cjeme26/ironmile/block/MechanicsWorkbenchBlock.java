package com.cjeme26.ironmile.block;

import com.cjeme26.ironmile.screen.MechanicsWorkbenchScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class MechanicsWorkbenchBlock extends CraftingTableBlock {
	public MechanicsWorkbenchBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected ActionResult onUse(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			BlockHitResult hit
	) {
		if (!world.isClient) {
			NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
					(syncId, inventory, openingPlayer) -> new MechanicsWorkbenchScreenHandler(
							syncId,
							inventory,
							ScreenHandlerContext.create(world, pos)
					),
					Text.translatable("container.ironmile.mechanics_workbench")
			);
			player.openHandledScreen(factory);
		}
		return ActionResult.success(world.isClient);
	}
}
