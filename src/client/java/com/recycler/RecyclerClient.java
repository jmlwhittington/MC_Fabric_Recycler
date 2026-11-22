package com.recycler;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.MenuScreens;

public class RecyclerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(
                ModMenuTypes.RECYCLER,
                RecyclerScreen::new
        );
		
		ClientPlayNetworking.registerGlobalReceiver(
				ConfigCheckPayload.TYPE,
				(payload, context) -> {
					context.client().execute(() -> {
						var client = context.client();

						if (client.player == null) {
							return;
						}

						int localProcessingTime =
								RecyclerConfig.getProcessingTimeTicks();

						String localRulesFingerprint =
								RecoveryRule.getConfigurationFingerprint();

						boolean processingMismatch =
								localProcessingTime != payload.processingTimeTicks();

						boolean rulesMismatch =
								!localRulesFingerprint.equals(
										payload.recoveryRulesFingerprint()
								);
						
						boolean localStairSlabSetting =
								RecyclerConfig.isStairSlabCraftingEnabled();

						boolean stairSlabMismatch =
								localStairSlabSetting != payload.stairSlabCraftingEnabled();

						if (!processingMismatch && !rulesMismatch && !stairSlabMismatch) {
							return;
						}

						client.player.sendSystemMessage(
								Component.literal(
										"[Recycler] Configuration differs from this server."
								)
						);

						if (processingMismatch) {
							client.player.sendSystemMessage(
									Component.literal(
											"Processing time: server = "
													+ payload.processingTimeTicks()
													+ " ticks; local = "
													+ localProcessingTime
													+ " ticks."
									)
							);
						}

						if (rulesMismatch) {
							client.player.sendSystemMessage(
									Component.literal(
											"Recovery rules: server and local rules differ."
									)
							);
						}
						
						if (stairSlabMismatch) {
							client.player.sendSystemMessage(
									Component.literal(
											"Stair/slab crafting: server = "
													+ payload.stairSlabCraftingEnabled()
													+ "; local = "
													+ localStairSlabSetting
									)
							);
						}

						client.player.sendSystemMessage(
								Component.literal(
										"The server controls Recycler behavior. "
												+ "Your local configuration was not changed."
								),
						);
					});
				}
		);
    }
}
