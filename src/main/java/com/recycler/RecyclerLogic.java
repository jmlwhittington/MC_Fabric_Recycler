
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RecyclerLogic {

    private static final Random RANDOM = new Random();
	
	private static final TagKey<Item> STORAGE_BLOCKS =
        TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath("c", "storage_blocks")
        );

    private RecyclerLogic() {
    }
	
	private static boolean isProtectedInput(ItemStack input) {
		if (input.is(Items.STICK)
				|| input.is(Items.LEATHER)) {
			return true;
		}

		if (input.has(DataComponents.FOOD)
				|| input.is(Items.CAKE)
				|| input.is(Items.FERMENTED_SPIDER_EYE)
				|| input.is(Items.GLISTERING_MELON_SLICE)) {
			return true;
		}
		
		Identifier inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
		String path = inputId.getPath();

		if (path.equals("carpet")
				|| path.endsWith("_carpet")
				|| path.endsWith("_ingot")
				|| path.equals("bamboo_block")
				|| path.equals("stripped_bamboo_block")
				|| path.equals("quartz_block")
				|| path.equals("amethyst_block")
				|| (input.is(STORAGE_BLOCKS))) {
			return true;
		}
		
		if (input.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock) {

			if (input.has(DataComponents.CONTAINER_LOOT)) {
				return true;
			}

			ItemContainerContents contents =
					input.get(DataComponents.CONTAINER);

			if (contents != null
					&& contents.nonEmptyItemCopyStream().findAny().isPresent()) {
				return true;
			}
		}
		
	    if (input.getItem() == Items.BUNDLE
				|| path.endsWith("_bundle")) {

			BundleContents contents =
					input.get(DataComponents.BUNDLE_CONTENTS);

			return contents != null
					&& contents.itemCopies().findAny().isPresent();
		}

		return false;
	}
	
	private static boolean isDyedShulkerBox(ItemStack input) {
		return input.getItem() != Items.SHULKER_BOX
				&& input.getItem() instanceof BlockItem blockItem
				&& blockItem.getBlock() instanceof ShulkerBoxBlock;
	}
	
	private static Optional<List<ItemStack>> getBedIngredients(Item bedItem) {
		Identifier bedId = BuiltInRegistries.ITEM.getKey(bedItem);
		String path = bedId.getPath();

		if (!path.endsWith("_bed")) {
			return Optional.empty();
		}

		String color = path.substring(0, path.length() - "_bed".length());

		Identifier woolId = Identifier.fromNamespaceAndPath(
				bedId.getNamespace(),
				color + "_wool"
		);

		Optional<Item> wool = BuiltInRegistries.ITEM.getOptional(woolId);

		if (wool.isEmpty()) {
			return Optional.empty();
		}

		List<ItemStack> ingredients = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			ingredients.add(new ItemStack(wool.get()));
			ingredients.add(new ItemStack(Items.OAK_PLANKS));
		}

		return Optional.of(ingredients);
	}
	
	private static List<ItemStack> createIngredients(Item item, int count) {
		List<ItemStack> ingredients = new ArrayList<>();

		for (int i = 0; i < count; i++) {
			ingredients.add(new ItemStack(item));
		}

		return ingredients;
	}

	private static Optional<List<ItemStack>> getSpecialIngredients(
			ItemStack input
	) {
		Item item = input.getItem();

		Identifier inputId = BuiltInRegistries.ITEM.getKey(item);
		String path = inputId.getPath();
		
		if (item == Items.TARGET) {
			return Optional.of(
					createIngredients(Items.HAY_BLOCK, 1)
			);
		}
		
		if (path.equals("copper_bulb")
				|| path.endsWith("_copper_bulb")) {

			List<ItemStack> ingredients = new ArrayList<>();

			ingredients.addAll(
					createIngredients(
							BuiltInRegistries.ITEM.getValue(
									Identifier.withDefaultNamespace("copper_block")
							),
							3
					)
			);
			ingredients.add(new ItemStack(Items.BLAZE_ROD));
			ingredients.add(new ItemStack(Items.REDSTONE));

			return Optional.of(ingredients);
		}

		if (item == Items.LEAD) {
			return Optional.of(
					createIngredients(Items.STRING, 2)
			);
		}

		if (path.endsWith("_wool")) {
			return Optional.of(
					createIngredients(Items.STRING, 4)
			);
		}

		if (path.endsWith("_harness")) {
			String color = path.substring(
					0,
					path.length() - "_harness".length()
			);

			Identifier woolId = Identifier.fromNamespaceAndPath(
					inputId.getNamespace(),
					color + "_wool"
			);

			Optional<Item> wool =
					BuiltInRegistries.ITEM.getOptional(woolId);

			if (wool.isEmpty()) {
				return Optional.empty();
			}

			List<ItemStack> ingredients = new ArrayList<>();

			ingredients.addAll(createIngredients(Items.LEATHER, 3));
			ingredients.addAll(createIngredients(Items.GLASS, 2));
			ingredients.add(new ItemStack(wool.get()));

			return Optional.of(ingredients);
		}

		if (path.endsWith("_cushion")) {
			String color = path.substring(
					0,
					path.length() - "_cushion".length()
			);

			Identifier slabId = Identifier.fromNamespaceAndPath(
					inputId.getNamespace(),
					color + "_wool_slab"
			);

			Optional<Item> slab =
					BuiltInRegistries.ITEM.getOptional(slabId);

			if (slab.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(
					createIngredients(slab.get(), 3)
			);
		}

		if (path.endsWith("_shelf")) {
			String wood = path.substring(
					0,
					path.length() - "_shelf".length()
			);

			Identifier plankId = Identifier.fromNamespaceAndPath(
					inputId.getNamespace(),
					wood + "_planks"
			);

			Optional<Item> plank =
					BuiltInRegistries.ITEM.getOptional(plankId);

			if (plank.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(
					createIngredients(plank.get(), 1)
			);
		}

		if (item == Items.IRON_DOOR) {
			return Optional.of(
					createIngredients(Items.IRON_INGOT, 2)
			);
		}

		return getBedIngredients(item);
	}
	
	private static Item getCopperBaseItem(Item item) {
		Identifier inputId = BuiltInRegistries.ITEM.getKey(item);
		String path = inputId.getPath();

		if (path.startsWith("waxed_")) {
			path = path.substring("waxed_".length());
		}

		boolean oxidizedCopperBlock =
				path.equals("copper")
				|| path.equals("exposed_copper")
				|| path.equals("weathered_copper")
				|| path.equals("oxidized_copper");

		if (oxidizedCopperBlock) {
			return BuiltInRegistries.ITEM.getValue(
					Identifier.withDefaultNamespace("copper_block")
			);
		}

		if (path.startsWith("exposed_")) {
			path = path.substring("exposed_".length());
		} else if (path.startsWith("weathered_")) {
			path = path.substring("weathered_".length());
		} else if (path.startsWith("oxidized_")) {
			path = path.substring("oxidized_".length());
		}

		if (!path.contains("copper")) {
			return item;
		}

		Identifier baseId = Identifier.fromNamespaceAndPath(
				inputId.getNamespace(),
				path
		);

		return BuiltInRegistries.ITEM.getOptional(baseId).orElse(item);
	}

	private static Item getRecipeLookupItem(ItemStack input) {
		Item item = input.getItem();

		Identifier inputId = BuiltInRegistries.ITEM.getKey(item);
		String path = inputId.getPath();

		if (isDyedShulkerBox(input)) {
			return Items.SHULKER_BOX;
		}

		if (path.endsWith("_bundle") && item != Items.BUNDLE) {
			Identifier bundleId = Identifier.fromNamespaceAndPath(
					inputId.getNamespace(),
					"bundle"
			);

			return BuiltInRegistries.ITEM.getOptional(bundleId)
					.orElse(item);
		}

		return getCopperBaseItem(item);
	}
	
	private static List<ItemStack> rollRecovery(List<ItemStack> ingredients) {
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

		return recoveredItems;
	}
	
	private static Optional<Item> getSupportedMultiOutputIngredient(
			Item inputItem,
			int outputCount,
			List<Ingredient> ingredients
	) {
		Item lookupItem = getCopperBaseItem(inputItem);

		Identifier inputId = BuiltInRegistries.ITEM.getKey(lookupItem);
		String path = inputId.getPath();

		Item expectedIngredient;
		int expectedOutputCount;
		int expectedIngredientCount;

		if (path.equals("copper_door")) {
			expectedIngredient = Items.COPPER_INGOT;
			expectedOutputCount = 3;
			expectedIngredientCount = 6;

		} else if (path.equals("copper_trapdoor")) {
			expectedIngredient = Items.COPPER_INGOT;
			expectedOutputCount = 2;
			expectedIngredientCount = 6;

		} else if (path.equals("copper_grate")
				|| path.equals("cut_copper")) {
			expectedIngredient = BuiltInRegistries.ITEM.getValue(
					Identifier.withDefaultNamespace("copper_block")
			);
			expectedOutputCount = 4;
			expectedIngredientCount = 4;

		} else {
			if (path.endsWith("_door")) {
				expectedOutputCount = 3;
			} else if (path.endsWith("_trapdoor")) {
				expectedOutputCount = 2;
			} else {
				return Optional.empty();
			}

			String woodName = path.substring(
					0,
					path.lastIndexOf('_')
			);

			Identifier plankId = Identifier.fromNamespaceAndPath(
					inputId.getNamespace(),
					woodName + "_planks"
			);

			Optional<Item> plank =
					BuiltInRegistries.ITEM.getOptional(plankId);

			if (plank.isEmpty()) {
				return Optional.empty();
			}

			expectedIngredient = plank.get();
			expectedIngredientCount = 6;
		}

		if (outputCount != expectedOutputCount
				|| ingredients.size() != expectedIngredientCount
				|| ingredients.size() % outputCount != 0) {
			return Optional.empty();
		}

		ItemStack representative = new ItemStack(expectedIngredient);

		for (Ingredient ingredient : ingredients) {
			if (!ingredient.test(representative)) {
				return Optional.empty();
			}
		}

		return Optional.of(expectedIngredient);
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
		
		if (getSpecialIngredients(input).isPresent()) {
			return true;
		}

		Item recipeInput = getRecipeLookupItem(input);
		
		if (isDyedShulkerBox(input)
				|| getBedIngredients(input.getItem()).isPresent()) {
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
						|| result.getItem() != recipeInput) {
					continue;
				}

                NormalCraftingRecipe craftingRecipe =
                        (NormalCraftingRecipe) recipe;

                List<Ingredient> ingredients =
                        craftingRecipe.placementInfo().ingredients();
				
				if (result.getCount() != 1
						&& getSupportedMultiOutputIngredient(
								input.getItem(),
								result.getCount(),
								ingredients
						).isEmpty()) {
					continue;
				}
				
				if (ingredients.size() <= 1) {
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
				|| input == null
                || input.isEmpty()) {
            return Optional.empty();
        }
		
		if (isProtectedInput(input)) {
			return Optional.empty();
		}

        Optional<List<ItemStack>> specialResult =
                RecoveryRule.getRecoveryResult(input);

		if (specialResult.isPresent()) {
			return specialResult;
		}
		
		Optional<List<ItemStack>> specialIngredients =
				getSpecialIngredients(input);

		if (specialIngredients.isPresent()) {
			return Optional.of(
					rollRecovery(specialIngredients.get())
			);
		}

		Item recipeInput = getRecipeLookupItem(input);

		Optional<List<ItemStack>> bedIngredients =
				getBedIngredients(input.getItem());

		if (bedIngredients.isPresent()) {
			return Optional.of(rollRecovery(bedIngredients.get()));
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
						|| result.getItem() != recipeInput) {
					continue;
				}

                List<Ingredient> recipeIngredients =
                        craftingRecipe.placementInfo().ingredients();
				
				if (recipeIngredients.size() <= 1) {
					continue;
				}

				if (result.getCount() != 1
						&& getSupportedMultiOutputIngredient(
								input.getItem(),
								result.getCount(),
								recipeIngredients
						).isEmpty()) {

					continue;
				}

				List<ItemStack> ingredients;

				if (result.getCount() == 1) {

					ingredients = new ArrayList<>(recipeIngredients.size());

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

					} else {

						Optional<Item> recoveredIngredient =
								getSupportedMultiOutputIngredient(
										input.getItem(),
										result.getCount(),
										recipeIngredients
								);

						if (recoveredIngredient.isEmpty()) {
							continue;
						}

						int ingredientCost =
								recipeIngredients.size() / result.getCount();

						ingredients = createIngredients(
								recoveredIngredient.get(),
								ingredientCost
						);
					}

				return Optional.of(rollRecovery(ingredients));

            } catch (RuntimeException exception) {
            }
        }

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
