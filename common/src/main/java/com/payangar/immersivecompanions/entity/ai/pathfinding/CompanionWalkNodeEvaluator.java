package com.payangar.immersivecompanions.entity.ai.pathfinding;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom node evaluator that respects condition-based jump restrictions.
 * When a companion cannot jump (e.g., CriticalInjuryCondition blocks JUMP),
 * this evaluator rejects nodes that would require jumping to reach.
 */
public class CompanionWalkNodeEvaluator extends WalkNodeEvaluator {

    @Override
    protected Node findAcceptedNode(int x, int y, int z, int stepRange, double floorLevel, Direction direction, PathType pathType) {
        Node node = super.findAcceptedNode(x, y, z, stepRange, floorLevel, direction, pathType);

        // If no valid node was found, nothing to filter
        if (node == null) {
            return null;
        }

        // Check if this is a companion with blocked jumping
        if (this.mob instanceof CompanionEntity companion) {
            if (!companion.canPerformAction(ActionType.JUMP)) {
                // Get the current Y position of the mob
                double currentY = this.mob.getY();

                // If the node requires going up more than step height allows,
                // it would require a jump - reject it
                float stepHeight = this.mob.maxUpStep();
                if (node.y > currentY + stepHeight) {
                    return null;
                }
            }
        }

        return node;
    }
}
