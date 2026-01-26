package com.payangar.immersivecompanions.compat.dynamiclights;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;

/**
 * Compatibility class for SodiumDynamicLights on NeoForge.
 * Unlike Fabric, NeoForge doesn't have entrypoint discovery for dynamic lights.
 * Must be called directly during client initialization.
 */
public class DynamicLightsCompatNeoForge {

    public static void init() {
        DynamicLightHandlers.registerDynamicLightHandler(
            NeoForgeEntityRegistration.COMPANION.get(),
            DynamicLightHandler.makeLivingEntityHandler(companion -> 0)
        );
        ImmersiveCompanions.LOGGER.info("Registered dynamic light handler for companions (NeoForge)");
    }
}
