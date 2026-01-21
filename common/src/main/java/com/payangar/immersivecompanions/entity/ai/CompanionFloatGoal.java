package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import net.minecraft.world.entity.ai.goal.FloatGoal;

/**
 * Float goal wrapper that respects companion conditions.
 *
 * <p>This goal prevents swimming when the companion has conditions that block
 * the SWIM action (e.g., critical injury). An injured companion will not swim
 * because they can't perform the action, which means the SWIMMING pose never
 * happens - the correct behavior without needing to fight over poses.
 */
public class CompanionFloatGoal extends FloatGoal {

    private final CompanionEntity companion;

    public CompanionFloatGoal(CompanionEntity companion) {
        super(companion);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        // Check if swimming is blocked by any condition
        if (!companion.canPerformAction(ActionType.SWIM)) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop swimming if a condition starts blocking it
        if (!companion.canPerformAction(ActionType.SWIM)) {
            return false;
        }
        return super.canContinueToUse();
    }
}
