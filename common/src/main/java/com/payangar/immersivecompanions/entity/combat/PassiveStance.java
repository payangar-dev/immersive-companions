package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionFleeFromAttackerGoal;
import com.payangar.immersivecompanions.entity.mode.GoalEntry;

import java.util.List;

/**
 * Passive combat stance: never attacks, flees when threatened.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Does not retaliate when attacked</li>
 *   <li>Does not defend teammates</li>
 *   <li>Does not assist teammates or owner</li>
 *   <li>Does not proactively attack</li>
 *   <li>Flees from attackers</li>
 * </ul>
 */
public final class PassiveStance implements CombatStance {

    public static final PassiveStance INSTANCE = new PassiveStance();

    private PassiveStance() {}

    @Override
    public String getId() {
        return "passive";
    }

    @Override
    public List<TargetGoalEntry> getTargetGoals() {
        // No target goals - passive companions never attack
        return List.of();
    }

    @Override
    public List<GoalEntry> getBehaviorGoals() {
        return List.of(
            // High priority flee behavior when attacked
            new GoalEntry(1, CompanionFleeFromAttackerGoal::new)
        );
    }

    @Override
    public void onEnter(CompanionEntity companion) {
        // Clear any existing target when entering passive stance
        companion.setTarget(null);
    }
}
