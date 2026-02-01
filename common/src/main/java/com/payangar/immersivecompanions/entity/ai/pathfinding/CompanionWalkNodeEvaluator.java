package com.payangar.immersivecompanions.entity.ai.pathfinding;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;  // Used by findAcceptedNode signature
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Custom node evaluator that respects condition-based movement restrictions.
 * Handles:
 * - Jump restrictions when companion cannot jump (e.g., CriticalInjuryCondition blocks JUMP)
 * - Fall safety to prevent companions from pathfinding off dangerous cliffs
 * - Hazardous block avoidance (stalactites, deep snow)
 *
 * Gap jumping is handled separately by CompanionGapJumpGoal, which
 * detects gaps at runtime and executes jumps independently of pathfinding.
 */
public class CompanionWalkNodeEvaluator extends WalkNodeEvaluator {

    /** Maximum safe fall distance (3 blocks = no damage threshold in Minecraft) */
    private static final int MAX_SAFE_FALL_DISTANCE = 3;
    /** Stricter fall limit for critically injured companions */
    private static final int INJURED_MAX_FALL_DISTANCE = 1;

    @Override
    protected Node findAcceptedNode(int x, int y, int z, int stepRange, double floorLevel, Direction direction, PathType pathType) {
        Node node = super.findAcceptedNode(x, y, z, stepRange, floorLevel, direction, pathType);

        // Apply companion-specific filters to valid nodes
        if (node != null) {
            return filterNode(node, pathType);
        }

        return null;
    }

    /**
     * Applies companion-specific filters to a node.
     * Returns null if the node should be rejected.
     */
    private Node filterNode(Node node, PathType pathType) {
        if (!(this.mob instanceof CompanionEntity companion)) {
            return node;
        }

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

        // Hazardous block check - avoid stalactites and deep snow
        BlockPos nodePos = new BlockPos(node.x, node.y, node.z);
        if (isHazardousBlock(nodePos)) {
            return null;
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

        return node;
    }

    /**
     * Checks if a position contains blocks that would trap or harm the companion.
     * Stalactites/stalagmites block movement, and deep snow layers trap entities.
     */
    private boolean isHazardousBlock(BlockPos pos) {
        BlockState state = this.currentContext.level().getBlockState(pos);

        // Pointed dripstone (stalactites/stalagmites) obstructs movement
        if (state.is(Blocks.POINTED_DRIPSTONE)) {
            return true;
        }

        // Stacked snow (2+ layers) can trap entities
        if (state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) >= 2) {
            return true;
        }

        // Check block above for head-level hazards
        BlockState above = this.currentContext.level().getBlockState(pos.above());
        if (above.is(Blocks.POINTED_DRIPSTONE)) {
            return true;
        }

        return false;
    }
}
