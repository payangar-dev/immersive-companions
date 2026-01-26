package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.client.gui.CompanionEquipmentScreen;
import com.payangar.immersivecompanions.client.renderer.CompanionRenderer;
import com.payangar.immersivecompanions.data.CompanionSkins;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import com.payangar.immersivecompanions.registry.NeoForgeMenuRegistration;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * NeoForge client-side event handlers.
 * Registers entity renderers and resource reload listeners.
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoForgeClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NeoForgeEntityRegistration.COMPANION.get(), CompanionRenderer::new);
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(NeoForgeMenuRegistration.COMPANION_EQUIPMENT.get(), CompanionEquipmentScreen::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Unit>() {
            @Override
            protected Unit prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return Unit.INSTANCE;
            }

            @Override
            protected void apply(Unit object, ResourceManager resourceManager, ProfilerFiller profiler) {
                CompanionSkins.discoverSkins(resourceManager);
            }
        });
    }
}
