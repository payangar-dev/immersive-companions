package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-server packet sent when the recruitment screen is closed.
 * Contains the entity ID of the companion whose interaction ended.
 */
public record CloseRecruitmentScreenPayload(int entityId) implements CustomPacketPayload {

    public static final Type<CloseRecruitmentScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "close_recruitment_screen")
    );

    public static final StreamCodec<FriendlyByteBuf, CloseRecruitmentScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CloseRecruitmentScreenPayload::entityId,
            CloseRecruitmentScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
