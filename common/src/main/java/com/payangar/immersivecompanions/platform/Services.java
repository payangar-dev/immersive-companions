package com.payangar.immersivecompanions.platform;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

import java.nio.file.Path;
import java.util.function.Consumer;

public interface Services {
    Path getConfigDir();
    boolean isDevelopmentEnvironment();
    String getLoaderName();
    boolean isModLoaded(String modId);

    /**
     * Opens a menu for a player with extra data.
     * Platform-specific implementation handles the extended menu opening.
     *
     * @param player       The player to open the menu for
     * @param menuProvider The menu provider
     * @param dataWriter   Consumer that writes extra data to the buffer
     */
    void openMenu(ServerPlayer player, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> dataWriter);

    /**
     * Checks if a player is currently performing the hopak dance animation.
     * Only works with Epic Fight mod.
     *
     * @param player The player to check
     * @return true if the player is dancing hopak, false otherwise
     */
    default boolean isPlayerDancingHopak(Player player) {
        return false;
    }

    /**
     * Plays the greeting wave emote on a companion via Epic Fight.
     * This is a one-shot fire-and-forget animation.
     * Only works with Epic Fight mod on NeoForge.
     *
     * @param companion The companion to play the emote on
     * @return true if the emote was played, false if Epic Fight is not available
     */
    default boolean playGreetingEmote(CompanionEntity companion) {
        return false;
    }

    static Services get() {
        return Holder.INSTANCE;
    }

    static void init(Services services) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Services already initialized!");
        }
        Holder.INSTANCE = services;
    }

    class Holder {
        private static Services INSTANCE;
    }
}
