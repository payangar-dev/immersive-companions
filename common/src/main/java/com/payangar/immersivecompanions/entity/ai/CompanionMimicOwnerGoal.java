package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * AI goal that makes companions mimic their owner's sneaking state.
 * Runs continuously while the companion has an owner.
 */
public class CompanionMimicOwnerGoal extends Goal {

    private final CompanionEntity companion;
    private boolean wasSneaking = false;

    public CompanionMimicOwnerGoal(CompanionEntity companion) {
        this.companion = companion;
        // No flags - doesn't interfere with movement or look
    }

    @Override
    public boolean canUse() {
        return companion.hasOwner();
    }

    @Override
    public boolean canContinueToUse() {
        return companion.hasOwner();
    }

    @Override
    public void tick() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return;
        }

        boolean ownerSneaking = owner.isCrouching();

        if (ownerSneaking && !wasSneaking) {
            // Owner started sneaking
            companion.setShiftKeyDown(true);
            companion.setPose(Pose.CROUCHING);
            wasSneaking = true;
        } else if (!ownerSneaking && wasSneaking) {
            // Owner stopped sneaking
            companion.setShiftKeyDown(false);
            companion.setPose(Pose.STANDING);
            wasSneaking = false;
        }
    }

    @Override
    public void stop() {
        // Reset sneaking state when goal stops
        if (wasSneaking) {
            companion.setShiftKeyDown(false);
            companion.setPose(Pose.STANDING);
            wasSneaking = false;
        }
    }
}
