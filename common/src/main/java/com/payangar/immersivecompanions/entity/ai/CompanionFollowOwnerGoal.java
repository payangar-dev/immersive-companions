package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.entity.teleport.SafePositionFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * AI goal that makes owned companions follow their owner.
 * Includes teleportation when the owner gets too far away.
 * Only active when the companion is in FOLLOW mode.
 */
public class CompanionFollowOwnerGoal extends Goal {

    private static final double STOP_DISTANCE = 3.0;
    private static final double START_DISTANCE = 5.0;
    private static final double TELEPORT_DISTANCE = 32.0;
    private static final int PATH_RECALC_DELAY = 10;

    /** Distance threshold to start sprinting (owner is far) */
    private static final double SPRINT_START_DISTANCE = 10.0;
    /** Distance threshold to stop sprinting (caught up with owner) */
    private static final double SPRINT_STOP_DISTANCE = 7.0;

    private final CompanionEntity companion;
    private final double speedModifier;
    private final PathNavigation navigation;
    private final Level level;

    private LivingEntity owner;
    private int timeToRecalcPath;

    public CompanionFollowOwnerGoal(CompanionEntity companion, double speedModifier) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.navigation = companion.getNavigation();
        this.level = companion.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only follow when in FOLLOW mode
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        LivingEntity potentialOwner = getOwner();
        if (potentialOwner == null) {
            return false;
        }
        if (potentialOwner.isSpectator()) {
            return false;
        }
        double distance = companion.distanceToSqr(potentialOwner);
        if (distance < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        this.owner = potentialOwner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop following if mode changed
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        if (navigation.isDone()) {
            return false;
        }
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        double distance = companion.distanceToSqr(owner);
        return distance > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
        if (companion.isSprinting()) {
            companion.stopSprinting();
        }
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }

        // Look at owner while following
        companion.getLookControl().setLookAt(owner, 10.0F, (float) companion.getMaxHeadXRot());

        double distanceSq = companion.distanceToSqr(owner);
        double distance = Math.sqrt(distanceSq);

        // Teleport if too far away
        if (distanceSq > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            teleportToOwner();
            return;
        }

        // Recalculate path periodically
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = PATH_RECALC_DELAY;
            if (!companion.isLeashed() && !companion.isPassenger()) {
                navigation.moveTo(owner, speedModifier);
            }
        }

        if (distance <=  STOP_DISTANCE || !companion.canSprint()) {
            if (companion.isSprinting()) {
                companion.stopSprinting();
            }
            return;
        }

        // Manage sprinting with hysteresis to prevent flickering
        if (!companion.isSprinting() && (owner.isSprinting() || distance > SPRINT_START_DISTANCE)) {
            // Start sprinting when owner is far
            companion.startSprinting();
        } else if (companion.isSprinting() && distance < SPRINT_STOP_DISTANCE && !owner.isSprinting()) {
            // Stop sprinting when caught up
            companion.stopSprinting();
        }
    }

    private void teleportToOwner() {
        BlockPos ownerPos = owner.blockPosition();

        // Use SafePositionFinder with extended search parameters
        BlockPos safePos = SafePositionFinder.findSafePositionExtended(
                level, ownerPos, companion.getType(), 5, 2, 20);

        if (safePos != null) {
            companion.moveTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    companion.getYRot(), companion.getXRot());
            navigation.stop();
        }
    }

    private Player getOwner() {
        return companion.getOwner();
    }
}
