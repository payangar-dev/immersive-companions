package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Custom ranged attack goal that includes a charging phase for bow/crossbow animations.
 * This allows the companion to display proper arm poses while charging their weapon.
 *
 * Features kiting behavior - the companion will retreat when enemies get too close,
 * maintaining optimal attack distance for ranged combat.
 */
public class CompanionRangedAttackGoal extends Goal {

    private final CompanionEntity companion;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final float attackRadius;
    private final float attackRadiusSqr;
    private final float minAttackRadius;
    private final float minAttackRadiusSqr;

    private int attackTime = -1;
    private int chargeTime = 0;
    private boolean isCharging = false;
    private int seeTime;
    private int strafeTime = 0;
    private boolean strafingClockwise = false;
    private boolean strafingBackwards = false;
    private int blockedShotTime = 0; // Tracks how long shot has been blocked by friendlies

    /**
     * Creates a ranged attack goal with default minimum distance (5 blocks).
     */
    public CompanionRangedAttackGoal(CompanionEntity companion, double speedModifier, int attackInterval, float attackRadius) {
        this(companion, speedModifier, attackInterval, attackRadius, 5.0F);
    }

    /**
     * Creates a ranged attack goal with configurable minimum distance.
     *
     * @param companion       The companion entity
     * @param speedModifier   Movement speed multiplier
     * @param attackInterval  Ticks between attacks
     * @param attackRadius    Maximum attack range (will approach if farther)
     * @param minAttackRadius Minimum safe distance (will retreat if closer)
     */
    public CompanionRangedAttackGoal(CompanionEntity companion, double speedModifier, int attackInterval,
                                     float attackRadius, float minAttackRadius) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackInterval;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.minAttackRadius = minAttackRadius;
        this.minAttackRadiusSqr = minAttackRadius * minAttackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Gets the charge time based on the equipped weapon.
     * Crossbows take slightly longer to charge than bows.
     * Times are slightly longer than animation duration to ensure full visual completion.
     */
    private int getChargeTime() {
        // Bow full draw animation: ~20 ticks, crossbow load: ~25 ticks
        // Add buffer for animation to fully complete before shooting
        return companion.getMainHandItem().getItem() instanceof CrossbowItem ? 35 : 30;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return companion.canUseRangedWeapon();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !companion.getNavigation().isDone()) && companion.canUseRangedWeapon();
    }

    @Override
    public void start() {
        super.start();
        companion.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        companion.setAggressive(false);
        companion.setCharging(false);
        companion.stopUsingItem();
        isCharging = false;
        chargeTime = 0;
        seeTime = 0;
        strafeTime = 0;
        blockedShotTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = companion.getTarget();
        if (target == null) {
            companion.setCharging(false);
            companion.stopUsingItem();
            isCharging = false;
            return;
        }

        double distSq = companion.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = companion.getSensing().hasLineOfSight(target);

        // Track line of sight
        if (canSee) {
            seeTime++;
        } else {
            seeTime = 0;
        }

        // Determine movement behavior based on distance
        boolean tooFar = distSq > attackRadiusSqr;
        boolean tooClose = distSq < minAttackRadiusSqr;

        // Handle movement - approach, retreat, or strafe
        if (tooFar || !canSee) {
            // Move toward target if too far or can't see
            companion.getNavigation().moveTo(target, speedModifier);
            resetChargingState();
        } else if (tooClose) {
            // Retreat! Target is too close - move backwards
            retreatFromTarget(target);
            // Can still shoot while retreating if we have line of sight
            companion.getLookControl().setLookAt(target, 30.0F, 30.0F);
        } else {
            // In optimal range - strafe while attacking
            companion.getNavigation().stop();
            handleStrafing(target, distSq);
            companion.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        // Handle attack cooldown
        if (attackTime > 0) {
            attackTime--;
            return;
        }

        // Only charge/shoot if we can see the target (even while moving)
        if (!canSee) {
            resetChargingState();
            return;
        }

        // Start or continue charging
        if (!isCharging) {
            isCharging = true;
            chargeTime = 0;
            companion.setCharging(true);
            // Start using the item to trigger vanilla bow/crossbow animations
            companion.startUsingItem(InteractionHand.MAIN_HAND);
        }

        chargeTime++;

        // Check if charge is complete
        int requiredChargeTime = getChargeTime();
        if (chargeTime >= requiredChargeTime) {
            // Check for friendly fire before shooting
            if (!hasClearShot(target)) {
                // Shot is blocked by a friendly - try to reposition
                blockedShotTime++;

                // Force strafe direction change to find clear angle
                if (blockedShotTime % 10 == 0) {
                    strafingClockwise = !strafingClockwise;
                }

                // Actively strafe to find a clear shot
                companion.getMoveControl().strafe(0.0F, strafingClockwise ? 0.8F : -0.8F);

                // Don't hold the charge forever - release after some time to avoid awkward pose
                if (blockedShotTime > 40) {
                    resetChargingState();
                    attackTime = attackIntervalMin / 2; // Shorter cooldown since we didn't actually shoot
                    blockedShotTime = 0;
                }
                return;
            }

            // Clear shot - fire!
            blockedShotTime = 0;
            float power = Math.min((float) chargeTime / 20.0f, 1.0f);
            companion.performRangedAttack(target, power);

            // Reset state
            resetChargingState();
            attackTime = attackIntervalMin;
        }
    }

    /**
     * Resets the charging state when movement interrupts the attack.
     */
    private void resetChargingState() {
        if (isCharging) {
            companion.setCharging(false);
            companion.stopUsingItem();
            isCharging = false;
            chargeTime = 0;
        }
    }

    /**
     * Checks if there's a clear shot to the target without hitting friendly entities.
     * Considers other companions, iron golems, and villagers as friendlies.
     *
     * @param target The target to shoot at
     * @return true if the shot path is clear of friendlies
     */
    private boolean hasClearShot(LivingEntity target) {
        Vec3 eyePos = companion.getEyePosition();
        Vec3 targetPos = target.getEyePosition();
        Vec3 direction = targetPos.subtract(eyePos);
        double distance = direction.length();

        if (distance < 0.1) {
            return true; // Too close to matter
        }

        // Normalize direction
        Vec3 dirNorm = direction.normalize();

        // Create a bounding box along the projectile path
        // We'll check for entities within this corridor
        double corridorWidth = 1.0; // Arrow hitbox width approximation

        // Get all potential friendly entities in the area between companion and target
        AABB searchArea = companion.getBoundingBox().expandTowards(direction).inflate(corridorWidth);
        List<LivingEntity> nearbyEntities = companion.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                entity -> entity != companion && entity != target && isFriendly(entity)
        );

        // Check each friendly entity to see if they're in the line of fire
        for (LivingEntity friendly : nearbyEntities) {
            if (isInLineOfFire(eyePos, dirNorm, distance, friendly, corridorWidth)) {
                return false; // Friendly is in the way
            }
        }

        return true;
    }

    /**
     * Determines if an entity is considered friendly and should not be shot.
     */
    private boolean isFriendly(LivingEntity entity) {
        // Other companions
        if (entity instanceof CompanionEntity) {
            return true;
        }
        // Iron golems (village defenders)
        if (entity instanceof IronGolem) {
            return true;
        }
        // Villagers
        if (entity instanceof AbstractVillager) {
            return true;
        }
        return false;
    }

    /**
     * Checks if an entity is within the projectile's path corridor.
     *
     * @param origin      The starting point of the projectile
     * @param direction   The normalized direction vector
     * @param maxDistance The maximum distance to check
     * @param entity      The entity to check
     * @param corridor    The width of the projectile corridor
     * @return true if the entity is in the line of fire
     */
    private boolean isInLineOfFire(Vec3 origin, Vec3 direction, double maxDistance,
                                   LivingEntity entity, double corridor) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0); // Center of entity
        Vec3 toEntity = entityPos.subtract(origin);

        // Project entity position onto the line of fire
        double projectionLength = toEntity.dot(direction);

        // Entity must be between us and the target
        if (projectionLength < 0 || projectionLength > maxDistance) {
            return false;
        }

        // Calculate perpendicular distance from the line
        Vec3 projectionPoint = origin.add(direction.scale(projectionLength));
        double perpendicularDist = entityPos.distanceTo(projectionPoint);

        // Check if within corridor (accounting for entity size)
        double entityRadius = Math.max(entity.getBbWidth(), entity.getBbHeight()) / 2;
        return perpendicularDist < (corridor + entityRadius);
    }

    /**
     * Moves the companion away from the target to maintain safe distance.
     */
    private void retreatFromTarget(LivingEntity target) {
        Vec3 companionPos = companion.position();
        Vec3 targetPos = target.position();

        // Calculate direction away from target
        Vec3 retreatDir = companionPos.subtract(targetPos).normalize();

        // Calculate retreat position (move back toward optimal range)
        double retreatDistance = minAttackRadius + 2.0; // Move slightly past minimum
        Vec3 retreatPos = companionPos.add(retreatDir.scale(retreatDistance));

        // Move to retreat position with slightly increased speed for urgency
        companion.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, speedModifier * 1.2);
    }

    /**
     * Handles strafing behavior to make the companion harder to hit.
     * Periodically changes strafe direction for unpredictable movement.
     */
    private void handleStrafing(LivingEntity target, double distSq) {
        strafeTime++;

        // Change strafe direction periodically
        if (strafeTime >= 20) {
            if (companion.getRandom().nextFloat() < 0.3F) {
                strafingClockwise = !strafingClockwise;
            }
            if (companion.getRandom().nextFloat() < 0.3F) {
                strafingBackwards = !strafingBackwards;
            }
            strafeTime = 0;
        }

        // Calculate strafe movement
        float strafeForward = strafingBackwards ? -0.5F : 0.5F;
        float strafeRight = strafingClockwise ? 0.5F : -0.5F;

        // Adjust forward/backward based on distance within optimal range
        double optimalDistSq = (attackRadiusSqr + minAttackRadiusSqr) / 2.0;
        if (distSq < optimalDistSq) {
            strafeForward = -0.5F; // Move back slightly if closer than optimal
        } else if (distSq > optimalDistSq * 1.5) {
            strafeForward = 0.5F; // Move forward slightly if farther than optimal
        }

        companion.getMoveControl().strafe(strafeForward, strafeRight);
    }
}
