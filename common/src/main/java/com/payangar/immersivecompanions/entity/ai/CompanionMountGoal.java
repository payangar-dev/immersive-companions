package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.entity.teleport.SafePositionFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * AI goal that makes companions mount nearby saddled horses when their owner is mounted.
 *
 * Two-phase behavior:
 * 1. Mounting phase: Companion pathfinds to a nearby saddled, tamed horse and mounts it
 * 2. Following phase: Companion controls the horse's navigation to follow the owner's horse
 *
 * When the owner dismounts, the companion continues riding toward the owner until the
 * horse's navigation completes, then dismounts and resumes normal foot-following.
 */
public class CompanionMountGoal extends Goal {

    /** Search radius for finding available horses */
    private static final double HORSE_SEARCH_RADIUS = 16.0;

    /** Distance to stop when following mounted owner */
    private static final double MOUNTED_FOLLOW_DISTANCE = 6.0;

    /** Teleport threshold for mounted companions (larger than foot-following) */
    private static final double MOUNTED_TELEPORT_DISTANCE = 48.0;

    /** Distance to horse before mounting */
    private static final double MOUNT_DISTANCE = 2.0;

    /** How often to recalculate path (in ticks) */
    private static final int PATH_RECALC_DELAY = 10;

    private final CompanionEntity companion;
    private final PathNavigation navigation;
    private final Level level;

    @Nullable
    private Player owner;

    @Nullable
    private AbstractHorse targetHorse;

    private int timeToRecalcPath;

    /** True when companion has successfully mounted a horse */
    private boolean isMounted;

    public CompanionMountGoal(CompanionEntity companion) {
        this.companion = companion;
        this.navigation = companion.getNavigation();
        this.level = companion.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only in FOLLOW mode
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }

        // Must have an owner
        this.owner = companion.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Owner must be riding a horse
        if (!(owner.getVehicle() instanceof AbstractHorse)) {
            return false;
        }

        // If companion is already mounted, continue with this goal
        if (companion.isPassenger() && companion.getVehicle() instanceof AbstractHorse) {
            return true;
        }

