package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * High-priority AI goal that makes companions stop moving and look at the player
 * while the recruitment screen is open.
 *
 * Uses both MOVE and LOOK flags to prevent other goals from controlling movement
 * or where the companion looks during the interaction.
 */
public class CompanionInteractionGoal extends Goal {

    private final CompanionEntity companion;

    public CompanionInteractionGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return companion.isBeingInteractedWith();
    }

    @Override
    public boolean canContinueToUse() {
        return companion.isBeingInteractedWith();
    }

    @Override
    public void start() {
        // Stop any current navigation
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Stop navigation every tick to prevent other goals from moving us
        companion.getNavigation().stop();

        // Look at the interacting player
        Player player = companion.getInteractingPlayer();
        if (player != null) {
            companion.getLookControl().setLookAt(
                    player.getX(),
                    player.getEyeY(),
                    player.getZ(),
                    10.0F,  // Max head rotation per tick (horizontal)
                    (float) companion.getMaxHeadXRot()  // Max head rotation per tick (vertical)
            );
        }
    }

    @Override
    public void stop() {
        // Nothing special needed on stop - other goals will take over naturally
    }
}
