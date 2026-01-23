package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Wrapper for MeleeAttackGoal that only activates for melee companions
 * when combat is not disabled.
 */
public class CompanionMeleeAttackGoal extends MeleeAttackGoal {

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
    public void stop() {
        super.stop();
        companion.setAggressive(false);
    }
}
