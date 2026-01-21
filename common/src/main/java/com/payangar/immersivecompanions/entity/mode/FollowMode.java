package com.payangar.immersivecompanions.entity.mode;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionFollowOwnerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionMimicOwnerGoal;

import java.util.List;

/**
 * Mode for owned companions that follow their owner.
 * Activated after a player purchases the companion.
 */
public final class FollowMode implements CompanionMode {

    public static final FollowMode INSTANCE = new FollowMode();

    private FollowMode() {}

    @Override
    public String getId() {
        return "follow";
    }

    @Override
    public List<GoalEntry> getGoals() {
        return List.of(
                new GoalEntry(4, CompanionMimicOwnerGoal::new),
                new GoalEntry(6, c -> new CompanionFollowOwnerGoal(c, 1.0))
        );
    }

    @Override
    public boolean canTransitionTo(CompanionMode target, CompanionEntity companion) {
        // All transitions allowed from follow mode
        return true;
    }
}
