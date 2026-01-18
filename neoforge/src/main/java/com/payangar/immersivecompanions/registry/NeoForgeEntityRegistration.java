package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge-specific entity registration using DeferredRegister.
 */
public class NeoForgeEntityRegistration {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ImmersiveCompanions.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CompanionEntity>> COMPANION =
            ENTITY_TYPES.register(ModEntityTypes.COMPANION_ID,
                    () -> ModEntityTypes.createCompanionBuilder().build(ModEntityTypes.COMPANION_ID));

    /**
     * Registers the DeferredRegister to the mod event bus.
     * Call this from the mod constructor.
     */
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(NeoForgeEntityRegistration::registerAttributes);

        // Set the supplier in the common holder
        ModEntityTypes.setCompanionSupplier(COMPANION);
    }

    /**
     * Registers entity attributes for the companion entity.
     */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMPANION.get(), CompanionEntity.createAttributes().build());
    }
}
