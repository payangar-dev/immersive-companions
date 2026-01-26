package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.inventory.CompanionEquipmentMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

/**
 * Common menu type holder for cross-platform access.
 * Platform-specific modules populate the COMPANION_EQUIPMENT supplier during registration.
 */
public class ModMenuTypes {

    public static final String COMPANION_EQUIPMENT_ID = "companion_equipment";
    public static final ResourceLocation COMPANION_EQUIPMENT_RESOURCE = ResourceLocation.fromNamespaceAndPath(
            ImmersiveCompanions.MOD_ID, COMPANION_EQUIPMENT_ID);

    private static Supplier<MenuType<CompanionEquipmentMenu>> companionEquipmentSupplier;

    public static MenuType<CompanionEquipmentMenu> getCompanionEquipment() {
        if (companionEquipmentSupplier == null) {
            throw new IllegalStateException("Companion equipment menu type not registered yet!");
        }
        return companionEquipmentSupplier.get();
    }

    public static void setCompanionEquipmentSupplier(Supplier<MenuType<CompanionEquipmentMenu>> supplier) {
        companionEquipmentSupplier = supplier;
    }
}