        // Try to find an available horse to mount
        this.targetHorse = findAvailableHorse();
        return targetHorse != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if mode changed
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }

        // Stop if owner is gone
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        boolean ownerMounted = owner.getVehicle() instanceof AbstractHorse;
        boolean companionMounted = companion.isPassenger() && companion.getVehicle() instanceof AbstractHorse;

        // If owner dismounted but companion is still mounted
        if (!ownerMounted && companionMounted) {
            // Check if horse has finished navigating (reached destination or can't get closer)
            AbstractHorse mountedHorse = (AbstractHorse) companion.getVehicle();
            if (mountedHorse.getNavigation().isDone()) {
                return false; // Navigation complete - stop goal, stop() will dismount
            }
            return true; // Still riding to catch up
        }

        // If owner is mounted and companion is mounted, continue
        if (ownerMounted && companionMounted) {
            return true;
        }

        // If owner is mounted but companion hasn't mounted yet, continue if we have a target
        if (ownerMounted && !companionMounted) {
            // Check if target horse is still valid
            if (targetHorse != null && targetHorse.isAlive() && !targetHorse.isVehicle()) {
                return true;
            }
            // Try to find a new horse
            this.targetHorse = findAvailableHorse();
            return targetHorse != null;
        }

        return false;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.isMounted = companion.isPassenger() && companion.getVehicle() instanceof AbstractHorse;
    }

    @Override
    public void stop() {
        // Dismount if still mounted
        if (companion.isPassenger()) {
            companion.stopRiding();
        }

        this.owner = null;
        this.targetHorse = null;
        this.isMounted = false;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }

        // Check if companion became mounted
        if (!isMounted && companion.isPassenger() && companion.getVehicle() instanceof AbstractHorse) {
            isMounted = true;
            navigation.stop();
        }

        if (isMounted) {
            tickMountedFollowing();
        } else {
            tickMounting();
        }
    }

    /**
     * Handles pathfinding to and mounting the target horse.
     */
    private void tickMounting() {
        if (targetHorse == null || !targetHorse.isAlive()) {
            return;
        }

        // If horse got claimed by something else, abort
        if (targetHorse.isVehicle()) {
            targetHorse = null;
            return;
        }

        // Look at the horse
        companion.getLookControl().setLookAt(targetHorse, 10.0F, (float) companion.getMaxHeadXRot());

        double distanceToHorse = companion.distanceToSqr(targetHorse);

        // Close enough to mount
        if (distanceToHorse < MOUNT_DISTANCE * MOUNT_DISTANCE) {
            companion.startRiding(targetHorse);
            return;
        }

        // Pathfind to horse
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = PATH_RECALC_DELAY;
            navigation.moveTo(targetHorse, 1.0);
        }
    }

    /**
     * Handles controlling the mounted horse to follow the owner's horse.
     */
    private void tickMountedFollowing() {
        Entity vehicle = companion.getVehicle();
        if (!(vehicle instanceof AbstractHorse mountedHorse)) {
            isMounted = false;
            return;
        }

        // Determine target position - owner if dismounted, owner's horse if mounted
        Vec3 targetPos;
        boolean ownerMounted = owner.getVehicle() instanceof AbstractHorse;

        if (ownerMounted) {
            AbstractHorse ownerHorse = (AbstractHorse) owner.getVehicle();
            targetPos = ownerHorse.position();
            // Look at owner's horse
            companion.getLookControl().setLookAt(ownerHorse, 10.0F, (float) companion.getMaxHeadXRot());
        } else {
            targetPos = owner.position();
            // Look at owner
            companion.getLookControl().setLookAt(owner, 10.0F, (float) companion.getMaxHeadXRot());
        }

        double distanceSq = mountedHorse.position().distanceToSqr(targetPos);
        double distance = Math.sqrt(distanceSq);

        // Teleport if too far
        if (distance > MOUNTED_TELEPORT_DISTANCE) {
            teleportMountedToOwner(mountedHorse);
            return;
        }

        // Control the horse's navigation to follow
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = PATH_RECALC_DELAY;

            // Only move if far enough away
            if (distance > MOUNTED_FOLLOW_DISTANCE) {
                // Use constant modifier - navigation already multiplies by the horse's MOVEMENT_SPEED attribute
                // This ensures proper linear scaling: fast horses move faster, slow horses move slower
                mountedHorse.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 2.5);
            } else {
                mountedHorse.getNavigation().stop();
            }
        }
    }

    /**
     * Teleports the mounted horse (with companion as passenger) near the owner.
     */
    private void teleportMountedToOwner(AbstractHorse horse) {
        Vec3 ownerPos = owner.position();
        BlockPos searchCenter = BlockPos.containing(ownerPos);

        // Find safe position for the horse
        BlockPos safePos = SafePositionFinder.findSafePositionExtended(
                level, searchCenter, horse.getType(), 5, 2, 20);

        if (safePos != null) {
            // Teleport horse - companion comes along as passenger
            horse.moveTo(
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    horse.getYRot(),
                    horse.getXRot()
            );
            horse.getNavigation().stop();
        }
    }

    /**
     * Finds an available horse nearby that the companion can mount.
     *
     * Criteria:
     * - Must be tamed and saddled
     * - Must not have a rider
     * - Must be alive and not a baby
     * - Must be within search radius
     */
    @Nullable
    private AbstractHorse findAvailableHorse() {
        AABB searchBox = companion.getBoundingBox().inflate(HORSE_SEARCH_RADIUS);
        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, searchBox, horse ->
                horse.isTamed() &&
                horse.isSaddled() &&
                !horse.isVehicle() &&
                horse.isAlive() &&
                !horse.isBaby()
        );

        if (horses.isEmpty()) {
            return null;
        }

        // Return the closest available horse
        AbstractHorse closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (AbstractHorse horse : horses) {
            double distSq = companion.distanceToSqr(horse);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = horse;
            }
        }

        return closest;
    }
}
