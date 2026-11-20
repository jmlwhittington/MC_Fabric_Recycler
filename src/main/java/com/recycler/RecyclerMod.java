
package com.recycler;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class RecyclerMod implements ModInitializer {

    public static final String MOD_ID = "recycler";

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
		ModMenuTypes.initialize();

        RecoveryRule.initialize();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
