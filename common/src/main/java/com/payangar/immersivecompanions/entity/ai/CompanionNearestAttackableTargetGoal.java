package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;

/**
 * Wrapper for NearestAttackableTargetGoal that only activates in AGGRESSIVE stance.
 * Proactively targets monsters while filtering out dangerous ones.
 */
public class CompanionNearestAttackableTargetGoal extends NearestAttackableTargetGoal<Monster> {

    private final CompanionEntity companion;

    public CompanionNearestAttackableTargetGoal(CompanionEntity companion) {
        super(companion, Monster.class, 10, true, false, target -> shouldAttackEntity(companion, target));
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        // Only proactively attack in AGGRESSIVE stance
        if (!companion.canProactivelyAttack()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!companion.canProactivelyAttack()) {
            return false;
        }
        return super.canContinueToUse();
    }

    /**
     * Determines if the companion should attack a given entity.
     * Filters out dangerous targets like Creepers and Endermen.
     */
    private static boolean shouldAttackEntity(CompanionEntity companion, LivingEntity entity) {
        // Don't attack Creepers (explode) or Endermen (teleport, aggro)
        if (entity instanceof Creeper || entity instanceof EnderMan) {
            return false;
        }
        return entity instanceof Monster;
    }
}
