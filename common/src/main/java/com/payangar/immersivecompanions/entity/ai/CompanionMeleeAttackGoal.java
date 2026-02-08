package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Wrapper for MeleeAttackGoal that only activates for melee companions
 * when combat is not disabled.
 *
 * Mounted combat uses a charge-and-retreat pattern: the companion rides toward
 * the target at speed, strikes as it passes through melee range, then retreats
 * away before turning to charge again from a new angle.
 */
public class CompanionMeleeAttackGoal extends MeleeAttackGoal {

    /** Squared distance to start sprinting toward target (6 blocks) */
    private static final double SPRINT_START_DISTANCE_SQ = 36.0;
    /** Squared distance to stop sprinting (3 blocks, in melee range) */
    private static final double SPRINT_STOP_DISTANCE_SQ = 9.0;

    // --- Mounted charge-and-retreat constants ---
    /** Speed multiplier when charging toward the target */
    private static final double CHARGE_SPEED = 2.5;
    /** Speed multiplier when retreating away from the target */
    private static final double RETREAT_SPEED = 2.0;
    /** How far to retreat before turning for the next charge (blocks) */
    private static final double RETREAT_DISTANCE = 10.0;
    private static final double RETREAT_DISTANCE_SQ = RETREAT_DISTANCE * RETREAT_DISTANCE;
    /** Squared distance that triggers retreat (3 blocks - close pass) */
    private static final double PASS_THROUGH_DISTANCE_SQ = 9.0;
    /** Minimum ticks to spend retreating before allowing next charge (1.5 sec) */
    private static final int MIN_RETREAT_TICKS = 30;
    /** Ticks without progress before repositioning (3 sec) */
    private static final int STUCK_THRESHOLD_TICKS = 60;
    /** Ticks between horse path recalculations (0.5 sec) */
    private static final int MOUNTED_PATH_RECALC_DELAY = 10;

    private final CompanionEntity companion;

    // --- Mounted combat state ---
    private enum MountedCombatPhase {
        /** Riding toward the target at speed */
        CHARGING,
        /** Pulling away after a close pass, before turning for the next charge */
        RETREATING
    }

    private MountedCombatPhase mountedCombatPhase = MountedCombatPhase.CHARGING;
    private int mountedPathRecalcTimer = 0;
    private int retreatTicksRemaining = 0;
    /** Own cooldown for mounted attacks, since vanilla's ticksUntilNextAttack is only decremented inside super.tick() */
    private int mountedAttackCooldown = 0;
    /** Tracks the closest distance reached during a charge, for stuck detection */
    private double chargeClosestDistanceSq = Double.MAX_VALUE;
    private int chargeStuckTicks = 0;

