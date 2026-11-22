package com.recycler;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.Command;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class RecyclerMod implements ModInitializer {

    public static final String MOD_ID = "recycler";

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModBlocks.initialize();
		ModMenuTypes.initialize();

		RecyclerConfig.load();
		RecoveryRule.initialize();

		ServerLifecycleEvents.SERVER_STARTED.register(
				server -> RecoveryRule.reload()
		);
		
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
				(server, resourceManager, success) -> {
					if (success) {
						RecyclerLogic.clearNetheriteRecipeCache();
					}
				}
		);

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> {
					dispatcher.register(
							Commands.literal("recycler")
									.requires(source ->
											source.permissions().hasPermission(
													Permissions.COMMANDS_MODERATOR
											)
									)
									.then(
											Commands.literal("reload")
													.executes(context -> {
														RecyclerConfig.load();
														
														boolean success =
																RecoveryRule.reload();

														if (success) {
															context.getSource().sendSuccess(
																	() -> Component.literal(
																			"Recycler configuration and recovery rules reloaded successfully."
																	),
																	false
															);

															return Command.SINGLE_SUCCESS;
														}

														context.getSource().sendFailure(
																Component.literal(
																		"Failed to reload Recycler recovery rules. Check latest.log."
																)
														);

														return 0;
													})
									)
									.then(
											Commands.literal("dump")
													.executes(context ->
															generateItemReport(
																	context.getSource().getServer(),
																	context.getSource()
															)
													)
									)
					);
				}
		);
	}
	
	private static int generateItemReport(
			net.minecraft.server.MinecraftServer server,
			net.minecraft.commands.CommandSourceStack source
	) {
		Level level = server.overworld();

		StringBuilder report = new StringBuilder();

		int totalItems = 0;
		int recyclableItems = 0;
		int unrecyclableItems = 0;
		int protectedItems = 0;
		int specialRuleItems = 0;
		int specialHandlingItems = 0;
		int recipeItems = 0;

		report.append("==================================================\n");
		report.append("RECYCLER ITEM AUDIT\n");
		report.append("==================================================\n\n");

		report.append("Minecraft Version: 26.3\n");
		report.append("Mod: Recycler\n\n");

		for (Item item : BuiltInRegistries.ITEM) {
			totalItems++;

			ItemStack input = new ItemStack(item);

			var itemId = BuiltInRegistries.ITEM.getKey(item);

			String itemName =
					net.minecraft.network.chat.Component
							.translatable(item.getDescriptionId())
							.getString();

			report.append("ITEM: ")
					.append(itemId)
					.append("\n");

			report.append("NAME: ")
					.append(itemName)
					.append("\n");

			if (RecyclerLogic.isProtectedInputForAudit(input)) {
				protectedItems++;

				report.append("STATUS: PROTECTED\n");
				report.append("REASON: Protected input\n");
				report.append("\n--------------------------------------------------\n\n");

				continue;
			}

			boolean hasRule =
					RecoveryRule.hasApplicableRule(input);

			boolean hasSpecialHandling =
					RecyclerLogic.isSpecialIngredientsForAudit(level, input);

			Optional<java.util.List<RecyclerLogic.PotentialOutput>> outputs =
					RecyclerLogic.getPotentialOutputs(level, input);

			if (outputs.isEmpty()) {
				unrecyclableItems++;

				report.append("STATUS: UNRECYCLABLE\n");

				report.append(
						"REASON: No applicable recovery rule or valid crafting recipe\n"
				);

				report.append("\n--------------------------------------------------\n\n");

				continue;
			}

			recyclableItems++;

			String sourceType;

			if (hasRule) {
				sourceType = "SPECIAL ITEM/TAG RULE";
				specialRuleItems++;
			} else if (hasSpecialHandling) {
				sourceType = "SPECIAL HANDLING";
				specialHandlingItems++;
			} else {
				sourceType = "CRAFTING RECIPE";
				recipeItems++;
			}

			report.append("STATUS: RECYCLABLE\n");
			report.append("SOURCE: ")
					.append(sourceType)
					.append("\n");

			report.append("\nPOTENTIAL RECOVERY:\n");

			if (outputs.get().isEmpty()) {
				report.append("  No potential outputs.\n");
			}

			for (RecyclerLogic.PotentialOutput output : outputs.get()) {
				ItemStack stack = output.stack();

				String outputName =
						net.minecraft.network.chat.Component
								.translatable(stack.getItem().getDescriptionId())
								.getString();

				report.append("  ")
						.append(outputName)
						.append(" [")
						.append(BuiltInRegistries.ITEM.getKey(stack.getItem()))
						.append("] x")
						.append(stack.getCount())
						.append("\n");
			}

			report.append("\n--------------------------------------------------\n\n");
		}

		report.append("==================================================\n");
		report.append("AUDIT SUMMARY\n");
		report.append("==================================================\n\n");

		report.append("Total Items Tested: ")
				.append(totalItems)
				.append("\n");

		report.append("Recyclable Items: ")
				.append(recyclableItems)
				.append("\n");

		report.append("Unrecyclable Items: ")
				.append(unrecyclableItems)
				.append("\n");

		report.append("Protected Items: ")
				.append(protectedItems)
				.append("\n\n");

		report.append("SPECIAL ITEM/TAG RULES: ")
				.append(specialRuleItems)
				.append("\n");

		report.append("SPECIAL HANDLING: ")
				.append(specialHandlingItems)
				.append("\n");

		report.append("NORMAL CRAFTING RECIPES: ")
				.append(recipeItems)
				.append("\n\n");

		report.append("==================================================\n");
		report.append("AUDIT COMPLETE\n");
		report.append("==================================================\n");

		Path reportPath = FabricLoader.getInstance()
				.getGameDir()
				.resolve("recycler_item_report.txt");

		final int completedItemCount = totalItems;

		try {
			Files.writeString(reportPath, report.toString());

			source.sendSuccess(
					() -> net.minecraft.network.chat.Component.literal(
							"Recycler audit complete! Tested "
									+ completedItemCount
									+ " items. Report saved to: "
									+ reportPath.toAbsolutePath()
					),
					true
			);

			return com.mojang.brigadier.Command.SINGLE_SUCCESS;

		} catch (IOException exception) {
			source.sendFailure(
					net.minecraft.network.chat.Component.literal(
							"Failed to write Recycler item report: "
									+ exception.getMessage()
					)
			);

			return 0;
		}
	}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
