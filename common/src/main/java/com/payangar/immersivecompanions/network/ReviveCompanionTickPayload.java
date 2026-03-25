package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet sent each tick while a player holds right-click
 * on an agonizing companion to revive them.
 */
public record ReviveCompanionTickPayload(int entityId) implements CustomPacketPayload {

    public static final Type<ReviveCompanionTickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "revive_companion_tick")
    );

    public static final StreamCodec<FriendlyByteBuf, ReviveCompanionTickPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReviveCompanionTickPayload::entityId,
            ReviveCompanionTickPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
