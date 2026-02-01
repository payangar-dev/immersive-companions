package com.payangar.immersivecompanions.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class for gap jumping.
 * Provides utilities for detecting gaps across various directions.
 *
 * Used by CompanionGapJumpGoal to detect gaps ahead of the companion
 * in their movement direction.
 */
public class GapJumpHelper {

    /** Maximum gap width that can be jumped (blocks) */
    public static final int MAX_JUMP_DISTANCE = 4;

    /** Maximum depth to scan down when looking for a landing surface */
    private static final int MAX_SCAN_DEPTH = 5;

    /**
     * Metadata for a gap jump.
     * Contains all information needed to execute the jump.
     *
     * @param jumpFrom   Position where the jump should start
     * @param landingPos Position where the companion will land
     * @param gapWidth   Width of the gap in blocks
     * @param heightDiff Height difference from start to landing (positive = landing is lower)
     * @param direction  Direction of the jump (may be null for non-cardinal directions)
     */
    public record GapJumpInfo(
        BlockPos jumpFrom,
        BlockPos landingPos,
        int gapWidth,
        int heightDiff,
        Direction direction
    ) {}

    /**
     * Detects a jumpable gap along a movement direction vector.
     * Used by CompanionGapJumpGoal to scan ahead in the companion's
     * actual movement direction (not just cardinal directions).
     *
     * @param level           The level to check in
     * @param from            Starting position
     * @param direction       Normalized movement direction vector (Y component ignored)
     * @param maxFallDistance Maximum safe fall distance
     * @return GapJumpInfo if a jumpable gap exists, null otherwise
     */
    public static GapJumpInfo detectGapAlongDirection(
        Level level,
        BlockPos from,
        Vec3 direction,
        int maxFallDistance
    ) {
        // Normalize and use only horizontal component
        double dirX = direction.x;
        double dirZ = direction.z;
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length < 0.1) {
            return null;
        }
        dirX /= length;
        dirZ /= length;

        // Check if gap starts in the movement direction
        // Round to nearest block position
        BlockPos ahead = from.offset(
            (int) Math.round(dirX),
            0,
            (int) Math.round(dirZ)
        );

        if (!isGapStart(level, ahead)) {
            return null;
        }

        // Check ceiling clearance for the jump
        if (!hasCeilingClearance(level, from)) {
            return null;
        }

        // Scan for landing across the gap
        for (int dist = 1; dist <= MAX_JUMP_DISTANCE; dist++) {
            // Position beyond the gap (dist + 1 blocks in direction)
            int scanX = from.getX() + (int) Math.round(dirX * (dist + 1));
            int scanZ = from.getZ() + (int) Math.round(dirZ * (dist + 1));
            BlockPos scanPos = new BlockPos(scanX, from.getY(), scanZ);

            BlockPos landing = findLandingSurface(level, scanPos, maxFallDistance);

            if (landing != null) {
                int heightDiff = from.getY() - landing.getY();

                // Only jump if height difference is at most 1 block (up or down)
                // If landing is 2+ blocks lower, companion should just walk off and fall
                if (Math.abs(heightDiff) > 1) {
                    continue; // Too much height difference, try next distance
                }

                // Determine closest cardinal direction for fallback
                Direction cardinalDir = getClosestCardinalDirection(dirX, dirZ);

                return new GapJumpInfo(from, landing, dist, heightDiff, cardinalDir);
            }
        }

