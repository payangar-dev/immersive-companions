package com.payangar.immersivecompanions.entity.mode;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;

import java.util.List;

/**
 * Default mode for unbought companions.
 * Companions in this mode wander around their village area.
 */
public final class WanderMode implements CompanionMode {

    public static final WanderMode INSTANCE = new WanderMode();

    private WanderMode() {}

    @Override
    public String getId() {
        return "wander";
    }

    @Override
    public List<GoalEntry> getGoals() {
        return List.of(
                new GoalEntry(6, c -> new WaterAvoidingRandomStrollGoal(c, 0.9))
        );
    }
}
