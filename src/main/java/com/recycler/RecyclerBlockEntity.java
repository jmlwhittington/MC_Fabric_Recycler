package com.recycler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.storage.ValueInput;
import net.minecraft.world.storage.ValueOutput;
import net.minecraft.world.level.Level;

public class RecyclerBlockEntity extends BlockEntity implements Container, WorldlyContainer {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_SLOTS = 6;
    public static final int INVENTORY_SIZE = 1 + OUTPUT_SLOTS;
	
	public static final int PROCESSING_TIME = 100;

	private int processingTicks = 0;
	private boolean isProcessing = false;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOT_INDICES = {1, 2, 3, 4, 5, 6};

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RECYCLER_BLOCK_ENTITY, pos, state);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);

        if (!result.isEmpty()) {
            setChanged();
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);

		if (!stack.isEmpty()) {
			stack.limitSize(Math.min(getMaxStackSize(), stack.getMaxStackSize()));
		}

		if (slot == INPUT_SLOT) {
			processingTicks = 0;
			isProcessing = false;
		}

		setChanged();
	}

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5
                ) <= 64.0;
    }
	
	public static void serverTick(
			Level level,
			BlockPos pos,
			BlockState state,
			RecyclerBlockEntity blockEntity
	) {
		if (level.isClientSide()) {
			return;
		}

		ItemStack input = blockEntity.getItem(INPUT_SLOT);

		if (input.isEmpty()) {
			if (blockEntity.processingTicks != 0 || blockEntity.isProcessing) {
				blockEntity.processingTicks = 0;
				blockEntity.isProcessing = false;
				blockEntity.setChanged();
			}

			return;
		}

		if (blockEntity.processingTicks >= PROCESSING_TIME) {
			blockEntity.isProcessing = false;
			return;
		}

		blockEntity.isProcessing = true;
		blockEntity.processingTicks++;

		if (blockEntity.processingTicks >= PROCESSING_TIME) {
			blockEntity.isProcessing = false;
		}

		blockEntity.setChanged();
	}

	public int getProcessingTicks() {
		return processingTicks;
	}

	public boolean isProcessing() {
		return isProcessing;
	}

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return INPUT_SLOTS;
        }

        return OUTPUT_SLOT_INDICES;
    }

    @Override
    public boolean canPlaceItemThroughFace(
            int slot,
            ItemStack stack,
            Direction direction
    ) {
        return direction == Direction.UP && slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(
            int slot,
            ItemStack stack,
            Direction direction
    ) {
        return slot >= OUTPUT_START && slot < INVENTORY_SIZE;
    }

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		for (int i = 0; i < items.size(); i++) {
			items.set(i, ItemStack.EMPTY);
		}

		ContainerHelper.loadAllItems(input, items);

		processingTicks = input.getIntOr("ProcessingTicks", 0);
		processingTicks = Math.max(0, Math.min(processingTicks, PROCESSING_TIME));

		isProcessing = false;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		ContainerHelper.saveAllItems(output, items);

		output.putInt("ProcessingTicks", processingTicks);
	}
	
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
	}
}
