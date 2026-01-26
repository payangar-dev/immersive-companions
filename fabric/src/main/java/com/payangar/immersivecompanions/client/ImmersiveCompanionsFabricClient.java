package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.client.gui.CompanionEquipmentScreen;
import com.payangar.immersivecompanions.client.renderer.CompanionRenderer;
import com.payangar.immersivecompanions.data.CompanionSkins;
import com.payangar.immersivecompanions.network.FabricNetworking;
import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import com.payangar.immersivecompanions.registry.FabricMenuRegistration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Fabric client-side initialization.
 * Registers entity renderers and resource reload listeners.
 */
@Environment(EnvType.CLIENT)
public class ImmersiveCompanionsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity renderers
        EntityRendererRegistry.register(FabricEntityRegistration.getCompanionEntityType(), CompanionRenderer::new);

        // Register menu screens
        MenuScreens.register(FabricMenuRegistration.getCompanionEquipmentMenuType(), CompanionEquipmentScreen::new);

        // Register networking client handlers
        FabricNetworking.registerClientHandlers();

        // Register resource reload listener for skin discovery
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "skin_discovery");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        CompanionSkins.discoverSkins(resourceManager);
                    }
                }
        );
    }
}
