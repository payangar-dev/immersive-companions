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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

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

    /** Minimum blocks between companions */
    private static final double MIN_COMPANION_SPACING = 1.5;
    /** Distance from owner where companions position */
    private static final double FORMATION_RADIUS = 2.5;
    /** Search radius for finding nearby companions */
    private static final double COMPANION_SEARCH_RADIUS = 10.0;
    /** Golden angle in radians for optimal circular distribution */
    private static final double GOLDEN_ANGLE = Math.toRadians(137.5);
    /** Maximum collision avoidance attempts */
    private static final int MAX_COLLISION_ATTEMPTS = 8;

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
                Vec3 targetPos = calculateSpacedTargetPosition();
                navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, speedModifier);
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
        // Calculate offset position for teleport search center
        Vec3 offsetPos = calculatePositionAtAngle(getCompanionAngle(), FORMATION_RADIUS);
        BlockPos searchCenter = BlockPos.containing(offsetPos);

        // Use SafePositionFinder with extended search parameters
        BlockPos safePos = SafePositionFinder.findSafePositionExtended(
                level, searchCenter, companion.getType(), 5, 2, 20);

        // Fallback to owner position if offset search fails
        if (safePos == null) {
            safePos = SafePositionFinder.findSafePositionExtended(
                    level, owner.blockPosition(), companion.getType(), 5, 2, 20);
        }

        if (safePos != null) {
            companion.moveTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                    companion.getYRot(), companion.getXRot());
            navigation.stop();
        }
    }

    /**
     * Calculates a target position that accounts for spacing from other companions.
     * Uses golden angle distribution for optimal spread when multiple companions follow the same owner.
     */
    private Vec3 calculateSpacedTargetPosition() {
        Vec3 ownerPos = owner.position();
        List<CompanionEntity> nearbyCompanions = getNearbyFollowingCompanions();

        // If alone, go directly to owner
        if (nearbyCompanions.isEmpty()) {
            return ownerPos;
        }

        double baseAngle = getCompanionAngle();
        double minSpacingSq = MIN_COMPANION_SPACING * MIN_COMPANION_SPACING;

        // Try base angle first, then rotate if collision detected
        for (int attempt = 0; attempt < MAX_COLLISION_ATTEMPTS; attempt++) {
            double angle = baseAngle + (attempt * Math.PI / 4); // Rotate 45° each attempt
            Vec3 candidatePos = calculatePositionAtAngle(angle, FORMATION_RADIUS);

            // Check for collision with other companions' target positions
            boolean collision = false;
            for (CompanionEntity other : nearbyCompanions) {
                double otherAngle = getAngleForEntity(other);
                Vec3 otherTargetPos = calculatePositionAtAngleFor(otherAngle, FORMATION_RADIUS, ownerPos);

                if (candidatePos.distanceToSqr(otherTargetPos) < minSpacingSq) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                return candidatePos;
            }
        }

        // Fallback: use base angle position even if there's a collision
        return calculatePositionAtAngle(baseAngle, FORMATION_RADIUS);
    }

    /**
     * Gets companions within search radius that are following the same owner.
     */
    private List<CompanionEntity> getNearbyFollowingCompanions() {
        AABB searchBox = companion.getBoundingBox().inflate(COMPANION_SEARCH_RADIUS);
        return level.getEntitiesOfClass(CompanionEntity.class, searchBox, other ->
                other != companion &&
                other.getMode() == CompanionMode.FOLLOW &&
                other.getOwner() != null &&
                other.getOwner().equals(owner)
        );
    }

    /**
     * Calculates the unique angle for this companion using golden angle distribution.
     */
    private double getCompanionAngle() {
        return getAngleForEntity(companion);
    }

    /**
     * Calculates the unique angle for any entity using golden angle distribution.
     */
    private double getAngleForEntity(CompanionEntity entity) {
        return (entity.getId() * GOLDEN_ANGLE) % (2 * Math.PI);
    }

    /**
     * Calculates a position at the given angle and radius from the owner.
     */
    private Vec3 calculatePositionAtAngle(double angle, double radius) {
        return calculatePositionAtAngleFor(angle, radius, owner.position());
    }

    /**
     * Calculates a position at the given angle and radius from a center point.
     */
    private Vec3 calculatePositionAtAngleFor(double angle, double radius, Vec3 center) {
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        return new Vec3(center.x + offsetX, center.y, center.z + offsetZ);
    }

    private Player getOwner() {
        return companion.getOwner();
    }
}
