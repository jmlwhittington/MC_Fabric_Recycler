
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
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class RecyclerLogic {

	public record PotentialOutput(
			ItemStack stack,
			double chance
	) {}

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
				|| input.is(Items.GLISTERING_MELON_SLICE)
				|| input.is(Items.MELON)) {
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
				|| input.is(STORAGE_BLOCKS)
				|| path.equals("copper_block")
				|| path.equals("exposed_copper")
				|| path.equals("weathered_copper")
				|| path.equals("oxidized_copper")
				|| path.equals("waxed_copper_block")
				|| path.equals("waxed_exposed_copper")
				|| path.equals("waxed_weathered_copper")
				|| path.equals("waxed_oxidized_copper")
				|| path.equals("granite")
				|| path.equals("clay")
				|| path.equals("honey_block")
				|| path.endsWith("_wool_stairs")
				|| path.endsWith("_wool_slab")) {
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
	
	private static Optional<Item> getCrackedBaseItem(ItemStack input) {
		Identifier inputId =
				BuiltInRegistries.ITEM.getKey(input.getItem());

		String path = inputId.getPath();

		if (!path.startsWith("cracked_")) {
			return Optional.empty();
		}

		String replacementPath =
				path.substring("cracked_".length());

		Identifier replacementId =
				Identifier.fromNamespaceAndPath(
						inputId.getNamespace(),
						replacementPath
				);

		return BuiltInRegistries.ITEM.getOptional(replacementId);
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

		if (item == Items.NAME_TAG) {
			return Optional.of(
					createIngredients(Items.PAPER, 1)
			);
		}

		if (item == Items.TNT) {
			return Optional.of(
					createIngredients(Items.GUNPOWDER, 5)
			);
		}

		if (path.endsWith("_map")
				&& !path.equals("filled_map")
				&& !path.equals("map")) {
			return Optional.of(
					createIngredients(Items.PAPER, 8)
			);
		}

		if (item == Items.TARGET) {
			return Optional.of(
					createIngredients(Items.HAY_BLOCK, 1)
			);
		}
		
		if (item == Items.QUARTZ_PILLAR) {
			return Optional.of(
					createIngredients(Items.QUARTZ_BLOCK, 1)
			);
		}
		
		if (item == Items.PACKED_MUD) {
			return Optional.of(
					createIngredients(Items.MUD, 1)
			);
		}

		if (path.equals("dark_prismarine")) {
			return Optional.of(
					createIngredients(Items.PRISMARINE_SHARD, 8)
			);
		}

		if (path.equals("mossy_stone_bricks")) {
			List<ItemStack> ingredients = new ArrayList<>();

			ingredients.add(new ItemStack(Items.STONE_BRICKS));
			ingredients.add(new ItemStack(Items.VINE));

			return Optional.of(ingredients);
		}

		if (path.equals("mossy_cobblestone")) {
			List<ItemStack> ingredients = new ArrayList<>();

			ingredients.add(new ItemStack(Items.COBBLESTONE));
			ingredients.add(new ItemStack(Items.VINE));

			return Optional.of(ingredients);
		}

		if (path.equals("red_nether_bricks")) {
			return Optional.of(
					createIngredients(Items.NETHER_BRICK, 2)
			);
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

		if (path.endsWith("_candle") && !path.equals("candle")) {
			path = "candle";
		}

		if (path.equals("chipped_anvil")
				|| path.equals("damaged_anvil")) {
			path = "anvil";
		}

		if (path.equals("sticky_piston")) {
			path = "piston";
		}

		String lightningPath = path;

		if (lightningPath.startsWith("waxed_")) {
			lightningPath = lightningPath.substring("waxed_".length());
		}

		if (lightningPath.startsWith("exposed_")) {
			lightningPath = lightningPath.substring("exposed_".length());
		} else if (lightningPath.startsWith("weathered_")) {
			lightningPath = lightningPath.substring("weathered_".length());
		} else if (lightningPath.startsWith("oxidized_")) {
			lightningPath = lightningPath.substring("oxidized_".length());
		}

		if (lightningPath.equals("lightning_rod")) {
			path = "lightning_rod";
		}

		if (path.equals("filled_map")) {
			return Items.MAP;
		}

		if (path.equals("written_book")
				|| path.equals("enchanted_book")) {
			path = "book";
		}

		if (path.equals("glow_item_frame")) {
			path = "item_frame";
		}

		Identifier lookupId = Identifier.fromNamespaceAndPath(
				inputId.getNamespace(),
				path
		);

		Item lookupItem = BuiltInRegistries.ITEM
				.getOptional(lookupId)
				.orElse(item);

		return getCopperBaseItem(lookupItem);
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
	

	private static Optional<List<ItemStack>> getMultiOutputIngredients(
			Item inputItem,
			int outputCount,
			List<Ingredient> ingredients
	) {
		if (outputCount <= 1
				|| ingredients.isEmpty()
				|| ingredients.size() % outputCount != 0) {
			return Optional.empty();
		}

		Item lookupItem = getCopperBaseItem(inputItem);

		String path =
				BuiltInRegistries.ITEM.getKey(lookupItem).getPath();

		if (path.equals("copper_grate")
				|| path.equals("cut_copper")) {

			if (outputCount != 4 || ingredients.size() != 4) {
				return Optional.empty();
			}
		}

		if (path.equals("copper_trapdoor")) {
			if (outputCount != 2 || ingredients.size() != 6) {
				return Optional.empty();
			}
		}

		Optional<Holder<Item>> representative =
				ingredients.get(0).items()
						.sorted(
								Comparator.comparing(itemHolder ->
										BuiltInRegistries.ITEM
												.getKey(itemHolder.value())
												.toString()
								)
						)
						.filter(itemHolder -> {
							ItemStack stack =
									new ItemStack(itemHolder.value());

							for (Ingredient ingredient : ingredients) {
								if (!ingredient.test(stack)) {
									return false;
								}
							}

							return true;
						})
						.findFirst();

		if (representative.isEmpty()) {
			return Optional.empty();
		}

		int ingredientCost =
				ingredients.size() / outputCount;

		if (path.equals("copper_trapdoor")) {
			ingredientCost = 4;
		}

		return Optional.of(
				createIngredients(
						representative.get().value(),
						ingredientCost
				)
		);
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
		
		Optional<Item> crackedBase = getCrackedBaseItem(input);

		if (crackedBase.isPresent()) {
			return canRecycleInput(
					level,
					new ItemStack(crackedBase.get())
			);
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
						&& getMultiOutputIngredients(
								input.getItem(),
								result.getCount(),
								ingredients
						).isEmpty()) {
					continue;
				}

				if (result.getCount() == 1
						&& ingredients.size() <= 1) {
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
		
		Optional<Item> crackedBase = getCrackedBaseItem(input);

		if (crackedBase.isPresent()) {
			return recoverIngredients(
					level,
					new ItemStack(crackedBase.get())
			);
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
						&& getMultiOutputIngredients(
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

						Optional<List<ItemStack>> multiOutputIngredients =
								getMultiOutputIngredients(
										input.getItem(),
										result.getCount(),
										recipeIngredients
								);

						if (multiOutputIngredients.isEmpty()) {
							continue;
						}

						ingredients = multiOutputIngredients.get();
					}

				return Optional.of(rollRecovery(ingredients));

            } catch (RuntimeException exception) {
            }
        }

        return Optional.empty();
    }

public static boolean isProtectedInputForAudit(ItemStack input) {
    return isProtectedInput(input);
}

public static boolean isSpecialIngredientsForAudit(ItemStack input) {
    return getSpecialIngredients(input).isPresent();
}

public static Optional<List<PotentialOutput>> getPotentialOutputs(
        Level level,
        ItemStack input
) {
    if (level == null
            || level.isClientSide()
            || level.getServer() == null
            || input == null
            || input.isEmpty()
            || isProtectedInput(input)) {
        return Optional.empty();
    }

    Optional<List<RecoveryRule.PotentialRuleOutput>> ruleResults =
            RecoveryRule.getPotentialRecoveryResult(input);

    if (ruleResults.isPresent()) {
        List<PotentialOutput> outputs = new ArrayList<>();

        for (RecoveryRule.PotentialRuleOutput result : ruleResults.get()) {
            outputs.add(
                    new PotentialOutput(
                            result.stack().copy(),
                            result.chance()
                    )
            );
        }

        return Optional.of(outputs);
    }

    Optional<List<ItemStack>> specialIngredients =
            getSpecialIngredients(input);

    if (specialIngredients.isPresent()) {
        return Optional.of(
                calculatePotentialOutputs(specialIngredients.get())
        );
    }

    Item recipeInput = getRecipeLookupItem(input);

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

            List<Ingredient> recipeIngredients =
                    craftingRecipe.placementInfo().ingredients();

            if (recipeIngredients.size() <= 1) {
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
                Optional<List<ItemStack>> multiOutputIngredients =
                        getMultiOutputIngredients(
                                input.getItem(),
                                result.getCount(),
                                recipeIngredients
                        );

                if (multiOutputIngredients.isEmpty()) {
                    continue;
                }

                ingredients = multiOutputIngredients.get();
            }

            return Optional.of(
                    calculatePotentialOutputs(ingredients)
            );

        } catch (RuntimeException exception) {
        }
    }

    return Optional.empty();
}

	private static List<PotentialOutput> calculatePotentialOutputs(
			List<ItemStack> ingredients
	) {
		List<PotentialOutput> outputs = new ArrayList<>();

		if (ingredients.isEmpty()) {
			return outputs;
		}

		for (ItemStack ingredient : ingredients) {
			outputs.add(
					new PotentialOutput(
							ingredient.copy(),
							50.0
					)
			);
		}

		return outputs;
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
