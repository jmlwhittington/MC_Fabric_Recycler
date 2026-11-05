
package com.recycler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RecyclerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public RecyclerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecyclerBlockEntity(pos, state);
    }
	
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level,
			BlockState state,
			BlockEntityType<T> type
	) {
		if (level.isClientSide() || !type.equals(ModBlocks.RECYCLER_BLOCK_ENTITY)) {
			return null;
		}

		return (tickLevel, pos, blockState, blockEntity) -> {
			if (blockEntity instanceof RecyclerBlockEntity recycler) {
				RecyclerBlockEntity.serverTick(tickLevel, pos, blockState, recycler);
			}
		};
	}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
