package com.payangar.immersivecompanions.network;

import net.minecraft.server.level.ServerPlayer;

/**
 * Platform-agnostic networking abstraction.
 * Implementations handle platform-specific packet registration and sending.
 */
public interface ModNetworking {

    /**
     * Sends an open recruitment screen packet to a player.
     *
     * @param player     The player to send the packet to
     * @param entityId   The entity ID of the companion
     * @param basePrice  The base price before reputation modifier
     * @param finalPrice The final price after reputation modifier
     */
    void sendOpenRecruitmentScreen(ServerPlayer player, int entityId, int basePrice, int finalPrice);

    /**
     * Sends a close recruitment screen packet from client to server.
     * Called when the recruitment screen is closed on the client.
     *
     * @param entityId The entity ID of the companion whose interaction ended
     */
    void sendCloseRecruitmentScreen(int entityId);

    /**
     * Sends a purchase companion packet from client to server.
     * Called when the player clicks the recruit button.
     *
     * @param entityId The entity ID of the companion to purchase
     */
    void sendPurchaseCompanion(int entityId);

    /**
     * Gets the networking instance.
     */
    static ModNetworking get() {
        return Holder.INSTANCE;
    }

    /**
     * Initializes the networking instance. Called by platform-specific code.
     */
    static void init(ModNetworking networking) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("ModNetworking already initialized!");
        }
        Holder.INSTANCE = networking;
    }

    class Holder {
        private static ModNetworking INSTANCE;
    }
}
