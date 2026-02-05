package com.payangar.immersivecompanions.platform;

import com.payangar.immersivecompanions.compat.epicfight.EpicFightEmoteHelper;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NeoForgeServices implements Services {

    /**
     * Tracks players currently dancing (hopak emote).
     * Updated via C2S packet from client-side emote detection.
     */
    private static final Set<UUID> dancingPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public String getLoaderName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> dataWriter) {
        player.openMenu(menuProvider, dataWriter);
    }

    @Override
    public boolean isPlayerDancingHopak(Player player) {
        if (!ModList.get().isLoaded("epicfight")) {
            return false;
        }
        // Dance state is synced from client via SyncPlayerDancePayload
        return dancingPlayers.contains(player.getUUID());
    }

    /**
     * Updates a player's dancing state. Called from the network handler
     * when receiving client-side dance detection packets.
     *
     * @param playerId The player's UUID
     * @param dancing  Whether the player is currently dancing
     */
    public static void setPlayerDancing(UUID playerId, boolean dancing) {
        if (dancing) {
            dancingPlayers.add(playerId);
        } else {
            dancingPlayers.remove(playerId);
        }
    }

    /**
     * Clears a specific player's dancing state.
     * Called when a player disconnects.
     *
     * @param playerId The player's UUID
     */
    public static void clearDancingPlayer(UUID playerId) {
        dancingPlayers.remove(playerId);
    }

    /**
     * Clears all dancing player states.
     * Called when the server stops.
     */
    public static void clearAllDancingPlayers() {
        dancingPlayers.clear();
    }

    @Override
    public boolean playGreetingEmote(CompanionEntity companion) {
        if (!ModList.get().isLoaded("epicfight")) {
            return false;
        }
        return EpicFightEmoteHelper.playGreetingEmote(companion);
    }
}
