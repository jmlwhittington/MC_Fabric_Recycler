package com.recycler;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final Item GENERIC_WOOD = register("generic_wood");

    public static final Item GENERIC_STONE = register("generic_stone");

    private ModItems() {
    }

    private static Item register(String name) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                RecyclerMod.id(name)
        );

        Item item = new Item(
                new Item.Properties()
                        .setId(itemKey)
        );

        return Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                item
        );
    }

    public static void initialize() {
    }
}
