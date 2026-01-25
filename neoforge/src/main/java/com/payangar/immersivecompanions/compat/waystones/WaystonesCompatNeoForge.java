package com.payangar.immersivecompanions.compat.waystones;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.compat.TeleportCompatManager;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.CompanionTeleportHandler;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.event.WaystoneTeleportEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Waystones compatibility handler for NeoForge.
 * Hooks into Waystones' teleportation events to teleport companions along with the player.
 *
 * This class is only loaded when Waystones is present, using isolated class loading
 * to prevent NoClassDefFoundError when Waystones is absent.
 */
public class WaystonesCompatNeoForge implements TeleportCompatManager.TeleportModHandler {

    public static final WaystonesCompatNeoForge INSTANCE = new WaystonesCompatNeoForge();

    private WaystonesCompatNeoForge() {
        // Singleton - use INSTANCE
    }

    @Override
    public String getModId() {
        return "waystones";
    }

    @Override
    public String getDisplayName() {
        return "Waystones (NeoForge)";
    }

    /**
     * Initializes the Waystones compatibility handler.
     * Call this from mod initialization when Waystones is detected.
     */
    public static void init() {
        ImmersiveCompanions.LOGGER.info("Waystones detected - registering companion teleport integration");

        // Register with TeleportCompatManager
        TeleportCompatManager.registerHandler(INSTANCE);

        // Register for Waystones teleport events via Balm
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystonesCompatNeoForge::onWaystoneTeleportPre);

        ImmersiveCompanions.LOGGER.debug("Waystones teleport event listener registered");
    }

    /**
     * Handler for WaystoneTeleportEvent.Pre.
     * Adds all companions in FOLLOW mode to the teleport context so Waystones
     * teleports them along with the player.
     */
    private static void onWaystoneTeleportPre(WaystoneTeleportEvent.Pre event) {
        if (!(event.getContext().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Get all companions that should teleport with this player
        List<CompanionEntity> companions = CompanionTeleportHandler.getCompanionsForTeleport(player);

        if (companions.isEmpty()) {
            return;
        }

        ImmersiveCompanions.LOGGER.debug("Adding {} companion(s) to Waystones teleport for player {}",
                companions.size(), player.getName().getString());

        long timestamp = System.currentTimeMillis();

        for (CompanionEntity companion : companions) {
            // Add companion to Waystones teleport context
            event.addAdditionalEntity(companion);

            // Mark as externally handled to prevent fallback system from double-teleporting
            CompanionTeleportHandler.markExternallyHandled(companion.getUUID(), timestamp);
        }
    }
}
