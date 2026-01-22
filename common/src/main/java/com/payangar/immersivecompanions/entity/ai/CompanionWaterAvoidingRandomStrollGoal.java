package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

/**
 * Wrapper for WaterAvoidingRandomStrollGoal that only activates in WANDER mode.
 * This replaces dynamic goal registration with a canUse() check.
 */
public class CompanionWaterAvoidingRandomStrollGoal extends WaterAvoidingRandomStrollGoal {

    private final CompanionEntity companion;

    public CompanionWaterAvoidingRandomStrollGoal(CompanionEntity companion, double speedModifier) {
        super(companion, speedModifier);
        this.companion = companion;
    }

    @Override
    public boolean canUse() {
        // Only wander when in WANDER mode
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop wandering if mode changed
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }
        return super.canContinueToUse();
    }
}
