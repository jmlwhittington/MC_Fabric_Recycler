package com.recycler;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RecyclerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(
                ModMenuTypes.RECYCLER,
                RecyclerScreen::new
        );
    }
}
