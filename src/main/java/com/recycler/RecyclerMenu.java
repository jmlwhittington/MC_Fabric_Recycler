package com.recycler;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.DataSlot;

public class RecyclerMenu extends AbstractContainerMenu {

    private static final int MACHINE_START = 0;
    private static final int MACHINE_END = RecyclerBlockEntity.INVENTORY_SIZE;

    private static final int PLAYER_START = MACHINE_END;
    private static final int PLAYER_MAIN_END = PLAYER_START + 27;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;
	
	private static final int STATUS_IDLE = 0;
	private static final int STATUS_PROCESSING = 1;
	private static final int STATUS_WAITING = 2;
	private static final int STATUS_REJECTED = 3;

	private final RecyclerBlockEntity blockEntity;
	private final int[] machineData = new int[2];

    public RecyclerMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(RecyclerBlockEntity.INVENTORY_SIZE)
        );
    }

    public RecyclerMenu(
            int containerId,
            Inventory playerInventory,
            Container container
    ) {
        super(ModMenuTypes.RECYCLER, containerId);

        checkContainerSize(
                container,
                RecyclerBlockEntity.INVENTORY_SIZE
        );

        this.container = container;
		
		this.blockEntity = container instanceof RecyclerBlockEntity recycler
				? recycler
				: null;

		addDataSlot(DataSlot.shared(machineData, 0));
		addDataSlot(DataSlot.shared(machineData, 1));

        container.startOpen(playerInventory.player);

		addSlot(new Slot(
				container,
				RecyclerBlockEntity.INPUT_SLOT,
				24,
				35
		));

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = RecyclerBlockEntity.OUTPUT_START
                        + row * 3 + column;

				addSlot(new Slot(
						container,
						slot,
						116 + column * 18,
						30 + row * 18
				));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 84);
    }

	@Override
	public void broadcastChanges() {
		if (blockEntity != null) {
			machineData[0] = blockEntity.getProcessingTicks();

			if (blockEntity.isInputRejected()) {
				machineData[1] = STATUS_REJECTED;
			} else if (blockEntity.isWaitingForOutputSpace()) {
				machineData[1] = STATUS_WAITING;
			} else if (blockEntity.isProcessing()) {
				machineData[1] = STATUS_PROCESSING;
			} else {
				machineData[1] = STATUS_IDLE;
			}
		}

		super.broadcastChanges();
	}

	public float getProgress() {
		return Math.min(1.0F,
				Math.max(0.0F, machineData[0] / (float) RecyclerBlockEntity.PROCESSING_TIME));
	}

	public int getMachineStatus() {
		return machineData[1];
	}

	public String getStatusText() {
		return switch (machineData[1]) {
			case STATUS_PROCESSING -> "Working";
			case STATUS_WAITING -> "Output full";
			case STATUS_REJECTED -> "Rejected";
			default -> "Idle";
		};
	}

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (slotIndex < MACHINE_END) {

            if (!moveItemStackTo(
                    stack,
                    PLAYER_START,
                    PLAYER_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }

        } else {

            if (!moveItemStackTo(
                    stack,
                    RecyclerBlockEntity.INPUT_SLOT,
                    RecyclerBlockEntity.INPUT_SLOT + 1,
                    false
            )) {

                if (slotIndex < PLAYER_MAIN_END) {
                    if (!moveItemStackTo(
                            stack,
                            PLAYER_MAIN_END,
                            PLAYER_END,
                            false
                    )) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!moveItemStackTo(
                            stack,
                            PLAYER_START,
                            PLAYER_MAIN_END,
                            false
                    )) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
