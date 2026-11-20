
package com.recycler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.mojang.brigadier.Command;
import net.minecraft.resources.Identifier;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public class RecyclerMod implements ModInitializer {

    public static final String MOD_ID = "recycler";

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModMenuTypes.initialize();

		RecoveryRule.initialize();

		ServerLifecycleEvents.SERVER_STARTED.register(
				server -> RecoveryRule.reload()
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
														boolean success =
																RecoveryRule.reload();

														if (success) {
															context.getSource().sendSuccess(
																	() -> Component.literal(
																			"Recycler recovery rules reloaded successfully."
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
					);
				}
		);
	}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
