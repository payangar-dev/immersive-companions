package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.bus.api.IEventBus;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.neoevent.PatchedRenderersEvent;
import yesman.epicfight.client.renderer.patched.entity.PCustomHumanoidEntityRenderer;

/**
 * Client-side Epic Fight compatibility.
 * Registers the patched renderer for companion entities.
 *
 * Note: This class must NOT use @EventBusSubscriber because NeoForge's AutomaticEventSubscriber
 * would try to load it even when Epic Fight is not installed, causing NoClassDefFoundError.
 * Instead, events are registered manually in init().
 */
public class EpicFightClientCompat {

    /**
     * Initializes client-side Epic Fight compatibility.
     * Call this from the mod constructor when Epic Fight is detected (client-side only).
     *
     * @param modEventBus The mod event bus to register listeners on
     */
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightClientCompat::registerPatchedRenderer);
    }

    /**
     * Registers the patched renderer for companion entities.
     * This event is fired on the MOD bus during client-side mod loading.
     */
    private static void registerPatchedRenderer(PatchedRenderersEvent.Add event) {
        event.addPatchedEntityRenderer(
            NeoForgeEntityRegistration.COMPANION.get(),
            entityType -> new PCustomHumanoidEntityRenderer<>(Meshes.BIPED, event.getContext(), entityType)
        );
        ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patched renderer with Epic Fight");
    }
}
