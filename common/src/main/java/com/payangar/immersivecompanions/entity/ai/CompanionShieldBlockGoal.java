package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AI goal for melee companions to proactively and reactively block with shields.
 *
 * Follows the guardvillagers pattern:
 * - Entity-level shieldCoolDown field decremented every tick in aiStep()
 * - When melee attacking: stopUsingItem() + set brief cooldown (8 ticks)
 * - Shield goal checks cooldown == 0 AND threat conditions to raise shield
 * - No fixed duration - blocks while conditions are met, interrupted by attacking
 *
 * Blocking triggers (any of these):
 * - Proactive: Enemy within 4 blocks AND approaching (closing distance)
 * - Reactive: Took damage recently (within ~1 second)
 *
 * Uses MOVE and LOOK flags to allow melee attack to interrupt blocking.
 */
public class CompanionShieldBlockGoal extends Goal {

    /** Distance within which we consider an enemy close enough to block proactively */
    private static final double BLOCK_RANGE = 4.0;
    private static final double BLOCK_RANGE_SQ = BLOCK_RANGE * BLOCK_RANGE;

    /** Ticks after taking damage during which reactive blocking is active */
    private static final int REACTIVE_BLOCK_TICKS = 20; // ~1 second

    private final CompanionEntity companion;

    /** Previous distance to target, for detecting approach */
    private double previousDistanceSq = Double.MAX_VALUE;

    public CompanionShieldBlockGoal(CompanionEntity companion) {
        this.companion = companion;
        // Control both movement and look to allow coordination with melee attack
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Check entity-level cooldown (set by melee attack)
        if (companion.getShieldCoolDown() > 0) {
            return false;
        }

        // Only companions without ranged weapons can use this goal
        if (companion.canUseRangedWeapon()) {
            return false;
        }
        if (!companion.hasShield()) {
            return false;
        }
        if (companion.isCombatDisabled()) {
            return false;
        }

        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        return shouldBlock(target);
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if cooldown was set (e.g., by melee attack)
        if (companion.getShieldCoolDown() > 0) {
            return false;
        }

        if (companion.canUseRangedWeapon()) {
            return false;
        }
        if (!companion.hasShield()) {
            return false;
        }
        if (companion.isCombatDisabled()) {
            return false;
        }

        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Continue blocking while threat conditions are met
        return shouldBlock(target);
    }

    /**
     * Determines if the companion should be blocking based on trigger conditions.
     */
    private boolean shouldBlock(LivingEntity target) {
        double distanceSq = companion.distanceToSqr(target);

        // Reactive: Recently took damage
        int lastHurtTime = companion.getLastHurtByMobTimestamp();
        int ticksSinceHurt = companion.tickCount - lastHurtTime;
        if (ticksSinceHurt < REACTIVE_BLOCK_TICKS && distanceSq <= BLOCK_RANGE_SQ) {
            return true;
        }

        // Proactive: Enemy within range AND approaching
        if (distanceSq <= BLOCK_RANGE_SQ) {
            boolean isApproaching = distanceSq < previousDistanceSq;
            return isApproaching;
        }

        return false;
    }

    @Override
    public void start() {
        companion.startUsingItem(InteractionHand.OFF_HAND);
        previousDistanceSq = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        LivingEntity target = companion.getTarget();
        if (target != null) {
            // Look at the target while blocking
            companion.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // Update distance tracking for approach detection
            double currentDistanceSq = companion.distanceToSqr(target);
            previousDistanceSq = currentDistanceSq;
        }
    }

    @Override
    public void stop() {
        companion.stopUsingItem();
        previousDistanceSq = Double.MAX_VALUE;
        // No cooldown here - let melee attack set it when attacking
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
