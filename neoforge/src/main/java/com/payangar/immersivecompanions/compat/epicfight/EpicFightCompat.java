package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.gameasset.Armatures;

/**
 * Entry point for Epic Fight mod compatibility.
 * This class is only loaded when Epic Fight is present, using isolated class loading
 * to prevent NoClassDefFoundError when Epic Fight is absent.
 *
 * Registers companion entities with Epic Fight's entity patch system via the Java API,
 * which is required for custom modded entities (datapack JSON doesn't work for them).
 */
public class EpicFightCompat {

    /**
     * Initializes Epic Fight compatibility.
     * Call this from the mod constructor when Epic Fight is detected.
     */
    public static void init() {
        ImmersiveCompanions.LOGGER.info("Epic Fight detected - registering companion entity patch");

        // Register entity patch via Epic Fight's event system
        EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(event -> {
            // Register the biped armature for companions (required for HumanoidMobPatch)
            Armatures.registerEntityTypeArmature(
                NeoForgeEntityRegistration.COMPANION.get(),
                Armatures.BIPED
            );

            // Register the entity patch factory
            event.registerEntityPatch(
                NeoForgeEntityRegistration.COMPANION.get(),
                CompanionEntityPatch::new
            );
            ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patch with Epic Fight");
        });

        // Initialize client-side compat (isolated method to prevent class loading on server)
        if (FMLLoader.getDist() == Dist.CLIENT) {
            initClient();
        }
    }

    /**
     * Isolated method to initialize client-side Epic Fight compatibility.
     * This prevents EpicFightClientCompat from being loaded on the server.
     */
    private static void initClient() {
        EpicFightClientCompat.init();
    }
}
