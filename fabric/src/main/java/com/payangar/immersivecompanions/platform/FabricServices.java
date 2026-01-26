package com.payangar.immersivecompanions.platform;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.nio.file.Path;
import java.util.function.Consumer;

public class FabricServices implements Services {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getLoaderName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider menuProvider, Consumer<RegistryFriendlyByteBuf> dataWriter) {
        player.openMenu(new ExtendedScreenHandlerFactory<Integer>() {
            @Override
            public Component getDisplayName() {
                return menuProvider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
                return menuProvider.createMenu(syncId, inventory, player);
            }

            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                // The dataWriter consumer writes entity ID - we extract it here
                // This is a workaround since Fabric's API expects a typed object
                // We'll write to a buffer, then read the entity ID back
                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                        io.netty.buffer.Unpooled.buffer(),
                        player.registryAccess());
                dataWriter.accept(buf);
                buf.readerIndex(0);
                return buf.readVarInt();
            }
        });
    }
}
