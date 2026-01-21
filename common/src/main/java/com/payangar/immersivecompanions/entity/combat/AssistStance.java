package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionAssistOwnerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionTeamCoordinationGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;

import java.util.List;

/**
 * Assist combat stance: retaliates, defends, and assists team in combat.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Retaliates when attacked</li>
 *   <li>Defends teammates under attack</li>
 *   <li>Assists teammates in attacking their targets</li>
 *   <li>Assists owner by attacking what the owner attacks</li>
 *   <li>Does not proactively attack</li>
 * </ul>
 */
public final class AssistStance implements CombatStance {

    public static final AssistStance INSTANCE = new AssistStance();

    private AssistStance() {}

    @Override
    public String getId() {
        return "assist";
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
            // Priority 3: Defend and assist teammates
            new TargetGoalEntry(3, CompanionTeamCoordinationGoal::new),
            // Priority 4: Assist owner
            new TargetGoalEntry(4, CompanionAssistOwnerGoal::new)
        );
    }
}