    public CompanionMeleeAttackGoal(CompanionEntity companion, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(companion, speedModifier, followingTargetEvenIfNotSeen);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        // Only companions without ranged weapons use this goal
        if (companion.canUseRangedWeapon()) {
            return false;
        }
        // Don't attack when combat is disabled (e.g., critical injury, passive stance)
        if (companion.isCombatDisabled()) {
            return false;
        }
        // When mounted, vanilla's createPath() fails for passengers.
        // Bypass it - just check we have a valid target.
        if (companion.isMountedOnHorse()) {
            LivingEntity target = companion.getTarget();
            return target != null && target.isAlive();
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if switched to ranged weapon or combat disabled
        if (companion.canUseRangedWeapon()) {
            return false;
        }
        if (companion.isCombatDisabled()) {
            return false;
        }
        // When mounted, bypass vanilla's pathfinding checks (companion's own nav is inactive)
        if (companion.isMountedOnHorse()) {
            LivingEntity target = companion.getTarget();
            return target != null && target.isAlive();
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        companion.setAggressive(true);
        // Reset mounted combat state for a fresh charge
        mountedCombatPhase = MountedCombatPhase.CHARGING;
        chargeClosestDistanceSq = Double.MAX_VALUE;
        chargeStuckTicks = 0;
        mountedPathRecalcTimer = 0;
        retreatTicksRemaining = 0;
        mountedAttackCooldown = 0;
        // Clear any stale path on the companion's own navigation when mounted.
        // Without this, PathNavigation.tick() (called from Mob.aiStep()) would keep
        // feeding the companion's MoveControl, interfering with the horse's movement.
        if (companion.isMountedOnHorse()) {
            companion.getNavigation().stop();
        }
    }

    @Override
    public void tick() {
        AbstractHorse horse = companion.getMountedHorse();

        if (horse != null) {
            tickMounted(horse);
        } else {
            tickOnFoot();
            // Reset mounted state for next mount
            mountedCombatPhase = MountedCombatPhase.CHARGING;
            mountedAttackCooldown = 0;
        }
    }

    /**
     * Handles melee combat while mounted using a charge-and-retreat pattern.
     *
     * The companion charges toward the target at speed. When it passes within
     * melee range, a dedicated attack cooldown triggers the strike. After the
     * close pass, the companion retreats away with a random lateral offset,
     * then turns to charge again from a new angle.
     *
     * We intentionally avoid calling super.tick() here — it would run the companion's
     * own pathfinding, activating its MoveControl (setSpeed/setZza on the passenger),
     * which interferes with the horse's AI-driven navigation and causes slowdown.
     */
    private void tickMounted(AbstractHorse horse) {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        companion.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Check if owner dismounted - if so, dismount companion too
        Player owner = companion.getOwner();
        if (owner != null && !(owner.getVehicle() instanceof AbstractHorse)) {
            horse.getNavigation().stop();
            companion.stopRiding();
            return;
        }

        double distanceSq = companion.distanceToSqr(target);

        switch (mountedCombatPhase) {
            case CHARGING -> tickCharging(horse, target, distanceSq);
            case RETREATING -> tickRetreating(horse, target, distanceSq);
        }

        // Handle attack cooldown and execution directly instead of calling super.tick().
        // super.tick() would run the companion's own pathfinding, activating its MoveControl
        // (setSpeed/setZza), which interferes with the horse's navigation and causes slowdown.
        if (mountedAttackCooldown > 0) {
            mountedAttackCooldown--;
        }
        if (mountedAttackCooldown <= 0 && this.canPerformAttack(target)) {
            mountedAttackCooldown = adjustedTickDelay(20);
            if (companion.hasShield()) {
                companion.stopUsingItem();
                if (companion.getShieldCoolDown() == 0) {
                    companion.setShieldCoolDown(8);
                }
            }
            companion.swing(InteractionHand.MAIN_HAND);
            companion.doHurtTarget(target);
        }
    }

    /**
     * Charge phase: ride toward the target. Transitions to retreat on close pass,
     * or repositions sideways if stuck behind an obstacle.
     */
    private void tickCharging(AbstractHorse horse, LivingEntity target, double distanceSq) {
        // Navigate toward target on a recalc timer
        if (--mountedPathRecalcTimer <= 0) {
            mountedPathRecalcTimer = MOUNTED_PATH_RECALC_DELAY;
            horse.getNavigation().moveTo(target, CHARGE_SPEED);
        }

        // Transition to retreat when close enough (passing through)
        if (distanceSq <= PASS_THROUGH_DISTANCE_SQ) {
            beginRetreat(horse, target);
            return;
        }

        // Stuck detection: track closest distance, reposition if no progress
        if (distanceSq < chargeClosestDistanceSq) {
            chargeClosestDistanceSq = distanceSq;
            chargeStuckTicks = 0;
        } else {
            chargeStuckTicks++;
            if (chargeStuckTicks > STUCK_THRESHOLD_TICKS) {
                repositionForCharge(horse, target);
                chargeStuckTicks = 0;
                chargeClosestDistanceSq = Double.MAX_VALUE;
            }
        }
    }

    /**
     * Calculates a retreat point away from the target with a random lateral offset
     * (so each subsequent charge comes from a different angle) and switches to retreat phase.
     */
    private void beginRetreat(AbstractHorse horse, LivingEntity target) {
        mountedCombatPhase = MountedCombatPhase.RETREATING;
        retreatTicksRemaining = MIN_RETREAT_TICKS;

        // Retreat away from target with random lateral offset for varied charge angles
        Vec3 away = companion.position().subtract(target.position());
        double horizontalDist = away.horizontalDistance();
        Vec3 awayDir = horizontalDist > 0.1
                ? new Vec3(away.x / horizontalDist, 0, away.z / horizontalDist)
                : Vec3.atLowerCornerOf(companion.getDirection().getNormal()); // fallback: facing direction

        // Perpendicular vector for lateral offset
        Vec3 perp = new Vec3(-awayDir.z, 0, awayDir.x);
        double lateralOffset = (companion.getRandom().nextDouble() - 0.5) * 6.0;
        Vec3 retreatPos = companion.position()
                .add(awayDir.scale(RETREAT_DISTANCE))
                .add(perp.scale(lateralOffset));

        horse.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, RETREAT_SPEED);
    }

    /**
     * Retreat phase: ride away from the target. Transitions back to charging once
     * the minimum retreat time has passed and the horse is far enough away (or nav finished).
     */
    private void tickRetreating(AbstractHorse horse, LivingEntity target, double distanceSq) {
        retreatTicksRemaining--;

        // Transition back to charging when: min retreat time passed AND (far enough OR nav finished)
        if (retreatTicksRemaining <= 0
                && (distanceSq >= RETREAT_DISTANCE_SQ || horse.getNavigation().isDone())) {
            mountedCombatPhase = MountedCombatPhase.CHARGING;
            chargeClosestDistanceSq = Double.MAX_VALUE;
            chargeStuckTicks = 0;
            mountedPathRecalcTimer = 0; // recalc immediately on next charge tick
        }
    }

    /**
     * When stuck during a charge (obstacle blocking the path), repositions the horse
     * to a perpendicular angle to try a different approach.
     */
    private void repositionForCharge(AbstractHorse horse, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(companion.position()).normalize();
        Vec3 perp = new Vec3(-toTarget.z, 0, toTarget.x);
        double side = companion.getRandom().nextBoolean() ? 1.0 : -1.0;
        Vec3 repositionPos = companion.position().add(perp.scale(side * 8.0));
        horse.getNavigation().moveTo(repositionPos.x, repositionPos.y, repositionPos.z, CHARGE_SPEED);
        mountedPathRecalcTimer = MOUNTED_PATH_RECALC_DELAY * 3; // Give time to reposition
    }

    /**
     * Handles melee combat on foot (original behavior).
     */
    private void tickOnFoot() {
        super.tick();

        // Manage sprinting based on distance to target
        LivingEntity target = companion.getTarget();
        if (target != null && target.isAlive()) {
            double distanceSq = companion.distanceToSqr(target);

            if (!companion.isSprinting() && distanceSq > SPRINT_START_DISTANCE_SQ) {
                // Start sprinting when target is far
                companion.startSprinting();
            } else if (companion.isSprinting() && distanceSq < SPRINT_STOP_DISTANCE_SQ) {
                // Stop sprinting when in melee range
                companion.stopSprinting();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        companion.setAggressive(false);
        if (companion.isSprinting()) {
            companion.stopSprinting();
        }
        // Stop horse navigation if we were controlling it
        AbstractHorse horse = companion.getMountedHorse();
        if (horse != null) {
            horse.getNavigation().stop();
        }
        // Reset mounted combat state
        mountedCombatPhase = MountedCombatPhase.CHARGING;
    }

    /**
     * Override to manage shield blocking when attacking.
     * Stops blocking and sets a brief cooldown to prevent immediate re-blocking.
     */
    @Override
    protected void checkAndPerformAttack(LivingEntity enemy) {
        if (this.canPerformAttack(enemy)) {
            this.resetAttackCooldown();

            // Manage shield if companion has one
            if (companion.hasShield()) {
                // Stop blocking before attacking
                companion.stopUsingItem();
                // Brief cooldown prevents immediate re-blocking (8 ticks = 0.4 sec)
                if (companion.getShieldCoolDown() == 0) {
                    companion.setShieldCoolDown(8);
                }
            }

            companion.swing(InteractionHand.MAIN_HAND);
            companion.doHurtTarget(enemy);
        }
    }
}
