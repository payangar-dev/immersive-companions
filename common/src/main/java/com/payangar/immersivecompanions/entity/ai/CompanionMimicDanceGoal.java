package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.platform.Services;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * AI goal that makes companions mirror their owner's hopak dance animation.
 * Only active in FOLLOW mode and when Epic Fight mod is loaded.
 * Triggers when the owner starts performing the hopak emote.
 */
public class CompanionMimicDanceGoal extends Goal {

    private final CompanionEntity companion;

    /** Cached owner reference */
    private Player owner;

    public CompanionMimicDanceGoal(CompanionEntity companion) {
        this.companion = companion;
        // Only MOVE flag - allow looking at owner while dancing
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only in FOLLOW mode
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }

        // Must have an owner
        this.owner = companion.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Only works when Epic Fight is loaded and owner is dancing hopak
        return Services.get().isPlayerDancingHopak(owner);
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if mode changed
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }

        // Stop if owner is gone
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        // Continue while owner is still dancing
        return Services.get().isPlayerDancingHopak(owner);
    }

    @Override
    public void start() {
        companion.setDancing(true);
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }

        // Look at owner while dancing
        companion.getLookControl().setLookAt(
                owner.getX(),
                owner.getEyeY(),
                owner.getZ(),
                10.0F,
                (float) companion.getMaxHeadXRot()
        );
    }

    @Override
    public void stop() {
        companion.setDancing(false);
        this.owner = null;
    }
}
