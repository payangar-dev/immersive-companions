package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionDefendTeammatesGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

import java.util.List;

/**
 * Defensive combat stance: retaliates and defends teammates.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Retaliates when attacked</li>
 *   <li>Defends teammates under attack</li>
 *   <li>Does NOT assist teammates or owner in attacking</li>
 *   <li>Does not proactively attack</li>
 * </ul>
 */
public final class DefensiveStance implements CombatStance {

    public static final DefensiveStance INSTANCE = new DefensiveStance();

    private DefensiveStance() {}

    @Override
    public String getId() {
        return "defensive";
    }

    @Override
    public List<TargetGoalEntry> getTargetGoals() {
        return List.of(
            // Priority 2: Retaliate when hurt (but not against owner or same-team companions)
            new TargetGoalEntry(2, c -> new HurtByTargetGoal(c) {
                @Override
                public boolean canUse() {
                    if (!super.canUse()) return false;
                    LivingEntity attacker = this.mob.getLastHurtByMob();
                    // Don't retaliate against owner
                    if (attacker != null && attacker.equals(c.getOwner())) {
                        return false;
                    }
                    return !c.isOnSameTeam(attacker);
                }
            }.setAlertOthers()),
            // Priority 3: Defend teammates under attack
            new TargetGoalEntry(3, CompanionDefendTeammatesGoal::new)
        );
    }
}
