package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

/**
 * Wrapper for HurtByTargetGoal that checks if the companion can retaliate.
 * Ignores attacks from the owner and same-team companions.
 */
public class CompanionHurtByTargetGoal extends HurtByTargetGoal {

    private final CompanionEntity companion;

    public CompanionHurtByTargetGoal(CompanionEntity companion) {
        super(companion);
        this.companion = companion;
        this.setAlertOthers();
    }

    @Override
    public boolean canUse() {
        // Check if companion can retaliate (not passive, not combat disabled)
        if (!companion.canRetaliate()) {
            return false;
        }

        if (!super.canUse()) {
            return false;
        }

        LivingEntity attacker = this.mob.getLastHurtByMob();
        if (attacker == null) {
            return false;
        }

        // Don't retaliate against owner
        if (attacker.equals(companion.getOwner())) {
            return false;
        }

        // Don't retaliate against same-team companions
        if (companion.isOnSameTeam(attacker)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!companion.canRetaliate()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
