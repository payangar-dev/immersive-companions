package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.command.ModCommands;
import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.CompanionTeleportHandler;
import com.payangar.immersivecompanions.mixin.MobAccessor;
import com.payangar.immersivecompanions.network.FabricNetworking;
import com.payangar.immersivecompanions.platform.FabricServices;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import com.payangar.immersivecompanions.spawning.CompanionSpawnLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;

public class ImmersiveCompanionsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Services.init(new FabricServices());

        // Register entities
        FabricEntityRegistration.register();

        // Register networking payloads
        FabricNetworking.registerPayloads();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ModCommands.register(dispatcher);
        });

        // Clear tracked chunks and teleport registry on server stop
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CompanionSpawnLogic.clearTrackedChunks();
            CompanionTeleportHandler.clear();
        });

        // Handle dimension changes - teleport companions when owner changes dimension
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            CompanionTeleportHandler.onPlayerTeleport(player, destination, player.position());
        });

        // Make monsters target companions
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!ModConfig.get().isEnableMonstersTargetCompanions()) {
                return;
            }

            if (entity instanceof Monster monster) {
                // Exclude Creepers and Endermen
                if (monster instanceof Creeper || monster instanceof EnderMan) {
                    return;
                }

                // Add goal to target companions with lower priority than player targeting
                ((MobAccessor) monster).getTargetSelector().addGoal(3, new NearestAttackableTargetGoal<>(
                        monster, CompanionEntity.class, true));
            }
        });

        // Initialize Waystones compatibility if present
        // Uses isolated class loading to prevent NoClassDefFoundError when Waystones is absent
        if (Services.get().isModLoaded("waystones")) {
            initWaystonesCompat();
        }

        ImmersiveCompanions.init();
    }

    /**
     * Isolated method to initialize Waystones compatibility.
     * This method references WaystonesCompatFabric which will only be loaded
     * when this method is called, preventing class loading errors when
     * Waystones is not installed.
     */
    private void initWaystonesCompat() {
        com.payangar.immersivecompanions.compat.waystones.WaystonesCompatFabric.init();
    }
}
