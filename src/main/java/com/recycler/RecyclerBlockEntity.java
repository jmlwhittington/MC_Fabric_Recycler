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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.Level;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecyclerBlockEntity extends BlockEntity
        implements Container, WorldlyContainer, MenuProvider {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_SLOTS = 6;
    public static final int INVENTORY_SIZE = 1 + OUTPUT_SLOTS;

    public static final int PROCESSING_TIME = 100;

    private int processingTicks = 0;
    private boolean isProcessing = false;

    private List<ItemStack> pendingRecovery = null;

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
		ItemStack previous = items.get(slot).copy();

		ItemStack result = ContainerHelper.removeItem(items, slot, amount);

		if (!result.isEmpty()) {
			if (slot == INPUT_SLOT
					&& !sameInputItem(previous, items.get(slot))) {
				processingTicks = 0;
				isProcessing = false;
				pendingRecovery = null;
			}

			setChanged();
		}

		return result;
	}

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);

        if (!result.isEmpty() && slot == INPUT_SLOT) {
            processingTicks = 0;
            isProcessing = false;
            pendingRecovery = null;
        }

        return result;
    }

	private boolean sameInputItem(ItemStack first, ItemStack second) {
		if (first.isEmpty() || second.isEmpty()) {
			return first.isEmpty() && second.isEmpty();
		}

		return ItemStack.isSameItemSameComponents(first, second);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		ItemStack previous = items.get(slot);

		items.set(slot, stack);

		if (!stack.isEmpty()) {
			stack.limitSize(Math.min(getMaxStackSize(), stack.getMaxStackSize()));
		}

		if (slot == INPUT_SLOT && !sameInputItem(previous, stack)) {
			processingTicks = 0;
			isProcessing = false;
			pendingRecovery = null;
		}

		setChanged();
	}

    @Override
    public void clearContent() {
        items.clear();
        processingTicks = 0;
        isProcessing = false;
        pendingRecovery = null;
        setChanged();
    }


	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot != INPUT_SLOT || stack.isEmpty() || level == null) {
			return false;
		}

		if (level.isClientSide()) {
			return true;
		}

		return RecyclerLogic.canRecycleInput(level, stack);
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
			if (blockEntity.processingTicks != 0
					|| blockEntity.isProcessing
					|| blockEntity.pendingRecovery != null) {

				blockEntity.processingTicks = 0;
				blockEntity.isProcessing = false;
				blockEntity.pendingRecovery = null;
				blockEntity.setChanged();
			}

			return;
		}

		if (blockEntity.pendingRecovery == null) {
			Optional<List<ItemStack>> result =
					RecyclerLogic.recoverIngredients(level, input);

			if (result.isEmpty()) {
				blockEntity.processingTicks = 0;
				blockEntity.isProcessing = false;
				blockEntity.setChanged();
				return;
			}

			blockEntity.pendingRecovery = new ArrayList<>();

			for (ItemStack stack : result.get()) {
				if (!stack.isEmpty()) {
					blockEntity.pendingRecovery.add(stack.copy());
				}
			}

			blockEntity.setChanged();
		}

		if (blockEntity.processingTicks < PROCESSING_TIME) {
			blockEntity.isProcessing = true;
			blockEntity.processingTicks++;
			blockEntity.setChanged();

			if (blockEntity.processingTicks < PROCESSING_TIME) {
				return;
			}
		}

		blockEntity.isProcessing = false;

		if (blockEntity.pendingRecovery.isEmpty()) {
			blockEntity.consumeInput(input);
			return;
		}

		if (!blockEntity.canFitAll(blockEntity.pendingRecovery)) {
			return;
		}

		blockEntity.insertResults(blockEntity.pendingRecovery);
		blockEntity.consumeInput(input);
	}

	private void consumeInput(ItemStack input) {
		input.shrink(1);

		if (input.isEmpty()) {
			items.set(INPUT_SLOT, ItemStack.EMPTY);
		}

		pendingRecovery = null;
		processingTicks = 0;
		isProcessing = false;

		setChanged();
	}

    private boolean canFitAll(List<ItemStack> results) {
        List<ItemStack> simulated = new ArrayList<>(OUTPUT_SLOTS);

        for (int i = OUTPUT_START; i < INVENTORY_SIZE; i++) {
            simulated.add(items.get(i).copy());
        }

        for (ItemStack result : results) {
            ItemStack remaining = result.copy();

            for (ItemStack existing : simulated) {
                if (remaining.isEmpty()) {
                    break;
                }

                if (!existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, remaining)) {

                    int space = existing.getMaxStackSize() - existing.getCount();
                    int moved = Math.min(space, remaining.getCount());

                    if (moved > 0) {
                        existing.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }

            for (int i = 0; i < simulated.size() && !remaining.isEmpty(); i++) {
                if (simulated.get(i).isEmpty()) {
                    int moved = Math.min(
                            remaining.getCount(),
                            remaining.getMaxStackSize()
                    );

                    simulated.set(i, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private void insertResults(List<ItemStack> results) {
        for (ItemStack result : results) {
            ItemStack remaining = result.copy();

            for (int slot = OUTPUT_START; slot < INVENTORY_SIZE; slot++) {
                ItemStack existing = items.get(slot);

                if (remaining.isEmpty()) {
                    break;
                }

                if (!existing.isEmpty()
                        && ItemStack.isSameItemSameComponents(existing, remaining)) {

                    int space = existing.getMaxStackSize() - existing.getCount();
                    int moved = Math.min(space, remaining.getCount());

                    if (moved > 0) {
                        existing.grow(moved);
                        remaining.shrink(moved);
                    }
                }
            }

            for (int slot = OUTPUT_START;
                    slot < INVENTORY_SIZE && !remaining.isEmpty();
                    slot++) {

                if (items.get(slot).isEmpty()) {
                    int moved = Math.min(
                            remaining.getCount(),
                            remaining.getMaxStackSize()
                    );

                    items.set(slot, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                }
            }
        }
    }

    public int getProcessingTicks() {
        return processingTicks;
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
		return direction == Direction.UP && canPlaceItem(slot, stack);
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

        pendingRecovery = new ArrayList<>();

        for (ItemStack stack : input.listOrEmpty("PendingRecovery", ItemStack.CODEC)) {
            if (!stack.isEmpty()) {
                pendingRecovery.add(stack.copy());
            }
        }

        if (pendingRecovery.isEmpty()) {
            pendingRecovery = null;
        }

        isProcessing = false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        ContainerHelper.saveAllItems(output, items);

        output.putInt("ProcessingTicks", processingTicks);

        if (pendingRecovery != null && !pendingRecovery.isEmpty()) {
            ValueOutput.TypedOutputList<ItemStack> pending =
                    output.list("PendingRecovery", ItemStack.CODEC);

            for (ItemStack stack : pendingRecovery) {
                pending.add(stack);
            }
        } else {
            output.discard("PendingRecovery");
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);

        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, this);
        }
    }
	
	@Override
	public Component getDisplayName() {
		return Component.translatable("block.recycler.recycler");
	}

	@Override
	public AbstractContainerMenu createMenu(
			int containerId,
			Inventory playerInventory,
			Player player
	) {
		return new RecyclerMenu(containerId, playerInventory, this);
	}
}
