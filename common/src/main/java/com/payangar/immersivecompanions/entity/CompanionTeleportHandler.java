package com.payangar.immersivecompanions.entity;

import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles teleporting companions to their owner when the owner teleports.
 * Maintains a registry of companions for efficient lookup (avoids area searches).
 */
public class CompanionTeleportHandler {

    /**
     * Public record for exposing tracked companion info to debug commands.
     */
    public record TrackedCompanionInfo(UUID companionId, ResourceKey<Level> dimension, BlockPos lastPos) {}

    /**
     * Returns a snapshot of the registry for debug purposes.
     * Maps player UUID to list of their tracked companions.
     */
    public static Map<UUID, List<TrackedCompanionInfo>> getRegistrySnapshot() {
        Map<UUID, List<TrackedCompanionInfo>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, Set<TrackedCompanion>> entry : registry.entrySet()) {
            List<TrackedCompanionInfo> companions = new ArrayList<>();
            for (TrackedCompanion tracked : entry.getValue()) {
                companions.add(new TrackedCompanionInfo(
                        tracked.companionId,
                        tracked.dimension,
                        tracked.lastPos
                ));
            }
            snapshot.put(entry.getKey(), companions);
        }
        return snapshot;
    }

    // === Companion Registry ===
    // Maps player UUID -> Set of their companion UUIDs in FOLLOW mode
    // Also tracks companion dimension and position for loading their chunk

    private static final Map<UUID, Set<TrackedCompanion>> registry = new ConcurrentHashMap<>();

    static final class TrackedCompanion {
        final UUID companionId;
        volatile ResourceKey<Level> dimension;
        volatile BlockPos lastPos;

        TrackedCompanion(UUID companionId, ResourceKey<Level> dimension, BlockPos pos) {
            this.companionId = companionId;
            this.dimension = dimension;
            this.lastPos = pos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrackedCompanion that = (TrackedCompanion) o;
            return Objects.equals(companionId, that.companionId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(companionId);
        }
    }

    // === Registry Management ===

    public static void register(CompanionEntity companion) {
        if (companion.level().isClientSide || companion.getOwnerUUID() == null) return;

        TrackedCompanion tracked = new TrackedCompanion(
                companion.getUUID(),
                companion.level().dimension(),
                companion.blockPosition()
        );

        registry.computeIfAbsent(companion.getOwnerUUID(), k -> ConcurrentHashMap.newKeySet())
                .add(tracked);
    }

    /**
     * Unregisters a companion using the companion entity.
     * Use this when owner is still set on the companion.
     */
    public static void unregister(CompanionEntity companion) {
        if (companion.getOwnerUUID() == null) return;
        unregister(companion.getUUID(), companion.getOwnerUUID());
    }

    /**
     * Unregisters a companion by IDs.
     * Use this when the owner may have already been cleared from the companion.
     */
    public static void unregister(UUID companionId, UUID ownerUUID) {
        if (ownerUUID == null) return;

        Set<TrackedCompanion> companions = registry.get(ownerUUID);
        if (companions != null) {
            companions.removeIf(t -> t.companionId.equals(companionId));
            if (companions.isEmpty()) {
                registry.remove(ownerUUID);
            }
        }
    }

    public static void updatePosition(CompanionEntity companion) {
        if (companion.getOwnerUUID() == null) return;

        Set<TrackedCompanion> companions = registry.get(companion.getOwnerUUID());
        if (companions != null) {
            for (TrackedCompanion t : companions) {
                if (t.companionId.equals(companion.getUUID())) {
                    t.dimension = companion.level().dimension();
                    t.lastPos = companion.blockPosition();
                    break;
                }
            }
        }
    }

    public static void clear() {
        registry.clear();
    }

    // === Teleport Logic ===

    /**
     * Called when a player teleports. Teleports all their FOLLOW mode companions.
     */
    public static void onPlayerTeleport(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos) {
        Set<TrackedCompanion> companions = registry.get(player.getUUID());
        if (companions == null || companions.isEmpty()) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        BlockPos targetBlockPos = BlockPos.containing(targetPos);

        for (TrackedCompanion tracked : companions) {
            teleportCompanion(server, tracked, targetLevel, targetBlockPos);
        }
    }

    private static void teleportCompanion(MinecraftServer server, TrackedCompanion tracked,
                                          ServerLevel targetLevel, BlockPos targetPos) {
        // Get the level where companion currently is
        ServerLevel companionLevel = server.getLevel(tracked.dimension);
        if (companionLevel == null) return;

        // Get the companion entity (may need to load chunk first)
        CompanionEntity companion = (CompanionEntity) companionLevel.getEntity(tracked.companionId);

        if (companion == null) {
            // Companion not loaded - need to load their chunk first
            // Force load the chunk temporarily
            companionLevel.getChunk(tracked.lastPos);
            companion = (CompanionEntity) companionLevel.getEntity(tracked.companionId);
        }

        if (companion == null || companion.getMode() != CompanionMode.FOLLOW) return;

        // Find safe position near target
        BlockPos safePos = findSafePosition(targetLevel, targetPos);
        if (safePos == null) {
            safePos = targetPos;
        }

        // Execute teleport
        if (companionLevel.dimension().equals(targetLevel.dimension())) {
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

        // Update tracking info
        tracked.dimension = targetLevel.dimension();
        tracked.lastPos = safePos;
    }

    @Nullable
    private static BlockPos findSafePosition(ServerLevel level, BlockPos center) {
        Random random = new Random();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(7) - 3;
            int dz = random.nextInt(7) - 3;
            BlockPos test = center.offset(dx, 0, dz);

            for (int dy = 0; dy <= 4; dy++) {
                BlockPos pos = test.above(dy);
                if (isSafe(level, pos)) return pos;
                if (dy > 0) {
                    pos = test.below(dy);
                    if (isSafe(level, pos)) return pos;
                }
            }
        }
        return null;
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && !level.getBlockState(pos).blocksMotion()
                && !level.getBlockState(pos.above()).blocksMotion();
    }
}
