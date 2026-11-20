package com.recycler;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RecyclerMenu extends AbstractContainerMenu {

    private static final int MACHINE_START = 0;
    private static final int MACHINE_END = RecyclerBlockEntity.INVENTORY_SIZE;

    private static final int PLAYER_START = MACHINE_END;
    private static final int PLAYER_MAIN_END = PLAYER_START + 27;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final Container container;

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

        container.startOpen(playerInventory.player);

        addSlot(new Slot(
                container,
                RecyclerBlockEntity.INPUT_SLOT,
                26,
                35
        ));

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = RecyclerBlockEntity.OUTPUT_START
                        + row * 3 + column;

                addSlot(new Slot(
                        container,
                        slot,
                        112 + column * 18,
                        26 + row * 18
                ));
            }
        }

        addStandardInventorySlots(playerInventory, 8, 84);
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
