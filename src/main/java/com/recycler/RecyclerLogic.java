package com.recycler;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RecyclerLogic {

    private static final Random RANDOM = new Random();

    private RecyclerLogic() {
    }

    public static List<ItemStack> recoverIngredients(
            Level level,
            ItemStack input
    ) {
        if (level == null || level.isClientSide() || input.isEmpty()) {
            return List.of();
        }

        Optional<RecipeHolder<CraftingRecipe>> recipeOptional =
                level.recipeAccess().getRecipeFor(
                        RecipeType.CRAFTING,
                        input,
                        level
                );

        if (recipeOptional.isEmpty()) {
            return List.of();
        }

        CraftingRecipe recipe = recipeOptional.get().value();

        NonNullList<ItemStack> ingredients = recipe.getIngredients();

        List<ItemStack> occupiedIngredients = new ArrayList<>();

        for (var ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                ItemStack[] matchingItems = ingredient.getItems();

                if (matchingItems.length > 0) {
                    occupiedIngredients.add(matchingItems[0].copy());
                }
            }
        }

        if (occupiedIngredients.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(occupiedIngredients, RANDOM);

        int ingredientCount = occupiedIngredients.size();

        List<ItemStack> recoveredItems = new ArrayList<>();

        for (int i = 0; i < ingredientCount; i++) {
            int x = ingredientCount - i;

            double chance = (100.0 * x) / (ingredientCount + 1);

            if (RANDOM.nextDouble() * 100.0 < chance) {
                recoveredItems.add(occupiedIngredients.get(i));
            }
        }

        return recoveredItems;
    }
}
