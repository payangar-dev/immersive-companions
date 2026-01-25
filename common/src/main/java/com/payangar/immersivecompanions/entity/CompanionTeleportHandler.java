package com.payangar.immersivecompanions.entity;

import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.entity.teleport.SafePositionFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles teleporting companions to their owner when the owner teleports.
 * Uses real-time entity search (like Waystones) instead of registry tracking.
 */
public class CompanionTeleportHandler {

    private static final double SEARCH_RADIUS = 16.0; // blocks

    // === External Teleport Deduplication ===
    private static final Map<UUID, Long> recentExternalTeleports = new ConcurrentHashMap<>();
    private static final long EXTERNAL_TELEPORT_COOLDOWN_MS = 1000;

    public static void markExternallyHandled(UUID companionId, long timestamp) {
        recentExternalTeleports.put(companionId, timestamp);
    }

    public static boolean wasRecentlyHandledExternally(UUID companionId) {
        Long timestamp = recentExternalTeleports.get(companionId);
        if (timestamp == null) return false;
        if (System.currentTimeMillis() - timestamp > EXTERNAL_TELEPORT_COOLDOWN_MS) {
            recentExternalTeleports.remove(companionId);
            return false;
        }
        return true;
    }

    /**
     * Finds all companions near the player that should teleport.
     * Uses real-time search like Waystones.
     */
    public static List<CompanionEntity> getCompanionsForTeleport(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(
            CompanionEntity.class,
            new AABB(player.blockPosition()).inflate(SEARCH_RADIUS),
            companion -> isValidForTeleport(companion, player)
        );
    }

    private static boolean isValidForTeleport(CompanionEntity companion, ServerPlayer player) {
        return companion.isAlive()
            && !companion.isRemoved()
            && !companion.isCriticallyInjured()
            && companion.getMode() == CompanionMode.FOLLOW
            && player.getUUID().equals(companion.getOwnerUUID());
    }

    /**
     * Called when a player teleports. Teleports all nearby FOLLOW mode companions.
     */
    public static void onPlayerTeleport(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        List<CompanionEntity> companions = getCompanionsForTeleport(player);
        if (companions.isEmpty()) return;

        BlockPos targetBlockPos = BlockPos.containing(targetPos);

        for (CompanionEntity companion : companions) {
            if (wasRecentlyHandledExternally(companion.getUUID())) {
                continue;
            }
            teleportCompanion(companion, targetLevel, targetBlockPos);
        }
    }

    private static void teleportCompanion(CompanionEntity companion, ServerLevel targetLevel, BlockPos targetPos) {
        BlockPos safePos = SafePositionFinder.findSafePosition(targetLevel, targetPos, companion.getType());
        if (safePos == null) {
            safePos = targetPos;
        }

        ServerLevel currentLevel = (ServerLevel) companion.level();

        if (currentLevel.dimension().equals(targetLevel.dimension())) {
            companion.moveTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    companion.getYRot(), companion.getXRot());
            companion.getNavigation().stop();
        } else {
            companion.changeDimension(new DimensionTransition(
                    targetLevel,
                    new Vec3(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5),
                    Vec3.ZERO, companion.getYRot(), companion.getXRot(),
                    DimensionTransition.DO_NOTHING
            ));
        }
    }

    public static void clear() {
        recentExternalTeleports.clear();
    }
}
