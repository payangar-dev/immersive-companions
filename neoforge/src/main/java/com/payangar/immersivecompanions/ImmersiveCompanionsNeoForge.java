package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.client.ConfigScreenFactory;
import com.payangar.immersivecompanions.compat.epicfight.EpicFightCompat;
import com.payangar.immersivecompanions.platform.NeoForgeServices;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ImmersiveCompanions.MOD_ID)
public class ImmersiveCompanionsNeoForge {
    public ImmersiveCompanionsNeoForge(IEventBus modEventBus) {
        Services.init(new NeoForgeServices());

        // Register entities
        NeoForgeEntityRegistration.register(modEventBus);

        // Register config screen (client-side only)
        if (FMLLoader.getDist() == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (modContainer, parent) -> ConfigScreenFactory.createConfigScreen(parent)
            );
        }

        // Initialize Epic Fight compatibility if present
        // Uses isolated class loading to prevent NoClassDefFoundError when Epic Fight is absent
        if (ModList.get().isLoaded("epicfight")) {
            initEpicFightCompat(modEventBus);
        }

        ImmersiveCompanions.init();
    }

    /**
     * Isolated method to initialize Epic Fight compatibility.
     * This method references EpicFightCompat which will only be loaded
     * when this method is called, preventing class loading errors when
     * Epic Fight is not installed.
     */
    private void initEpicFightCompat(IEventBus modEventBus) {
        EpicFightCompat.init(modEventBus);
    }
}
