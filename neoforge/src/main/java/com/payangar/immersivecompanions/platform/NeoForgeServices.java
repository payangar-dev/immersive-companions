package com.payangar.immersivecompanions.platform;

import com.payangar.immersivecompanions.compat.epicfight.EpicFightEmoteHelper;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Consumer;

public class NeoForgeServices implements Services {

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
    public boolean playGreetingEmote(CompanionEntity companion) {
        if (!ModList.get().isLoaded("epicfight")) {
            return false;
        }
        return EpicFightEmoteHelper.playGreetingEmote(companion);
    }
}
