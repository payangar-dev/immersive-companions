package com.payangar.immersivecompanions.compat.dynamiclights;

import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandler;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;

/**
 * Compatibility class for LambDynamicLights and SodiumDynamicLights on Fabric.
 * Registers the companion entity type with the dynamic lights system so that
 * companions holding light-emitting items (torches, lanterns) will emit light.
 */
public class DynamicLightsCompatFabric implements DynamicLightsInitializer {
    @Override
    public void onInitializeDynamicLights() {
        DynamicLightHandlers.registerDynamicLightHandler(
            FabricEntityRegistration.getCompanionEntityType(),
            DynamicLightHandler.makeLivingEntityHandler(companion -> 0)
        );
    }
}
