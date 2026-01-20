package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client packet for opening the recruitment screen.
 * Contains the entity ID of the companion and the calculated price.
 */
public record OpenRecruitmentScreenPayload(int entityId, int basePrice, int finalPrice) implements CustomPacketPayload {

    public static final Type<OpenRecruitmentScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "open_recruitment_screen")
    );

    public static final StreamCodec<FriendlyByteBuf, OpenRecruitmentScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenRecruitmentScreenPayload::entityId,
            ByteBufCodecs.VAR_INT, OpenRecruitmentScreenPayload::basePrice,
            ByteBufCodecs.VAR_INT, OpenRecruitmentScreenPayload::finalPrice,
            OpenRecruitmentScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
