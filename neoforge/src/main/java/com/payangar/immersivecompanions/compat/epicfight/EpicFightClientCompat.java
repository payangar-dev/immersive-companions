package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;

/**
 * Client-side Epic Fight compatibility.
 * Registers the patched renderer for companion entities.
 */
public class EpicFightClientCompat {

    /**
     * Initializes client-side Epic Fight compatibility.
     * Call this from EpicFightCompat.init() when Epic Fight is detected (client-side only).
     */
    public static void init() {
        // Register patched renderer via Epic Fight's client event system
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            event.addPatchedEntityRenderer(
                NeoForgeEntityRegistration.COMPANION.get(),
                entityType -> new PCompanionRenderer(event.getContext(), entityType)
            );
            ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patched renderer with Epic Fight");
        });
    }
}
