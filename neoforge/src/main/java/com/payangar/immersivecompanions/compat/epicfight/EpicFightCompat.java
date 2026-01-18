package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import yesman.epicfight.api.neoevent.EntityPatchRegistryEvent;
import yesman.epicfight.gameasset.Armatures;

/**
 * Entry point for Epic Fight mod compatibility.
 * This class is only loaded when Epic Fight is present, using isolated class loading
 * to prevent NoClassDefFoundError when Epic Fight is absent.
 *
 * Registers companion entities with Epic Fight's entity patch system via the Java API,
 * which is required for custom modded entities (datapack JSON doesn't work for them).
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class EpicFightCompat {

    /**
     * Initializes Epic Fight compatibility.
     * Call this from the mod constructor when Epic Fight is detected.
     *
     * @param modEventBus The mod event bus to register listeners on
     */
    public static void init(IEventBus modEventBus) {
        ImmersiveCompanions.LOGGER.info("Epic Fight detected - registering companion entity patch");
    }

    /**
     * Registers companion entity patches with Epic Fight.
     * This event is fired on the MOD bus during mod loading.
     */
    @SubscribeEvent
    public static void registerEntityPatches(EntityPatchRegistryEvent event) {
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
