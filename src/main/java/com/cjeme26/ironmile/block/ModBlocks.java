package com.cjeme26.ironmile.block;

import com.cjeme26.ironmile.IronMile;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlocks {
	public static final Block MECHANICS_WORKBENCH = Registry.register(
			Registries.BLOCK,
			IronMile.id("mechanics_workbench"),
			new MechanicsWorkbenchBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE))
	);

	public static final Block TEMPORARY_HEADLIGHT = Registry.register(
			Registries.BLOCK,
			IronMile.id("temporary_headlight"),
			new TemporaryHeadlightBlock(
					AbstractBlock.Settings.create()
							.noCollision()
							.nonOpaque()
							.replaceable()
							.dropsNothing()
							.luminance(state -> 15)
							.ticksRandomly()
			)
	);

	private ModBlocks() {
	}

	public static void initialize() {
		IronMile.LOGGER.info("Registering Iron Mile blocks");
	}
}
