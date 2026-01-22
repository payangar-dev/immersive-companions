package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * AI goal that makes companions mimic their owner's sneaking state.
 * Only active when the companion is in FOLLOW mode.
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
        // Only mimic when in FOLLOW mode
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        return companion.hasOwner();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop mimicking if mode changed
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        return companion.hasOwner();
    }

    @Override
    public void tick() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return;
        }

        boolean ownerSneaking = owner.isCrouching();

        if (ownerSneaking && !companion.isCrouching()) {
            companion.startSneaking();
            wasSneaking = true;
        } else if (!ownerSneaking && companion.isCrouching()) {
            companion.stopSneaking();
            wasSneaking = false;
        }
    }

    @Override
    public void stop() {
        // Reset sneaking state when goal stops
        if (wasSneaking) {
            companion.stopSneaking();
            wasSneaking = false;
        }
    }
}
