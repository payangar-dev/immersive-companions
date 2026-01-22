package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * AI goal that makes owned companions follow their owner.
 * Includes teleportation when the owner gets too far away.
 * Only active when the companion is in FOLLOW mode.
 */
public class CompanionFollowOwnerGoal extends Goal {

    private static final double STOP_DISTANCE = 2.0;
    private static final double START_DISTANCE = 5.0;
    private static final double TELEPORT_DISTANCE = 12.0;
    private static final int PATH_RECALC_DELAY = 10;

    private final CompanionEntity companion;
    private final double speedModifier;
    private final PathNavigation navigation;
    private final Level level;

    private LivingEntity owner;
    private int timeToRecalcPath;

    public CompanionFollowOwnerGoal(CompanionEntity companion, double speedModifier) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.navigation = companion.getNavigation();
        this.level = companion.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only follow when in FOLLOW mode
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        LivingEntity potentialOwner = getOwner();
        if (potentialOwner == null) {
            return false;
        }
        if (potentialOwner.isSpectator()) {
            return false;
        }
        double distance = companion.distanceToSqr(potentialOwner);
        if (distance < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        this.owner = potentialOwner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop following if mode changed
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        if (navigation.isDone()) {
            return false;
        }
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        double distance = companion.distanceToSqr(owner);
        return distance > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (owner == null) {
            return;
        }

        // Look at owner while following
        companion.getLookControl().setLookAt(owner, 10.0F, (float) companion.getMaxHeadXRot());

        double distance = companion.distanceToSqr(owner);

        // Teleport if too far away
        if (distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
            teleportToOwner();
            return;
        }

        // Recalculate path periodically
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = PATH_RECALC_DELAY;
            if (!companion.isLeashed() && !companion.isPassenger()) {
                navigation.moveTo(owner, speedModifier);
            }
        }
    }

    private void teleportToOwner() {
        BlockPos ownerPos = owner.blockPosition();

        // Try to find a safe teleport position nearby
        for (int attempt = 0; attempt < 10; attempt++) {
            int dx = randomOffset(3);
            int dz = randomOffset(3);
            BlockPos testPos = ownerPos.offset(dx, 0, dz);

            if (canTeleportTo(testPos)) {
                companion.moveTo(testPos.getX() + 0.5, testPos.getY(), testPos.getZ() + 0.5,
                        companion.getYRot(), companion.getXRot());
                navigation.stop();
                return;
            }
        }
    }

    private int randomOffset(int range) {
        return companion.getRandom().nextInt(range * 2 + 1) - range;
    }

    private boolean canTeleportTo(BlockPos pos) {
        BlockState groundState = level.getBlockState(pos.below());
        if (!groundState.isValidSpawn(level, pos.below(), companion.getType())) {
            return false;
        }

        // Check that the space is not blocked (needs 2 blocks of air above ground)
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        for (int y = 0; y < 2; y++) {
            BlockState state = level.getBlockState(mutablePos);
            if (state.blocksMotion()) {
                return false;
            }
            mutablePos.move(0, 1, 0);
        }

        return true;
    }

    private Player getOwner() {
        return companion.getOwner();
    }
}
