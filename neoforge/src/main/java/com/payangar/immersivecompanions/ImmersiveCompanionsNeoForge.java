package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.platform.NeoForgeServices;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ImmersiveCompanions.MOD_ID)
public class ImmersiveCompanionsNeoForge {
    public ImmersiveCompanionsNeoForge(IEventBus modEventBus) {
        Services.init(new NeoForgeServices());

        // Register entities
        NeoForgeEntityRegistration.register(modEventBus);

        ImmersiveCompanions.init();
    }
}
