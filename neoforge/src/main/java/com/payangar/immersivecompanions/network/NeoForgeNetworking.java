package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.client.gui.CompanionRecruitmentScreen;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge implementation of networking.
 */
public class NeoForgeNetworking implements ModNetworking {

    /**
     * Registers networking with the mod event bus.
     * Call this from the mod constructor.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworking::registerPayloads);
        ModNetworking.init(new NeoForgeNetworking());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(ImmersiveCompanions.MOD_ID);

        // S2C: Open recruitment screen
        registrar.playToClient(
                OpenRecruitmentScreenPayload.TYPE,
                OpenRecruitmentScreenPayload.STREAM_CODEC,
                NeoForgeNetworking::handleOpenRecruitmentScreen
        );

        // C2S: Close recruitment screen
        registrar.playToServer(
                CloseRecruitmentScreenPayload.TYPE,
                CloseRecruitmentScreenPayload.STREAM_CODEC,
                NeoForgeNetworking::handleCloseRecruitmentScreen
        );
    }

    private static void handleOpenRecruitmentScreen(OpenRecruitmentScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLLoader.getDist().isClient()) {
                openScreenOnClient(payload);
            }
        });
    }

    private static void openScreenOnClient(OpenRecruitmentScreenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var entity = mc.level.getEntity(payload.entityId());
            if (entity instanceof CompanionEntity companion) {
                mc.setScreen(new CompanionRecruitmentScreen(companion, payload.basePrice(), payload.finalPrice()));
            }
        }
    }

    private static void handleCloseRecruitmentScreen(CloseRecruitmentScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                var entity = level.getEntity(payload.entityId());
                if (entity instanceof CompanionEntity companion) {
                    // Validate that this player is the one interacting
                    if (player.getUUID().equals(companion.getInteractingPlayer() != null ?
                            companion.getInteractingPlayer().getUUID() : null)) {
                        companion.clearInteractingPlayer();
                    }
                }
            }
        });
    }

    @Override
    public void sendOpenRecruitmentScreen(ServerPlayer player, int entityId, int basePrice, int finalPrice) {
        PacketDistributor.sendToPlayer(player, new OpenRecruitmentScreenPayload(entityId, basePrice, finalPrice));
    }

    @Override
    public void sendCloseRecruitmentScreen(int entityId) {
        PacketDistributor.sendToServer(new CloseRecruitmentScreenPayload(entityId));
    }
}
