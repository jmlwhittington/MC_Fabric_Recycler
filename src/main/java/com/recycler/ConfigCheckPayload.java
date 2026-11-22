package com.recycler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigCheckPayload(
        int processingTimeTicks,
        String recoveryRulesFingerprint
) implements CustomPacketPayload {

    public static final Type<ConfigCheckPayload> TYPE =
            new Type<>(RecyclerMod.id("config_check"));

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ConfigCheckPayload
    > CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ConfigCheckPayload::processingTimeTicks,
            ByteBufCodecs.STRING_UTF8,
            ConfigCheckPayload::recoveryRulesFingerprint,
            ConfigCheckPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
