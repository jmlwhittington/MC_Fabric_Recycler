package com.recycler;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.component.ResolvableFloat;
import net.minecraft.world.item.component.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;

public final class ModItems {

    public static final Item GENERIC_WOOD = register(
        "generic_wood",
        new Item.Properties()
            .component(
                DataComponents.COOKING_FUEL,
                new CookingFuel(
                    new ResolvableInt.Constant(300),
                    ResolvableFloat.fromKey(
                        ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER
                    )
                )
            )
    );

    public static final Item GENERIC_STONE = register("generic_stone");
    public static final Item GLASS_SHARDS = register("glass_shards");
    public static final Item CULLET = register("cullet");

    private ModItems() {
    }

    private static Item register(String name) {
        return register(name, new Item.Properties());
    }

    private static Item register(String name, Item.Properties properties) {
        ResourceKey<Item> itemKey = ResourceKey.create(
            Registries.ITEM,
            RecyclerMod.id(name)
        );

        Item item = new Item(properties.setId(itemKey));

        return Registry.register(
            BuiltInRegistries.ITEM,
            itemKey,
            item
        );
    }

    public static void initialize() {
    }
}
