package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.client.renderer.CompanionRenderer;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * NeoForge client-side event handlers.
 * Registers entity renderers.
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NeoForgeEntityRegistration.COMPANION.get(), CompanionRenderer::new);
    }
}
