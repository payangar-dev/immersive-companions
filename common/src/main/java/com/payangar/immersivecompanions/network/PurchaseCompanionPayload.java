package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet sent when the player clicks the recruit button.
 * Contains only the entity ID - price is recalculated server-side for security.
 */
public record PurchaseCompanionPayload(int entityId) implements CustomPacketPayload {

    public static final Type<PurchaseCompanionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "purchase_companion")
    );

    public static final StreamCodec<FriendlyByteBuf, PurchaseCompanionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PurchaseCompanionPayload::entityId,
            PurchaseCompanionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
