package com.recycler;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RecyclerLogic {

    private static final Random RANDOM = new Random();

    private RecyclerLogic() {
    }

    public static Optional<List<ItemStack>> recoverIngredients(
            Level level,
            ItemStack input
    ) {
        if (level == null || level.isClientSide() || input.isEmpty()) {
            return Optional.empty();
        }

        Optional<List<ItemStack>> specialResult =
                RecoveryRule.getRecoveryResult(input);

        if (specialResult.isPresent()) {
            return specialResult;
        }

        List<RecipeHolder<?>> recipes = new ArrayList<>(
                level.getServer().getRecipeManager().getRecipes()
        );

        recipes.sort(
                Comparator.comparing(
                        holder -> holder.id().identifier().toString()
                )
        );

        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> recipe = holder.value();

            if (!(recipe instanceof NormalCraftingRecipe craftingRecipe)
                    || recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }

            ItemStack result = craftingRecipe.assemble(CraftingInput.EMPTY);

            if (result.isEmpty() || result.getItem() != input.getItem()) {
                continue;
            }

            List<ItemStack> ingredients = new ArrayList<>();

            for (Ingredient ingredient : craftingRecipe.placementInfo().ingredients()) {
                Optional<Holder<Item>> representative =
                        ingredient.items().findFirst();

                if (representative.isPresent()) {
                    ingredients.add(
                            new ItemStack(representative.get().value())
                    );
                }
            }

            if (ingredients.isEmpty()) {
                return Optional.of(List.of());
            }

            Collections.shuffle(ingredients, RANDOM);

            int ingredientCount = ingredients.size();
            List<ItemStack> recoveredItems = new ArrayList<>();

            for (int i = 0; i < ingredientCount; i++) {
                int x = ingredientCount - i;
                double chance =
                        (100.0 * x) / (ingredientCount + 1);

                if (RANDOM.nextDouble() * 100.0 < chance) {
                    recoveredItems.add(ingredients.get(i).copy());
                }
            }

            return Optional.of(recoveredItems);
        }

        return Optional.empty();
    }
}
