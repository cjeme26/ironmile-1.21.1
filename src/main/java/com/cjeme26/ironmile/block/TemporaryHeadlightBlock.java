package com.cjeme26.ironmile.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public final class TemporaryHeadlightBlock extends Block {
	private static final int CLEANUP_DELAY_TICKS = 3;

	public TemporaryHeadlightBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		this.refreshOrRemove(world, pos);
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		this.refreshOrRemove(world, pos);
	}

	private void refreshOrRemove(ServerWorld world, BlockPos pos) {
		if (world.getBlockState(pos).isOf(this)) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
		}
	}
}
