package com.payangar.immersivecompanions.registry;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.inventory.CompanionEquipmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge-specific menu type registration using DeferredRegister.
 * Uses IMenuTypeExtension for menus that require extra data (entity ID).
 */
public class NeoForgeMenuRegistration {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ImmersiveCompanions.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CompanionEquipmentMenu>> COMPANION_EQUIPMENT =
            MENU_TYPES.register(ModMenuTypes.COMPANION_EQUIPMENT_ID,
                    () -> IMenuTypeExtension.create(CompanionEquipmentMenu::new));

    /**
     * Registers the DeferredRegister to the mod event bus.
     * Call this from the mod constructor.
     */
    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);

        // Set the supplier in the common holder
        ModMenuTypes.setCompanionEquipmentSupplier(COMPANION_EQUIPMENT);
    }
}
