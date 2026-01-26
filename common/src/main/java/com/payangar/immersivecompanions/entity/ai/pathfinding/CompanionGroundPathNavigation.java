package com.payangar.immersivecompanions.entity.ai.pathfinding;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Custom ground navigation that uses CompanionWalkNodeEvaluator.
 * This enables condition-aware pathfinding where jump restrictions
 * are respected during path calculation.
 */
public class CompanionGroundPathNavigation extends GroundPathNavigation {

    public CompanionGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new CompanionWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }
}
