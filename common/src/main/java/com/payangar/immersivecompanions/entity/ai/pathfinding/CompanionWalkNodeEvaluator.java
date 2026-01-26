package com.payangar.immersivecompanions.entity.ai.pathfinding;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom node evaluator that respects condition-based movement restrictions.
 * Handles:
 * - Jump restrictions when companion cannot jump (e.g., CriticalInjuryCondition blocks JUMP)
 * - Fall safety to prevent companions from pathfinding off dangerous cliffs
 */
public class CompanionWalkNodeEvaluator extends WalkNodeEvaluator {

    /** Maximum safe fall distance (3 blocks = no damage threshold in Minecraft) */
    private static final int MAX_SAFE_FALL_DISTANCE = 3;
    /** Stricter fall limit for critically injured companions */
    private static final int INJURED_MAX_FALL_DISTANCE = 1;

    @Override
    protected Node findAcceptedNode(int x, int y, int z, int stepRange, double floorLevel, Direction direction, PathType pathType) {
        Node node = super.findAcceptedNode(x, y, z, stepRange, floorLevel, direction, pathType);

        // If no valid node was found, nothing to filter
        if (node == null) {
            return null;
        }

        // Check if this is a companion with blocked jumping
        if (this.mob instanceof CompanionEntity companion) {
            double currentY = this.mob.getY();

            // Jump restriction check
            if (!companion.canPerformAction(ActionType.JUMP)) {
                // If the node requires going up more than step height allows,
                // it would require a jump - reject it
                float stepHeight = this.mob.maxUpStep();
                if (node.y > currentY + stepHeight) {
                    return null;
                }
            }

            // Fall safety check - allow falling into water (water negates fall damage)
            if (pathType == PathType.WATER) {
                return node;
            }

            // Calculate fall distance (positive = going down)
            double fallDistance = currentY - node.y;

            if (fallDistance > 0) {
                int maxFallDistance = companion.isCriticallyInjured()
                    ? INJURED_MAX_FALL_DISTANCE
                    : MAX_SAFE_FALL_DISTANCE;

                if (fallDistance > maxFallDistance) {
                    return null; // Reject dangerous drops
                }
            }
        }

        return node;
    }
}
