package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * AI goal that makes companions flee from their most recent attacker.
 * Used by the Passive combat stance.
 *
 * <p>The companion will flee for a configurable duration after being attacked,
 * then stop fleeing if no new attacks occur.
 */
public class CompanionFleeFromAttackerGoal extends Goal {

    /** How long to flee after being attacked (in ticks, 100 = 5 seconds) */
    private static final int FLEE_DURATION = 100;

    /** How far to try to flee */
    private static final double FLEE_DISTANCE = 16.0;

    /** Speed multiplier when fleeing */
    private static final double FLEE_SPEED = 1.2;

    /** How often to recalculate path when fleeing */
    private static final int PATH_RECALC_DELAY = 10;

    private final CompanionEntity companion;
    @Nullable
    private LivingEntity attacker;
    private int pathRecalcTimer;

    public CompanionFleeFromAttackerGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity lastAttacker = companion.getLastHurtByMob();
        if (lastAttacker == null || !lastAttacker.isAlive()) {
            return false;
        }

        // Check if the attack was recent
        int timeSinceHurt = companion.tickCount - companion.getLastHurtByMobTimestamp();
        if (timeSinceHurt > FLEE_DURATION) {
            return false;
        }

        // Don't flee from same-team companions (shouldn't be attacked by them, but safety check)
        if (companion.isOnSameTeam(lastAttacker)) {
            return false;
        }

        this.attacker = lastAttacker;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (attacker == null || !attacker.isAlive()) {
            return false;
        }

        // Check if the attack was recent
        int timeSinceHurt = companion.tickCount - companion.getLastHurtByMobTimestamp();
        return timeSinceHurt <= FLEE_DURATION;
    }

    @Override
    public void start() {
        this.pathRecalcTimer = 0;
        fleeFromAttacker();
    }

    @Override
    public void tick() {
        if (--pathRecalcTimer <= 0) {
            pathRecalcTimer = PATH_RECALC_DELAY;
            fleeFromAttacker();
        }
    }

    @Override
    public void stop() {
        this.attacker = null;
        companion.getNavigation().stop();
    }

    /**
     * Calculates and sets a path away from the attacker.
     */
    private void fleeFromAttacker() {
        if (attacker == null) {
            return;
        }

        Vec3 fleePos = DefaultRandomPos.getPosAway(
                (PathfinderMob) companion,
                (int) FLEE_DISTANCE,
                7,
                attacker.position()
        );

        if (fleePos != null) {
            companion.getNavigation().moveTo(fleePos.x, fleePos.y, fleePos.z, FLEE_SPEED);
        }
    }
}
