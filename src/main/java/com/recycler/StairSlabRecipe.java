package com.recycler;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StairSlabRecipe extends CustomRecipe {

    private static final StairSlabRecipe INSTANCE =
            new StairSlabRecipe();

    public static final MapCodec<StairSlabRecipe> CODEC =
            MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, StairSlabRecipe>
            STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static final RecipeSerializer<StairSlabRecipe> SERIALIZER =
            new RecipeSerializer<>(CODEC, STREAM_CODEC);

    private static final TagKey<Block> COMMON_STAIRS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("c", "stairs")
            );

    private static final TagKey<Block> COMMON_SLABS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("c", "slabs")
            );

    private static final TagKey<Block> FORGE_STAIRS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("forge", "stairs")
            );

    private static final TagKey<Block> FORGE_SLABS =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("forge", "slabs")
            );

    private static final Map<Item, Optional<Resolution>> CACHE =
            new ConcurrentHashMap<>();

    private record Resolution(
            Item source,
            Optional<Item> slabRemainder
    ) {}

    public static void register() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                RecyclerMod.id("stair_slab"),
                SERIALIZER
        );
    }

    public static void clearCache() {
        CACHE.clear();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
		if (!RecyclerConfig.isStairSlabCraftingEnabled()) {
			return false;
		}
		
        Item inputItem = getTwoMatchingItems(input);

        if (inputItem == null || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        return getResolution(serverLevel, inputItem).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
		if (!RecyclerConfig.isStairSlabCraftingEnabled()) {
			return ItemStack.EMPTY;
		}
		
        Item inputItem = getTwoMatchingItems(input);

        if (inputItem == null) {
            return ItemStack.EMPTY;
        }

        Optional<Resolution> resolution =
                CACHE.getOrDefault(inputItem, Optional.empty());

        return resolution
                .map(value -> new ItemStack(value.source()))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining =
                NonNullList.withSize(input.size(), ItemStack.EMPTY);

        Item inputItem = getTwoMatchingItems(input);

        if (inputItem == null) {
            return remaining;
        }

        Optional<Resolution> resolution =
                CACHE.getOrDefault(inputItem, Optional.empty());

        if (resolution.isEmpty()
                || resolution.get().slabRemainder().isEmpty()) {
            return remaining;
        }

        Item slab = resolution.get().slabRemainder().get();

        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                remaining.set(i, new ItemStack(slab));
                break;
            }
        }

        return remaining;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }

    private static Item getTwoMatchingItems(CraftingInput input) {
        Item foundItem = null;
        int occupiedSlots = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            occupiedSlots++;

            if (occupiedSlots > 2) {
                return null;
            }

            if (foundItem == null) {
                foundItem = stack.getItem();
            } else if (foundItem != stack.getItem()) {
                return null;
            }
        }

        return occupiedSlots == 2 ? foundItem : null;
    }

    private static Optional<Resolution> getResolution(
            ServerLevel level,
            Item inputItem
    ) {
        return CACHE.computeIfAbsent(
                inputItem,
                item -> findResolution(
                        level.getServer().getRecipeManager(),
                        item
                )
        );
    }

    private static Optional<Resolution> findResolution(
            RecipeManager manager,
            Item inputItem
    ) {
        boolean stair = isStair(inputItem);
        boolean slab = isSlab(inputItem);

        if (stair == slab) {
            return Optional.empty();
        }

        Optional<Item> source =
                findUniqueSource(manager, inputItem);

        if (source.isEmpty()) {
            return Optional.empty();
        }

        if (slab) {
            return Optional.of(
                    new Resolution(source.get(), Optional.empty())
            );
        }

        Optional<Item> matchingSlab =
                findUniqueSlab(manager, source.get());

        if (matchingSlab.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new Resolution(source.get(), matchingSlab)
        );
    }

    private static Optional<Item> findUniqueSource(
            RecipeManager manager,
            Item outputItem
    ) {
        Item source = null;
        boolean foundRecipe = false;

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();

            if (!isSupportedRecipe(recipe)) {
                continue;
            }

            ItemStack result = getStaticResult(recipe);

            if (result.isEmpty() || result.getItem() != outputItem) {
                continue;
            }

            foundRecipe = true;

            NormalCraftingRecipe craftingRecipe =
                    (NormalCraftingRecipe) recipe;

            Optional<Item> recipeSource =
                    getSingleMaterialSource(craftingRecipe);

            if (recipeSource.isEmpty()) {
                return Optional.empty();
            }

            if (source != null && source != recipeSource.get()) {
                return Optional.empty();
            }

            source = recipeSource.get();
        }

        return foundRecipe
                ? Optional.ofNullable(source)
                : Optional.empty();
    }

    private static Optional<Item> findUniqueSlab(
            RecipeManager manager,
            Item source
    ) {
        Set<Item> slabItems = new HashSet<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();

            if (!isSupportedRecipe(recipe)) {
                continue;
            }

            ItemStack result = getStaticResult(recipe);

            if (!result.isEmpty() && isSlab(result.getItem())) {
                slabItems.add(result.getItem());
            }
        }

        Item matchingSlab = null;

        for (Item slabItem : slabItems) {
            Optional<Item> slabSource =
                    findUniqueSource(manager, slabItem);

            if (slabSource.isEmpty()) {
                if (hasAmbiguousRecipeUsingSource(
                        manager,
                        slabItem,
                        source
                )) {
                    return Optional.empty();
                }

                continue;
            }

            if (slabSource.get() != source) {
                continue;
            }

            if (matchingSlab != null && matchingSlab != slabItem) {
                return Optional.empty();
            }

            matchingSlab = slabItem;
        }

        return Optional.ofNullable(matchingSlab);
    }

    private static boolean hasAmbiguousRecipeUsingSource(
            RecipeManager manager,
            Item outputItem,
            Item source
    ) {
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();

            if (!isSupportedRecipe(recipe)) {
                continue;
            }

            ItemStack result = getStaticResult(recipe);

            if (result.isEmpty() || result.getItem() != outputItem) {
                continue;
            }

            NormalCraftingRecipe craftingRecipe =
                    (NormalCraftingRecipe) recipe;

            if (getSingleMaterialSource(craftingRecipe).isPresent()) {
                continue;
            }

            List<Ingredient> ingredients =
                    craftingRecipe.placementInfo().ingredients();

            if (ingredients.isEmpty()) {
                continue;
            }

            boolean sourcePossible = true;

            for (Ingredient ingredient : ingredients) {
                boolean containsSource = ingredient.items()
                        .anyMatch(holderItem -> holderItem.value() == source);

                if (!containsSource) {
                    sourcePossible = false;
                    break;
                }
            }

            if (sourcePossible) {
                return true;
            }
        }

        return false;
    }

    private static Optional<Item> getSingleMaterialSource(
            NormalCraftingRecipe recipe
    ) {
        List<Ingredient> ingredients =
                recipe.placementInfo().ingredients();

        if (ingredients.isEmpty()) {
            return Optional.empty();
        }

        Item source = null;

        for (Ingredient ingredient : ingredients) {
            List<Holder<Item>> alternatives =
                    ingredient.items().toList();

            if (alternatives.size() != 1) {
                return Optional.empty();
            }

            Item candidate = alternatives.getFirst().value();

            if (!(candidate instanceof BlockItem)) {
                return Optional.empty();
            }

            if (source != null && source != candidate) {
                return Optional.empty();
            }

            source = candidate;
        }

        return Optional.ofNullable(source);
    }

    private static boolean isSupportedRecipe(Recipe<?> recipe) {
        return recipe.getType() == RecipeType.CRAFTING
                && (recipe instanceof ShapedRecipe
                || recipe instanceof ShapelessRecipe)
                && recipe instanceof NormalCraftingRecipe;
    }

    private static ItemStack getStaticResult(Recipe<?> recipe) {
        List<RecipeDisplay> displays = recipe.display();

        if (displays.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SlotDisplay resultDisplay = displays.getFirst().result();

        if (resultDisplay instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            return stackDisplay.stack().create();
        }

        return ItemStack.EMPTY;
    }

    private static boolean isStair(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();

        return block instanceof StairBlock
                || block.builtInRegistryHolder().is(COMMON_STAIRS)
                || block.builtInRegistryHolder().is(FORGE_STAIRS);
    }

    private static boolean isSlab(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();

        return block instanceof SlabBlock
                || block.builtInRegistryHolder().is(COMMON_SLABS)
                || block.builtInRegistryHolder().is(FORGE_SLABS);
    }
}
