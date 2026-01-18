package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.client.renderer.CompanionRenderer;
import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Fabric client-side initialization.
 * Registers entity renderers.
 */
@Environment(EnvType.CLIENT)
public class ImmersiveCompanionsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity renderers
        EntityRendererRegistry.register(FabricEntityRegistration.getCompanionEntityType(), CompanionRenderer::new);
    }
}
