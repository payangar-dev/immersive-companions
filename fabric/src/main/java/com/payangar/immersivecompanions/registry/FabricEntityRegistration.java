package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

/**
 * Fabric-specific entity registration.
 */
public class FabricEntityRegistration {

    private static EntityType<CompanionEntity> companionEntityType;

    /**
     * Registers all entities for Fabric.
     * Call this from the mod initializer.
     */
    public static void register() {
        // Register the entity type
        companionEntityType = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ModEntityTypes.COMPANION_RESOURCE,
                ModEntityTypes.createCompanionBuilder().build(ModEntityTypes.COMPANION_ID)
        );

        // Register attributes
        FabricDefaultAttributeRegistry.register(companionEntityType, CompanionEntity.createAttributes());

        // Set the supplier in the common holder
        ModEntityTypes.setCompanionSupplier(() -> companionEntityType);
    }

    public static EntityType<CompanionEntity> getCompanionEntityType() {
        return companionEntityType;
    }
}
