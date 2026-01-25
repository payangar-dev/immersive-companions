package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Wrapper for MeleeAttackGoal that only activates for melee companions
 * when combat is not disabled.
 */
public class CompanionMeleeAttackGoal extends MeleeAttackGoal {

    /** Squared distance to start sprinting toward target (6 blocks) */
    private static final double SPRINT_START_DISTANCE_SQ = 36.0;
    /** Squared distance to stop sprinting (3 blocks, in melee range) */
    private static final double SPRINT_STOP_DISTANCE_SQ = 9.0;

    private final CompanionEntity companion;

    public CompanionMeleeAttackGoal(CompanionEntity companion, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(companion, speedModifier, followingTargetEvenIfNotSeen);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        // Only melee companions use this goal
        if (companion.getCombatType().isRanged()) {
            return false;
        }
        // Don't attack when combat is disabled (e.g., critical injury, passive stance)
        if (companion.isCombatDisabled()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if switched to ranged or combat disabled
        if (companion.getCombatType().isRanged()) {
            return false;
        }
        if (companion.isCombatDisabled()) {
            return false;
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        companion.setAggressive(true);
    }

    @Override
    public void tick() {
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
    }
}
