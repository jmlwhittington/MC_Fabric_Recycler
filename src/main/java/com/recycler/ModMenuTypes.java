package com.recycler;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;

public final class ModMenuTypes {

    public static final MenuType<RecyclerMenu> RECYCLER =
            Registry.register(
                    BuiltInRegistries.MENU,
                    RecyclerMod.id("recycler"),
                    new MenuType<>(
                            RecyclerMenu::new,
                            FeatureFlags.DEFAULT_FLAGS
                    )
            );

    private ModMenuTypes() {
    }

    public static void initialize() {
    }
}
