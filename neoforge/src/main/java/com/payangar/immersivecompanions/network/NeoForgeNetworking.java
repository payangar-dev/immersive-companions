package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.client.gui.CompanionRecruitmentScreen;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.combat.CombatStance;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.platform.NeoForgeServices;
import com.payangar.immersivecompanions.recruitment.CompanionPricing;
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

        // C2S: Purchase companion
        registrar.playToServer(
                PurchaseCompanionPayload.TYPE,
                PurchaseCompanionPayload.STREAM_CODEC,
                NeoForgeNetworking::handlePurchaseCompanion
        );

        // C2S: Sync player dance state (Epic Fight emote detection)
        registrar.playToServer(
                SyncPlayerDancePayload.TYPE,
                SyncPlayerDancePayload.STREAM_CODEC,
                NeoForgeNetworking::handleSyncPlayerDance
        );

        // C2S: Revive companion tick (held right-click on agonizing companion)
        registrar.playToServer(
                ReviveCompanionTickPayload.TYPE,
                ReviveCompanionTickPayload.STREAM_CODEC,
                NeoForgeNetworking::handleReviveCompanionTick
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

    private static void handlePurchaseCompanion(PurchaseCompanionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                var entity = level.getEntity(payload.entityId());
                if (entity instanceof CompanionEntity companion) {
                    // Validate that this player is the one interacting
                    var interacting = companion.getInteractingPlayer();
                    if (interacting == null || !player.getUUID().equals(interacting.getUUID())) {
                        return; // Not the interacting player
                    }

                    // Recalculate price server-side for security
                    int basePrice = companion.getBasePrice();
                    int finalPrice = CompanionPricing.calculateFinalPrice(basePrice, companion, player);

                    // Deduct emeralds
                    if (!CompanionPricing.removeEmeralds(player, finalPrice)) {
                        return; // Can't afford
                    }

                    // Transfer ownership
                    companion.setOwnerUUID(player.getUUID());
                    companion.setCompanionTeam("player_" + player.getUUID().toString());
                    companion.setMode(CompanionMode.FOLLOW);
                    companion.setCombatStance(CombatStance.ASSIST);
                    companion.clearInteractingPlayer();

                    // Send recruitment message to the player
                    companion.sendRecruitmentMessage(player);
                }
            }
        });
    }

    private static void handleSyncPlayerDance(SyncPlayerDancePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NeoForgeServices.setPlayerDancing(player.getUUID(), payload.dancing());
            }
        });
    }

    @Override
    public void sendOpenRecruitmentScreen(ServerPlayer player, int entityId, int basePrice, int finalPrice) {
        PacketDistributor.sendToPlayer(player, new OpenRecruitmentScreenPayload(entityId, basePrice, finalPrice));
    }

    private static void handleReviveCompanionTick(ReviveCompanionTickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ServerLevel level = player.serverLevel();
                var entity = level.getEntity(payload.entityId());
                if (entity instanceof CompanionEntity companion
                        && companion.isAgonizing()
                        && player.distanceToSqr(companion) <= 9.0) {
                    companion.tickReviveProgress(player);
                }
            }
        });
    }

    @Override
    public void sendReviveCompanionTick(int entityId) {
        PacketDistributor.sendToServer(new ReviveCompanionTickPayload(entityId));
    }

    @Override
    public void sendCloseRecruitmentScreen(int entityId) {
        PacketDistributor.sendToServer(new CloseRecruitmentScreenPayload(entityId));
    }

    @Override
    public void sendPurchaseCompanion(int entityId) {
        PacketDistributor.sendToServer(new PurchaseCompanionPayload(entityId));
    }
}
