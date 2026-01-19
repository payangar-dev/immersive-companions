package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.mixin.MobAccessor;
import com.payangar.immersivecompanions.platform.FabricServices;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.registry.FabricEntityRegistration;
import com.payangar.immersivecompanions.spawning.CompanionSpawnLogic;
import net.fabricmc.api.ModInitializer;
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

        // Clear tracked chunks on server stop
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CompanionSpawnLogic.clearTrackedChunks();
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

        ImmersiveCompanions.init();
    }
}
