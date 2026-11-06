
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.resources.Identifier;

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
	
	private static boolean isProtectedInput(ItemStack input) {
		if (input.getItem() instanceof DyeItem) {
			return true;
		}

		if (!(input.getItem() instanceof BlockItem blockItem)
				|| !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
			return false;
		}

		if (input.has(DataComponents.CONTAINER_LOOT)) {
			return true;
		}

		ItemContainerContents contents = input.get(DataComponents.CONTAINER);

		return contents != null
				&& contents.nonEmptyItemCopyStream().findAny().isPresent();
	}
	
	private static Optional<Item> getSupportedMultiOutputPlank(
        Item inputItem,
        int outputCount,
        List<Ingredient> ingredients
	) {
		Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
		String path = inputId.getPath();

		int expectedOutputCount;

		if (path.endsWith("_door")) {
			expectedOutputCount = 3;
		} else if (path.endsWith("_trapdoor")) {
			expectedOutputCount = 2;
		} else {
			return Optional.empty();
		}

		if (outputCount != expectedOutputCount
				|| ingredients.isEmpty()
				|| ingredients.size() % outputCount != 0) {
			return Optional.empty();
		}

		String woodName = path.substring(0, path.lastIndexOf('_'));

		Identifier plankId = Identifier.fromNamespaceAndPath(
				inputId.getNamespace(),
				woodName + "_planks"
		);

		Optional<Item> plank = BuiltInRegistries.ITEM.getOptional(plankId);

		if (plank.isEmpty()) {
			return Optional.empty();
		}

		ItemStack representative = new ItemStack(plank.get());

		// Every occupied recipe slot must accept this exact plank item.
		for (Ingredient ingredient : ingredients) {
			if (!ingredient.test(representative)) {
				return Optional.empty();
			}
		}

		return plank;
	}

    public static boolean canRecycleInput(Level level, ItemStack input) {
        if (level == null
                || level.isClientSide()
                || level.getServer() == null
                || input == null
                || input.isEmpty()) {
            return false;
        }
		
		if (isProtectedInput(input)) {
			return false;
		}

        if (RecoveryRule.hasApplicableRule(input)) {
            return true;
        }

        List<RecipeHolder<?>> recipes = new ArrayList<>(
                level.getServer().getRecipeManager().getRecipes()
        );

        recipes.sort(
                Comparator.comparing(
                        recipeHolder ->
                                recipeHolder.id().identifier().toString()
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

            try {
                ItemStack result = getStaticResult(recipe);

                if (result.isEmpty()
                        || result.getItem() != input.getItem()
                    continue;
                }

                NormalCraftingRecipe craftingRecipe =
                        (NormalCraftingRecipe) recipe;

                List<Ingredient> ingredients =
                        craftingRecipe.placementInfo().ingredients();
				
				if (result.getCount() != 1
						&& getSupportedMultiOutputPlank(
								input.getItem(),
								result.getCount(),
								ingredients
						).isEmpty()) {
					continue;
				}
				
				if (ingredients.size() <= 1) {
					continue;
				}

                if (ingredients.isEmpty()) {
                    continue;
                }

                boolean validIngredients = true;

                for (Ingredient ingredient : ingredients) {
                    Optional<Holder<Item>> representative =
                            ingredient.items()
                                    .sorted(
                                            Comparator.comparing(itemHolder ->
                                                    BuiltInRegistries.ITEM
                                                            .getKey(itemHolder.value())
                                                            .toString()
                                            )
                                    )
                                    .filter(itemHolder ->
                                            ingredient.test(
                                                    new ItemStack(itemHolder.value())
                                            )
                                    )
                                    .findFirst();

                    if (representative.isEmpty()) {
                        validIngredients = false;
                        break;
                    }
                }

                if (validIngredients) {
                    return true;
                }

            } catch (RuntimeException exception) {
                RecyclerMod.LOGGER.warn(
                        "Recycler skipped invalid crafting recipe {} during eligibility check",
                        holder.id(),
                        exception
                );
            }
        }

        return false;
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
		
		if (isProtectedInput(input)) {
			return Optional.empty();
		}

        Optional<List<ItemStack>> specialResult =
                RecoveryRule.getRecoveryResult(input);

		if (specialResult.isPresent()) {
			if (specialResult.get().isEmpty()) {
				RecyclerMod.LOGGER.info(
						"Recycler matched a special recovery rule for {}, "
								+ "but recovered no items.",
						BuiltInRegistries.ITEM.getKey(input.getItem())
				);
			} else {
				RecyclerMod.LOGGER.info(
						"Recycler matched a special recovery rule for {}.",
						BuiltInRegistries.ITEM.getKey(input.getItem())
				);
			}

			return specialResult;
		}

        List<RecipeHolder<?>> recipes = new ArrayList<>(
                level.getServer().getRecipeManager().getRecipes()
        );

        recipes.sort(
                Comparator.comparing(
                        recipeHolder ->
                                recipeHolder.id().identifier().toString()
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

                RecyclerMod.LOGGER.info(
                        "Recycler found recipe {} for {} with output count {}.",
                        holder.id(),
                        BuiltInRegistries.ITEM.getKey(input.getItem()),
                        result.getCount()
                );

                if (result.getCount() != 1) {
                    RecyclerMod.LOGGER.info(
                            "Recycler skipped recipe {} because its output count is not 1.",
                            holder.id()
                    );

                    continue;
                }

                List<Ingredient> recipeIngredients =
                        craftingRecipe.placementInfo().ingredients();
				
				if (recipeIngredients.size() <= 1) {
					continue;
				}

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
                                            Comparator.comparing(itemHolder ->
                                                    BuiltInRegistries.ITEM
                                                            .getKey(itemHolder.value())
                                                            .toString()
                                            )
                                    )
                                    .filter(itemHolder ->
                                            ingredient.test(
                                                    new ItemStack(itemHolder.value())
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

				if (recoveredItems.isEmpty()) {
					RecyclerMod.LOGGER.info(
							"Recycler recovered no ingredients from recipe {}.",
							holder.id()
					);

					return Optional.of(recoveredItems);
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

        RecyclerMod.LOGGER.info(
                "Recycler found no eligible single-output crafting recipe for {}.",
                BuiltInRegistries.ITEM.getKey(input.getItem())
        );

        return Optional.empty();
    }

    private static ItemStack getStaticResult(Recipe<?> recipe) {

        if (!(recipe instanceof ShapedRecipe)
                && !(recipe instanceof ShapelessRecipe)) {
            return ItemStack.EMPTY;
        }

        List<RecipeDisplay> displays = recipe.display();

        if (displays.isEmpty()) {
            return ItemStack.EMPTY;
        }

        SlotDisplay resultDisplay = displays.get(0).result();

        if (resultDisplay
                instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            return stackDisplay.stack().create();
        }

        return ItemStack.EMPTY;
    }
}
