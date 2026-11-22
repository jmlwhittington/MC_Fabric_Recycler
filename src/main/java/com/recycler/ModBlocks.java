package com.recycler;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModBlocks {
    public static final Block RECYCLER = registerBlock(
            "recycler",
            BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
    );

	public static final ResourceKey<CreativeModeTab> RECYCLER_TAB_KEY =
			ResourceKey.create(
					BuiltInRegistries.CREATIVE_MODE_TAB.key(),
					RecyclerMod.id("recycler")
			);

	public static final CreativeModeTab RECYCLER_TAB =
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.recycler.recycler"))
					.icon(() -> new ItemStack(RECYCLER))
					.displayItems((parameters, output) -> {
						output.accept(RECYCLER);
					    output.accept(ModItems.GENERIC_WOOD);
						output.accept(ModItems.GENERIC_STONE);
						output.accept(ModItems.GLASS_SHARDS);
						output.accept(ModItems.CULLET);
					})
					.build();

    public static final BlockEntityType<RecyclerBlockEntity> RECYCLER_BLOCK_ENTITY =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(RecyclerMod.MOD_ID, "recycler"),
                    FabricBlockEntityTypeBuilder.create(
                            RecyclerBlockEntity::new,
                            RECYCLER
                    ).build()
            );

    private ModBlocks() {
    }

    private static Block registerBlock(String name, BlockBehaviour.Properties properties) {
        ResourceKey<Block> blockKey = ResourceKey.create(
                Registries.BLOCK,
                Identifier.fromNamespaceAndPath(RecyclerMod.MOD_ID, name)
        );
        properties.setId(blockKey);

        Block block = new RecyclerBlock(properties);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(RecyclerMod.MOD_ID, name)
        );
        Item item = new BlockItem(
                block,
                new Item.Properties()
                        .useBlockDescriptionPrefix()
                        .setId(itemKey)
        );
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return block;
    }

	public static void initialize() {
		Registry.register(
				BuiltInRegistries.CREATIVE_MODE_TAB,
				RECYCLER_TAB_KEY,
				RECYCLER_TAB
		);
	}
}