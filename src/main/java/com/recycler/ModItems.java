package com.recycler;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {

	public static final Item GENERIC_WOOD = register(
		"generic_wood",
		new Item.Properties()
			.setId(ResourceKey.create(
				Registries.ITEM,
				RecyclerMod.id("generic_wood")
			))
			.component(DataComponents.COOKING_FUEL, new Fuel(300))
	);

    public static final Item GENERIC_STONE = register("generic_stone");
	
	public static final Item GLASS_SHARDS = register("glass_shards");

	public static final Item CULLET = register("cullet");

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
