package com.payangar.immersivecompanions.entity.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Utility class for finding safe teleport positions for companions.
 * Consolidates position-finding logic used by both CompanionTeleportHandler
 * and CompanionFollowOwnerGoal.
 */
public final class SafePositionFinder {

    private static final Random RANDOM = new Random();

    private SafePositionFinder() {
        // Utility class - prevent instantiation
    }

    /**
     * Finds a safe position near the target for teleporting a companion.
     * Searches in a 7x7 area around the target with vertical variance.
     *
     * @param level The level to search in
     * @param center The center position to search around
     * @param entityType The entity type to validate spawn positions for (can be null)
     * @return A safe position, or null if none found
     */
    @Nullable
    public static BlockPos findSafePosition(Level level, BlockPos center, @Nullable EntityType<?> entityType) {
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = RANDOM.nextInt(7) - 3;
            int dz = RANDOM.nextInt(7) - 3;
            BlockPos test = center.offset(dx, 0, dz);

            for (int dy = 0; dy <= 4; dy++) {
                BlockPos pos = test.above(dy);
                if (isSafe(level, pos, entityType)) return pos;
                if (dy > 0) {
                    pos = test.below(dy);
                    if (isSafe(level, pos, entityType)) return pos;
                }
            }
        }
        return null;
    }

    /**
     * Finds a safe position using extended search parameters.
     * Used by CompanionFollowOwnerGoal for same-dimension teleports.
     *
     * @param level The level to search in
     * @param center The center position to search around
     * @param entityType The entity type to validate spawn positions for
     * @param horizontalRange Maximum horizontal offset from center
     * @param verticalRange Maximum vertical offset from center
     * @param attempts Number of random attempts to make
     * @return A safe position, or null if none found
     */
    @Nullable
    public static BlockPos findSafePositionExtended(Level level, BlockPos center,
            @Nullable EntityType<?> entityType, int horizontalRange, int verticalRange, int attempts) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            int dx = RANDOM.nextInt(horizontalRange * 2 + 1) - horizontalRange;
            int dy = RANDOM.nextInt(verticalRange * 2 + 1) - verticalRange;
            int dz = RANDOM.nextInt(horizontalRange * 2 + 1) - horizontalRange;
            BlockPos testPos = center.offset(dx, dy, dz);

            BlockPos validPos = searchVertically(level, testPos, entityType, 4);
            if (validPos != null) {
                return validPos;
            }
        }
        return null;
    }

    /**
     * Searches vertically from a position to find a valid teleport spot.
     *
     * @param level The level to search in
     * @param pos The starting position
     * @param entityType The entity type to validate for
     * @param verticalSearchRange How far up and down to search
     * @return A valid position, or null if none found
     */
    @Nullable
    public static BlockPos searchVertically(Level level, BlockPos pos,
            @Nullable EntityType<?> entityType, int verticalSearchRange) {
        if (isSafe(level, pos, entityType)) {
            return pos;
        }

        for (int yOffset = 1; yOffset <= verticalSearchRange; yOffset++) {
            BlockPos above = pos.above(yOffset);
            if (isSafe(level, above, entityType)) {
                return above;
            }
            BlockPos below = pos.below(yOffset);
            if (isSafe(level, below, entityType)) {
                return below;
            }
        }

        return null;
    }

    /**
     * Checks if a position is safe for teleporting to.
     * A position is safe if:
     * - The block below is solid (can stand on)
     * - The position and the block above are not blocking motion (2 blocks of space)
     *
     * @param level The level to check in
     * @param pos The position to check
     * @param entityType The entity type to validate spawn for (can be null for basic checks)
     * @return true if the position is safe
     */
    public static boolean isSafe(Level level, BlockPos pos, @Nullable EntityType<?> entityType) {
        BlockState groundState = level.getBlockState(pos.below());

        // Check ground is solid
        if (entityType != null) {
            if (!groundState.isValidSpawn(level, pos.below(), entityType)) {
                return false;
            }
        } else {
            if (!groundState.isFaceSturdy(level, pos.below(), Direction.UP)) {
                return false;
            }
        }

        // Check 2 blocks of space above ground
        if (level.getBlockState(pos).blocksMotion()) {
            return false;
        }
        if (level.getBlockState(pos.above()).blocksMotion()) {
            return false;
        }

        return true;
    }
}
