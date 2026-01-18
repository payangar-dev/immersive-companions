package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.neoevent.PatchedRenderersEvent;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;

/**
 * Client-side Epic Fight compatibility.
 * Registers the patched renderer for companion entities.
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EpicFightClientCompat {

    /**
     * Registers the patched renderer for companion entities.
     * This event is fired on the MOD bus during client-side mod loading.
     */
    @SubscribeEvent
    public static void registerPatchedRenderer(PatchedRenderersEvent.Add event) {
        event.addPatchedEntityRenderer(
            NeoForgeEntityRegistration.COMPANION.get(),
            entityType -> new PCustomHumanoidEntityRenderer<>(Meshes.BIPED, event.getContext(), entityType)
        );
        ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patched renderer with Epic Fight");
    }
}
