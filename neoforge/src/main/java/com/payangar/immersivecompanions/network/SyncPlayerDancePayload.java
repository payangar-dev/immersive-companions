package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet sent when the local player's dance state changes.
 * Used to sync Epic Fight hopak emote detection from client to server,
 * since emotes only play on the client-side animator.
 */
public record SyncPlayerDancePayload(boolean dancing) implements CustomPacketPayload {

    public static final Type<SyncPlayerDancePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "sync_player_dance")
    );

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerDancePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncPlayerDancePayload::dancing,
            SyncPlayerDancePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
