package com.payangar.immersivecompanions.network;

import com.payangar.immersivecompanions.client.gui.CompanionRecruitmentScreen;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.combat.CombatStance;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.recruitment.CompanionPricing;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric implementation of networking.
 */
public class FabricNetworking implements ModNetworking {

    /**
     * Registers the payload types and server handlers. Call from ModInitializer.
     */
    public static void registerPayloads() {
        // S2C: Open recruitment screen
        PayloadTypeRegistry.playS2C().register(
                OpenRecruitmentScreenPayload.TYPE,
                OpenRecruitmentScreenPayload.STREAM_CODEC
        );

        // C2S: Close recruitment screen
        PayloadTypeRegistry.playC2S().register(
                CloseRecruitmentScreenPayload.TYPE,
                CloseRecruitmentScreenPayload.STREAM_CODEC
        );

        // C2S: Purchase companion
        PayloadTypeRegistry.playC2S().register(
                PurchaseCompanionPayload.TYPE,
                PurchaseCompanionPayload.STREAM_CODEC
        );

        // Register server handler for close packet
        ServerPlayNetworking.registerGlobalReceiver(
                CloseRecruitmentScreenPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    ServerLevel level = player.serverLevel();

                    context.server().execute(() -> {
                        var entity = level.getEntity(payload.entityId());
                        if (entity instanceof CompanionEntity companion) {
                            // Validate that this player is the one interacting
                            if (player.getUUID().equals(companion.getInteractingPlayer() != null ?
                                    companion.getInteractingPlayer().getUUID() : null)) {
                                companion.clearInteractingPlayer();
                            }
                        }
                    });
                }
        );

        // Register server handler for purchase packet
        ServerPlayNetworking.registerGlobalReceiver(
                PurchaseCompanionPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    ServerLevel level = player.serverLevel();

                    context.server().execute(() -> {
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
                    });
                }
        );

        ModNetworking.init(new FabricNetworking());
    }

    /**
     * Registers client-side payload handlers. Call from ClientModInitializer.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                OpenRecruitmentScreenPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        openScreenOnClient(payload);
                    });
                }
        );
    }

    @Environment(EnvType.CLIENT)
    private static void openScreenOnClient(OpenRecruitmentScreenPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var entity = mc.level.getEntity(payload.entityId());
            if (entity instanceof CompanionEntity companion) {
                mc.setScreen(new CompanionRecruitmentScreen(companion, payload.basePrice(), payload.finalPrice()));
            }
        }
    }

    @Override
    public void sendOpenRecruitmentScreen(ServerPlayer player, int entityId, int basePrice, int finalPrice) {
        ServerPlayNetworking.send(player, new OpenRecruitmentScreenPayload(entityId, basePrice, finalPrice));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void sendCloseRecruitmentScreen(int entityId) {
        ClientPlayNetworking.send(new CloseRecruitmentScreenPayload(entityId));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void sendPurchaseCompanion(int entityId) {
        ClientPlayNetworking.send(new PurchaseCompanionPayload(entityId));
    }
}
