package com.recycler;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
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
        if (level == null
                || level.isClientSide()
                || level.getServer() == null
                || input.isEmpty()) {
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

            if (recipe.getClass() != ShapedRecipe.class
                    && recipe.getClass() != ShapelessRecipe.class) {
                continue;
            }

            if (recipe.getType() != RecipeType.CRAFTING) {
                continue;
            }

            NormalCraftingRecipe craftingRecipe =
                    (NormalCraftingRecipe) recipe;

            try {
                ItemStack result = getStaticResult(recipe);

                if (result.isEmpty()
                        || result.getItem() != input.getItem()) {
                    continue;
                }

                List<Ingredient> recipeIngredients =
                        craftingRecipe.placementInfo().ingredients();

                if (recipeIngredients.isEmpty()) {
                    continue;
                }

                List<ItemStack> ingredients =
                        new ArrayList<>(recipeIngredients.size());

                boolean validIngredients = true;

                for (Ingredient ingredient : recipeIngredients) {

                    Optional<Holder<Item>> representative =
                            ingredient.items()
                                    .sorted(
                                            Comparator.comparing(holder ->
                                                    BuiltInRegistries.ITEM
                                                            .getKey(holder.value())
                                                            .toString()
                                            )
                                    )
                                    .filter(holder ->
                                            ingredient.test(
                                                    new ItemStack(holder.value())
                                            )
                                    )
                                    .findFirst();

                    if (representative.isEmpty()) {
                        validIngredients = false;
                        break;
                    }

                    ingredients.add(
                            new ItemStack(representative.get().value())
                    );
                }

                if (!validIngredients) {
                    continue;
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

            } catch (RuntimeException exception) {
                RecyclerMod.LOGGER.warn(
                        "Recycler skipped invalid crafting recipe {}",
                        holder.id(),
                        exception
                );
            }
        }

        return Optional.empty();
    }

    private static ItemStack getStaticResult(Recipe<?> recipe) {

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            var template = shapelessRecipe.result();

            if (template == null) {
                return ItemStack.EMPTY;
            }

            return template.create();
        }

        if (recipe instanceof ShapedRecipe shapedRecipe) {
            List<RecipeDisplay> displays = shapedRecipe.display();

            if (displays.isEmpty()) {
                return ItemStack.EMPTY;
            }

            SlotDisplay resultDisplay =
                    displays.get(0).result();

            if (resultDisplay
                    instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {

                return stackDisplay.stack().create();
            }
        }

        return ItemStack.EMPTY;
    }
}
