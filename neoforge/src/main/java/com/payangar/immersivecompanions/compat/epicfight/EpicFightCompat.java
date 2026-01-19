package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import yesman.epicfight.api.neoevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

/**
 * Entry point for Epic Fight mod compatibility.
 * This class is only loaded when Epic Fight is present, using isolated class loading
 * to prevent NoClassDefFoundError when Epic Fight is absent.
 *
 * Registers companion entities with Epic Fight's entity patch system via the Java API,
 * which is required for custom modded entities (datapack JSON doesn't work for them).
 *
 * Note: This class must NOT use @EventBusSubscriber because NeoForge's AutomaticEventSubscriber
 * would try to load it even when Epic Fight is not installed, causing NoClassDefFoundError.
 * Instead, events are registered manually in init().
 */
public class EpicFightCompat {

    /**
     * Initializes Epic Fight compatibility.
     * Call this from the mod constructor when Epic Fight is detected.
     *
     * @param modEventBus The mod event bus to register listeners on
     */
    public static void init(IEventBus modEventBus) {
        ImmersiveCompanions.LOGGER.info("Epic Fight detected - registering companion entity patch");
        modEventBus.addListener(EpicFightCompat::registerEntityPatches);

        // Initialize client-side compat (isolated method to prevent class loading on server)
        if (FMLLoader.getDist() == Dist.CLIENT) {
            initClient(modEventBus);
        }
    }

    /**
     * Isolated method to initialize client-side Epic Fight compatibility.
     * This prevents EpicFightClientCompat from being loaded on the server.
     */
    private static void initClient(IEventBus modEventBus) {
        EpicFightClientCompat.init(modEventBus);
    }

    /**
     * Registers companion entity patches with Epic Fight.
     * This event is fired on the MOD bus during mod loading.
     */
    private static void registerEntityPatches(EntityPatchRegistryEvent event) {
        // Register the biped armature for companions (required for HumanoidMobPatch)
        Armatures.registerEntityTypeArmature(
            NeoForgeEntityRegistration.COMPANION.get(),
            Armatures.BIPED
        );

        // Register the entity patch factory
        event.getTypeEntry().put(
            NeoForgeEntityRegistration.COMPANION.get(),
            entity -> new CompanionEntityPatch((CompanionEntity) entity)
        );
        ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patch with Epic Fight");
    }
}
