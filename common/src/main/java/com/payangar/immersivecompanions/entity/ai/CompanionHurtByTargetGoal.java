package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

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

        // If our mount was attacked, treat it as if we were attacked
        propagateMountHurt();

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

        // Don't retaliate against tamed animals (horses, wolves, cats, etc.)
        if (CompanionEntity.isTamedAnimal(attacker)) {
            return false;
        }

        return true;
    }

    /**
     * If the companion is riding a horse and that horse was recently attacked,
     * propagate the attacker to the companion so vanilla HurtByTargetGoal picks it up.
     */
    private void propagateMountHurt() {
        AbstractHorse mount = companion.getMountedHorse();
        if (mount == null) return;

        LivingEntity mountAttacker = mount.getLastHurtByMob();
        if (mountAttacker == null || !mountAttacker.isAlive()) return;

        // Only propagate recent attacks (5 seconds)
        int mountHurtAge = mount.tickCount - mount.getLastHurtByMobTimestamp();
        if (mountHurtAge > 100) return;

        // Don't overwrite if companion already has a more recent attacker
        LivingEntity ownAttacker = companion.getLastHurtByMob();
        if (ownAttacker != null && ownAttacker.isAlive()) {
            int ownHurtAge = companion.tickCount - companion.getLastHurtByMobTimestamp();
            if (ownHurtAge <= mountHurtAge) return;
        }

        companion.setLastHurtByMob(mountAttacker);
    }

    @Override
    public boolean canContinueToUse() {
        if (!companion.canRetaliate()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
