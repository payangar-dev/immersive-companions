package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Helper class for detecting gaps ahead of a moving companion and triggering jumps.
 * This provides runtime tick-based gap detection rather than complex pathfinding integration.
 */
public class GapJumpHelper {

    /** Minimum horizontal speed required to consider gap detection (blocks/tick) */
    private static final double MIN_MOVEMENT_SPEED = 0.05;

    /** Maximum gap width that can be jumped (blocks) */
    private static final int MAX_JUMP_DISTANCE = 4;

    /** How far ahead to check for gap start (blocks) */
    private static final double GAP_DETECTION_DISTANCE = 1.0;

    /** Maximum depth to scan down when looking for a landing surface */
    private static final int MAX_SCAN_DEPTH = 5;

    /** Minimum clearance above the companion for jumping (blocks) */
    private static final double MIN_CEILING_CLEARANCE = 1.5;

    /** How close to gap edge before triggering jump (blocks) */
    private static final double JUMP_TRIGGER_DISTANCE = 0.8;

    /**
     * Information about a detected gap ahead of the companion.
     *
     * @param detected    Whether a gap was detected
     * @param width       Width of the gap in blocks
     * @param landingPos  Position where the companion would land
     * @param heightDiff  Height difference from start to landing (positive = landing is lower)
     * @param isSafe      Whether the landing is within safe fall distance
     */
    public record GapInfo(boolean detected, int width, BlockPos landingPos, int heightDiff, boolean isSafe) {
        public static final GapInfo NONE = new GapInfo(false, 0, null, 0, false);
    }

    /**
     * Detects if there's a gap ahead in the companion's movement direction.
     * Only checks when the companion is moving fast enough (sprinting).
     *
     * @param companion The companion to check for gaps ahead of
     * @return Information about the detected gap, or GapInfo.NONE if no gap
     */
    public static GapInfo detectGapAhead(CompanionEntity companion) {
        Vec3 velocity = companion.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Don't check if not moving fast enough
        if (horizontalSpeed < MIN_MOVEMENT_SPEED) {
            return GapInfo.NONE;
        }

        // Normalize direction
        double dirX = velocity.x / horizontalSpeed;
        double dirZ = velocity.z / horizontalSpeed;

        Level level = companion.level();
        Vec3 pos = companion.position();

        // Check position ahead for gap start
        double checkX = pos.x + dirX * GAP_DETECTION_DISTANCE;
        double checkZ = pos.z + dirZ * GAP_DETECTION_DISTANCE;
        BlockPos checkPos = BlockPos.containing(checkX, pos.y, checkZ);

        // Is there a gap starting here? (air at feet level, no solid ground below)
        if (!isGapStart(level, checkPos)) {
            return GapInfo.NONE;
        }

        // Found a gap start - now scan forward to find landing surface
        int maxFallDistance = companion.getMaxFallDistance();
        BlockPos startPos = BlockPos.containing(pos.x, pos.y, pos.z);

        for (int distance = 1; distance <= MAX_JUMP_DISTANCE; distance++) {
            double scanX = pos.x + dirX * (GAP_DETECTION_DISTANCE + distance);
            double scanZ = pos.z + dirZ * (GAP_DETECTION_DISTANCE + distance);
            BlockPos scanPos = BlockPos.containing(scanX, pos.y, scanZ);

            // Scan downward for a landing surface
            BlockPos landingPos = findLandingSurface(level, scanPos, maxFallDistance);
            if (landingPos != null) {
                int heightDiff = startPos.getY() - landingPos.getY();
                boolean isSafe = heightDiff <= maxFallDistance;

                // Check ceiling clearance for the jump
                if (!hasCeilingClearance(level, companion, startPos)) {
                    return GapInfo.NONE;
                }

                return new GapInfo(true, distance, landingPos, heightDiff, isSafe);
            }
        }

        // No landing surface found within jump range
        return GapInfo.NONE;
    }

    /**
     * Checks if the given position is the start of a gap.
     * A gap start is air at feet level with no solid ground directly below (within 1-2 blocks).
     */
    private static boolean isGapStart(Level level, BlockPos pos) {
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
    private static BlockPos findLandingSurface(Level level, BlockPos startPos, int maxFallDistance) {
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
    private static boolean isValidLandingSpot(Level level, BlockPos pos) {
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
    private static boolean hasCeilingClearance(Level level, CompanionEntity companion, BlockPos startPos) {
        // Check ~2 blocks above for ceiling
        BlockPos aboveHead = startPos.above(2);
        BlockState aboveState = level.getBlockState(aboveHead);
        return isPassable(aboveState);
    }

    /**
     * Determines if the companion should attempt a gap jump.
     * All conditions must be met for a jump to be triggered.
     *
     * @param companion The companion
     * @param gap       The detected gap info
     * @return true if the companion should jump
     */
    public static boolean shouldJump(CompanionEntity companion, GapInfo gap) {
        // Must have detected a safe gap
        if (!gap.detected || !gap.isSafe) {
            return false;
        }

        // Must be sprinting
        if (!companion.isSprinting()) {
            return false;
        }

        // Must be on ground
        if (!companion.onGround()) {
            return false;
        }

        // Must be able to perform jump action
        if (!companion.canPerformAction(ActionType.JUMP)) {
            return false;
        }

        // Must not be in water or lava
        if (companion.isInWater() || companion.isInLava()) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the companion is close enough to the gap edge to trigger a jump.
     *
     * @param companion The companion
     * @return true if within trigger distance of a gap
     */
    public static boolean isNearGapEdge(CompanionEntity companion) {
        Vec3 velocity = companion.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        if (horizontalSpeed < MIN_MOVEMENT_SPEED) {
            return false;
        }

        // Normalize direction
        double dirX = velocity.x / horizontalSpeed;
        double dirZ = velocity.z / horizontalSpeed;

        Level level = companion.level();
        Vec3 pos = companion.position();

        // Check at trigger distance
        double checkX = pos.x + dirX * JUMP_TRIGGER_DISTANCE;
        double checkZ = pos.z + dirZ * JUMP_TRIGGER_DISTANCE;
        BlockPos checkPos = BlockPos.containing(checkX, pos.y, checkZ);

        return isGapStart(level, checkPos);
    }

    /**
     * Performs the gap jump by applying appropriate velocity.
     * The jump strength is based on the gap width and height difference.
     *
     * @param companion The companion to make jump
     * @param gap       The gap information for calculating jump velocity
     */
    public static void performGapJump(CompanionEntity companion, GapInfo gap) {
        Vec3 velocity = companion.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Calculate jump velocity
        // Base upward velocity similar to player jump (0.42)
        double baseJumpVelocity = 0.42;

        // Adjust for height difference - need more height if landing is higher
        // (though typically landing is same level or lower)
        double heightAdjustment = 0.0;
        if (gap.heightDiff < 0) {
            // Landing is higher - need extra upward velocity
            heightAdjustment = Math.abs(gap.heightDiff) * 0.1;
        }

        // Calculate forward boost based on gap width
        // Wider gaps need more forward momentum
        double forwardBoost = 0.1 + (gap.width * 0.05);

        // Apply jump
        double dirX = velocity.x / horizontalSpeed;
        double dirZ = velocity.z / horizontalSpeed;

        companion.setDeltaMovement(
            velocity.x + dirX * forwardBoost,
            baseJumpVelocity + heightAdjustment,
            velocity.z + dirZ * forwardBoost
        );

        // Mark as jumping (this triggers jump animation and prevents double-jumps)
        companion.setOnGround(false);
    }
}
