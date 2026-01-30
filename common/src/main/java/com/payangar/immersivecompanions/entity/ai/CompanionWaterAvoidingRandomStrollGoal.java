package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Wander goal for companions that avoids water and ignores noActionTime checks.
 *
 * Extends RandomStrollGoal directly (with checkNoActionTime=false) instead of
 * WaterAvoidingRandomStrollGoal because the latter doesn't expose that parameter.
 * This fixes a bug where companions would stop wandering after ~5 seconds of
 * inactivity due to Minecraft's noActionTime optimization for distant mobs.
 */
public class CompanionWaterAvoidingRandomStrollGoal extends RandomStrollGoal {

    protected static final float PROBABILITY = 0.001F;

    private final CompanionEntity companion;
    protected final float probability;

    public CompanionWaterAvoidingRandomStrollGoal(CompanionEntity companion, double speedModifier) {
        this(companion, speedModifier, PROBABILITY);
    }

    public CompanionWaterAvoidingRandomStrollGoal(CompanionEntity companion, double speedModifier, float probability) {
        // checkNoActionTime=false prevents the goal from being blocked when
        // the companion hasn't taken any "action" for 100+ ticks
        super(companion, speedModifier, 120, false);
        this.companion = companion;
        this.probability = probability;
    }

    @Override
    public boolean canUse() {
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }
        return super.canContinueToUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // If in water, try to find a land position first
        if (this.mob.isInWater()) {
            Vec3 landPos = LandRandomPos.getPos(this.mob, 15, 7);
            return landPos != null ? landPos : super.getPosition();
        }

        // Random chance to use water-avoiding position
        if (this.mob.getRandom().nextFloat() >= this.probability) {
            Vec3 waterAvoidPos = LandRandomPos.getPos(this.mob, 10, 7);
            return waterAvoidPos != null ? waterAvoidPos : super.getPosition();
        }

        return super.getPosition();
    }
}
