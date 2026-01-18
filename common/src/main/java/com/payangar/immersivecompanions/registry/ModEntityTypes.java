package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/**
 * Common entity type holder for cross-platform access.
 * Platform-specific modules populate the COMPANION supplier during registration.
 */
public class ModEntityTypes {

    public static final String COMPANION_ID = "companion";
    public static final ResourceLocation COMPANION_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            ImmersiveCompanions.MOD_ID, COMPANION_ID);

    private static Supplier<EntityType<CompanionEntity>> companionSupplier;

    public static EntityType<CompanionEntity> getCompanion() {
        if (companionSupplier == null) {
            throw new IllegalStateException("Companion entity type not registered yet!");
        }
        return companionSupplier.get();
    }

    public static void setCompanionSupplier(Supplier<EntityType<CompanionEntity>> supplier) {
        companionSupplier = supplier;
    }

    /**
     * Creates the entity type builder for CompanionEntity.
     * Used by both platform-specific registration implementations.
     */
    public static EntityType.Builder<CompanionEntity> createCompanionBuilder() {
        return EntityType.Builder.of(CompanionEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F) // Same as player
                .clientTrackingRange(10);
    }
}
