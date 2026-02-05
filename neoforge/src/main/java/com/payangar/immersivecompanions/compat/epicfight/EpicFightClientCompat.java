package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.network.SyncPlayerDancePayload;
import com.payangar.immersivecompanions.registry.NeoForgeEntityRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Client-side Epic Fight compatibility.
 * Registers the patched renderer for companion entities and
 * handles client-side emote detection for dance sync.
 */
public class EpicFightClientCompat {

    /** Tracks the previous dance state to detect changes */
    private static boolean wasPlayerDancing = false;

    /**
     * Initializes client-side Epic Fight compatibility.
     * Call this from EpicFightCompat.init() when Epic Fight is detected (client-side only).
     */
    public static void init() {
        // Register patched renderer via Epic Fight's client event system
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(event -> {
            event.addPatchedEntityRenderer(
                NeoForgeEntityRegistration.COMPANION.get(),
                entityType -> new PCompanionRenderer(event.getContext(), entityType)
            );
            ImmersiveCompanions.LOGGER.debug("Registered CompanionEntity patched renderer with Epic Fight");
        });

        // Register client tick handler for dance detection
        NeoForge.EVENT_BUS.addListener(EpicFightClientCompat::onClientTick);
    }

    /**
     * Client tick handler that detects when the local player starts or stops
     * dancing the hopak emote and syncs the state to the server.
     */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            // Reset state when not in a world
            if (wasPlayerDancing) {
                wasPlayerDancing = false;
            }
            return;
        }

        boolean isCurrentlyDancing = isLocalPlayerDancingHopak(player);

        // Only send packet on state change
        if (isCurrentlyDancing != wasPlayerDancing) {
            wasPlayerDancing = isCurrentlyDancing;
            PacketDistributor.sendToServer(new SyncPlayerDancePayload(isCurrentlyDancing));
        }
    }

    /**
     * Checks if the local player is currently performing the hopak dance animation.
     * Uses the ClientAnimator which has the emote state (unlike ServerAnimator).
     *
     * @param player The local player to check
     * @return true if the player is dancing hopak
     */
    private static boolean isLocalPlayerDancingHopak(LocalPlayer player) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                player, LivingEntityPatch.class);

        if (patch == null) {
            return false;
        }

        return patch.getAnimator().isPlaying(Animations.BIPED_HOPAK);
    }
}
