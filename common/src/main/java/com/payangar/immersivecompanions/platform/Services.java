package com.payangar.immersivecompanions.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

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
