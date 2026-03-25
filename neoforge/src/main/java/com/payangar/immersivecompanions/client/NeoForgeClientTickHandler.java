package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * NeoForge client tick handler for input detection.
 * Runs on the NeoForge event bus (game bus) to handle per-tick client logic.
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ReviveInputHandler.tick();
    }
}