        return null;
    }

    /**
     * Checks if there's a jumpable gap in the given cardinal direction.
     * Kept for compatibility with pathfinder-based approaches.
     *
     * @param level           The level to check in
     * @param from            Starting position
     * @param direction       Direction to check for gap
     * @param maxFallDistance Maximum safe fall distance
     * @return GapJumpInfo if a jumpable gap exists, null otherwise
     */
    public static GapJumpInfo detectGapInDirection(
        Level level,
        BlockPos from,
        Direction direction,
        int maxFallDistance
    ) {
        // Only check horizontal directions
        if (direction.getAxis() == Direction.Axis.Y) {
            return null;
        }

        // Check if gap starts ahead
        BlockPos ahead = from.relative(direction);
        if (!isGapStart(level, ahead)) {
            return null;
        }

        // Check ceiling clearance for the jump
        if (!hasCeilingClearance(level, from)) {
            return null;
        }

        // Scan for landing across the gap
        for (int dist = 1; dist <= MAX_JUMP_DISTANCE; dist++) {
            // Position beyond the gap
            BlockPos scanPos = from.relative(direction, dist + 1);
            BlockPos landing = findLandingSurface(level, scanPos, maxFallDistance);

            if (landing != null) {
                int heightDiff = from.getY() - landing.getY();

                // Only jump if height difference is at most 1 block (up or down)
                // If landing is 2+ blocks lower, companion should just walk off and fall
                if (Math.abs(heightDiff) > 1) {
                    continue; // Too much height difference, try next distance
                }

                return new GapJumpInfo(from, landing, dist, heightDiff, direction);
            }
        }

        return null;
    }

    /**
     * Gets the closest cardinal direction to a direction vector.
     */
    private static Direction getClosestCardinalDirection(double dirX, double dirZ) {
        if (Math.abs(dirX) > Math.abs(dirZ)) {
            return dirX > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dirZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * Checks if the given position is the start of a gap.
     * A gap start is air at feet level with no solid ground directly below (within 1-2 blocks).
     */
    public static boolean isGapStart(Level level, BlockPos pos) {
        // Check if air at feet level
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        // Check if there's no solid ground below (it's a real gap, not just a step)
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        // If there's solid ground right below, it's not a gap (might be stairs/slabs)
        if (isSolidForLanding(belowState)) {
            return false;
        }

        // Check one more block down - if also no ground, it's definitely a gap
        BlockPos twoBelow = pos.below(2);
        return !isSolidForLanding(level.getBlockState(twoBelow));
    }

    /**
     * Scans downward from a position to find a valid landing surface.
     *
     * @param level           The level to check in
     * @param startPos        The starting position at the expected landing height
     * @param maxFallDistance Maximum blocks the companion can safely fall
     * @return The landing position, or null if no safe landing found
     */
    public static BlockPos findLandingSurface(Level level, BlockPos startPos, int maxFallDistance) {
        // First check at same level - if there's solid ground with air above, it's a valid landing
        if (isValidLandingSpot(level, startPos)) {
            return startPos;
        }

        // Scan downward for landing surface
        for (int depth = 1; depth <= Math.min(maxFallDistance, MAX_SCAN_DEPTH); depth++) {
            BlockPos checkPos = startPos.below(depth);
            if (isValidLandingSpot(level, checkPos)) {
                return checkPos;
            }
        }

        return null;
    }

    /**
     * Checks if a position is a valid landing spot.
     * Requires solid ground below and air (or passable blocks) at feet and head level.
     */
    public static boolean isValidLandingSpot(Level level, BlockPos pos) {
        BlockState groundState = level.getBlockState(pos.below());
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(pos.above());

        // Need solid ground below
        if (!isSolidForLanding(groundState)) {
            return false;
        }

        // Need passable space at feet and head level
        return isPassable(feetState) && isPassable(headState);
    }

    /**
     * Checks if a block state is solid enough to land on.
     * Includes full blocks, slabs, stairs, etc.
     */
    private static boolean isSolidForLanding(BlockState state) {
        // Use Minecraft's collision check - if it has any collision shape, it's landable
        return !state.getCollisionShape(null, BlockPos.ZERO).isEmpty();
    }

    /**
     * Checks if a block state is passable (can walk/jump through).
     */
    private static boolean isPassable(BlockState state) {
        return state.isAir() || !state.blocksMotion();
    }

    /**
     * Checks if there's enough ceiling clearance for a jump.
     */
    private static boolean hasCeilingClearance(Level level, BlockPos startPos) {
        // Check ~2 blocks above for ceiling
        BlockPos aboveHead = startPos.above(2);
        BlockState aboveState = level.getBlockState(aboveHead);
        return isPassable(aboveState);
    }
}
