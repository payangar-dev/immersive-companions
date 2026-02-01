package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Goal-based gap jumping for companions.
 *
 * This goal runs independently of pathfinding, scanning ahead in the companion's
 * movement direction to detect gaps and execute jumps when appropriate.
 *
 * Approach inspired by "Mobs Attempt Parkour" mod:
 * - Uses direct velocity setting (setDeltaMovement) rather than setJumping()
 * - Runs as a Goal with full control over timing
 * - Decoupled from pathfinder node evaluation
 */
public class CompanionGapJumpGoal extends Goal {

    private final CompanionEntity companion;
    private final Level level;
    private final PathNavigation navigation;

    /** Detected gap info for the pending jump */
    private GapJumpHelper.GapJumpInfo pendingJump = null;

    /** Cooldown to prevent rapid jump attempts */
    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 20;

    /** Ticks spent preparing sprint before jump */
    private int sprintPrepTicks = 0;
    private static final int SPRINT_PREP_TICKS = 5;

    /** Maximum safe fall distance for normal companions */
    private static final int MAX_SAFE_FALL = 3;
    /** Maximum safe fall distance for injured companions */
    private static final int INJURED_MAX_FALL = 1;

    /** How far ahead to scan for gaps (in blocks) */
    private static final double SCAN_DISTANCE = 3.0;

    /** Tracks if we're mid-jump to skip goal while airborne */
    private boolean inJump = false;

    public CompanionGapJumpGoal(CompanionEntity companion) {
        this.companion = companion;
        this.level = companion.level();
        this.navigation = companion.getNavigation();
        // This goal controls movement during jumps
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // Decrease cooldown
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        // Check if landing from a previous jump
        if (inJump) {
            if (companion.onGround()) {
                inJump = false;
            }
            return false;
        }

        // Basic preconditions
        if (!companion.onGround()) {
            return false;
        }
        if (!companion.canPerformAction(ActionType.JUMP)) {
            return false;
        }
        if (!companion.canSprint()) {
            return false;
        }
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        if (companion.isInWater() || companion.isInLava()) {
            return false;
        }

        // Must have an active navigation path
        Path path = navigation.getPath();
        if (path == null || path.isDone()) {
            return false;
        }

        // Scan ahead for a gap
        pendingJump = scanForGapAhead();
        return pendingJump != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue until jump is executed or we lose conditions
        if (pendingJump == null) {
            return false;
        }
        if (inJump) {
            // Stay in goal until we land
            return !companion.onGround();
        }
        if (!companion.onGround()) {
            return false;
        }
        if (!companion.canPerformAction(ActionType.JUMP)) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        sprintPrepTicks = 0;
    }

    @Override
    public void stop() {
        pendingJump = null;
        sprintPrepTicks = 0;
        // Don't stop sprinting here - let the follow goal manage it
    }

    @Override
    public void tick() {
        if (pendingJump == null) {
            return;
        }

        // If airborne, just wait for landing
        if (inJump) {
            return;
        }

        // Phase 1: Start sprinting if not already
        if (!companion.isSprinting()) {
            companion.startSprinting();
            sprintPrepTicks = SPRINT_PREP_TICKS;
            return;
        }

        // Phase 2: Wait for sprint to build momentum
        if (sprintPrepTicks > 0) {
            sprintPrepTicks--;
            return;
        }

        // Phase 3: Check if at edge position and execute jump
        if (isAtJumpEdge()) {
            executeJump();
        }
    }

    /**
     * Checks if the companion is at the edge where the jump should begin.
     */
    private boolean isAtJumpEdge() {
        if (pendingJump == null) {
            return false;
        }

        Vec3 pos = companion.position();
        BlockPos jumpFrom = pendingJump.jumpFrom();

        // Check horizontal distance to jump point
        double dx = pos.x - (jumpFrom.getX() + 0.5);
        double dz = pos.z - (jumpFrom.getZ() + 0.5);
        double horizontalDistSq = dx * dx + dz * dz;

        // Trigger when close to the edge block center
        return horizontalDistSq < 1.0;
    }

    /**
     * Executes the gap jump using direct velocity manipulation.
     */
    private void executeJump() {
        Vec3 vel = companion.getDeltaMovement();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        // Standard jump velocity (same as player)
        double jumpY = 0.42;

        // Add extra height for downward jumps
        if (pendingJump.heightDiff() > 0) {
            jumpY += pendingJump.heightDiff() * 0.08;
        }

        if (speed < 0.15) {
            // Not enough momentum - use facing direction as fallback
            float yaw = companion.getYRot() * Mth.DEG_TO_RAD;
            double minSpeed = 0.3; // Sprint-like speed

            companion.setDeltaMovement(
                -Mth.sin(yaw) * minSpeed,
                jumpY,
                Mth.cos(yaw) * minSpeed
            );
        } else {
            // Preserve horizontal momentum with slight boost for longer gaps
            double boost = 1.0 + (pendingJump.gapWidth() * 0.08);

            companion.setDeltaMovement(
                vel.x * boost,
                jumpY,
                vel.z * boost
            );
        }

        companion.setOnGround(false);
        inJump = true;
        cooldown = COOLDOWN_TICKS;
        pendingJump = null;
    }

    /**
     * Scans ahead in the companion's movement direction to find jumpable gaps.
     * Uses the navigation path direction when available, falls back to velocity.
     *
     * @return GapJumpInfo if a jumpable gap is found, null otherwise
     */
    private GapJumpHelper.GapJumpInfo scanForGapAhead() {
        Vec3 moveDir = getMovementDirection();
        if (moveDir == null) {
            return null;
        }

        BlockPos currentPos = companion.blockPosition();
        int maxFall = companion.isCriticallyInjured() ? INJURED_MAX_FALL : MAX_SAFE_FALL;

        // Scan along movement direction
        return GapJumpHelper.detectGapAlongDirection(level, currentPos, moveDir, maxFall);
    }

    /**
     * Gets the companion's intended movement direction.
     * Prefers navigation target direction, falls back to current velocity.
     *
     * @return Normalized movement direction vector, or null if stationary
     */
    private Vec3 getMovementDirection() {
        Path path = navigation.getPath();

        // Try to get direction from path
        if (path != null && !path.isDone()) {
            int nextIndex = path.getNextNodeIndex();
            if (nextIndex < path.getNodeCount()) {
                Vec3 nextNode = path.getEntityPosAtNode(companion, nextIndex);
                Vec3 pos = companion.position();

                double dx = nextNode.x - pos.x;
                double dz = nextNode.z - pos.z;
                double length = Math.sqrt(dx * dx + dz * dz);

                if (length > 0.1) {
                    return new Vec3(dx / length, 0, dz / length);
                }
            }
        }

        // Fallback to current velocity
        Vec3 vel = companion.getDeltaMovement();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        if (speed > 0.05) {
            return new Vec3(vel.x / speed, 0, vel.z / speed);
        }

        return null;
    }
}
