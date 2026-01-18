package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.platform.FabricServices;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import com.payangar.immersivecompanions.spawning.CompanionSpawnLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ImmersiveCompanionsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Services.init(new FabricServices());

        // Register entities
        FabricEntityRegistration.register();

        // Clear tracked chunks on server stop
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CompanionSpawnLogic.clearTrackedChunks();
        });

        ImmersiveCompanions.init();
    }
}
