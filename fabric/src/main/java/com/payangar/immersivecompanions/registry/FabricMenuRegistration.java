package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.inventory.CompanionEquipmentMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;

/**
 * Fabric-specific menu type registration.
 * Uses ExtendedScreenHandlerType for menus that require extra data (entity ID).
 */
public class FabricMenuRegistration {

    private static MenuType<CompanionEquipmentMenu> companionEquipmentMenuType;

    /**
     * Registers all menu types for Fabric.
     * Call this from the mod initializer.
     */
    public static void register() {
        // Register the menu type with extended data support
        // The codec serializes the entity ID (Integer) to/from the buffer
        companionEquipmentMenuType = Registry.register(
                BuiltInRegistries.MENU,
                ModMenuTypes.COMPANION_EQUIPMENT_RESOURCE,
                new ExtendedScreenHandlerType<>(
                        (syncId, inventory, entityId) -> new CompanionEquipmentMenu(syncId, inventory, entityId),
                        ByteBufCodecs.VAR_INT
                )
        );

        // Set the supplier in the common holder
        ModMenuTypes.setCompanionEquipmentSupplier(() -> companionEquipmentMenuType);
    }

    public static MenuType<CompanionEquipmentMenu> getCompanionEquipmentMenuType() {
        return companionEquipmentMenuType;
    }
}
