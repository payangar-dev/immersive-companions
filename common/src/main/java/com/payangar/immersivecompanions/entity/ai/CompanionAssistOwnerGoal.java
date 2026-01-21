package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * AI goal that makes companions assist their owner by targeting whatever the owner attacks.
 * Uses the owner's lastHurtMob to determine targets.
 */
public class CompanionAssistOwnerGoal extends TargetGoal {

    /** How often to check for owner's target (in ticks) */
    private static final int CHECK_INTERVAL = 20;

    /** How recent the owner's attack must be to assist (in ticks, 100 = 5 seconds) */
    private static final int RECENT_ATTACK_THRESHOLD = 100;

    private final CompanionEntity companion;
    private final TargetingConditions targetConditions;

    @Nullable
    private LivingEntity ownerTarget;
    private int checkTimer;

    public CompanionAssistOwnerGoal(CompanionEntity companion) {
        super(companion, false, false);
        this.companion = companion;
        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        // Only owned companions can assist owner
        if (!companion.hasOwner()) {
            return false;
        }

        if (--checkTimer > 0) {
            return false;
        }
        checkTimer = CHECK_INTERVAL;

        // Don't assist when critically injured
        if (ModConfig.get().isEnableCriticalInjury() && companion.isCriticallyInjured()) {
            return false;
        }

        ownerTarget = findOwnerTarget();
        return ownerTarget != null;
    }

    @Override
    public void start() {
        companion.setTarget(ownerTarget);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Don't continue if critically injured
        if (ModConfig.get().isEnableCriticalInjury() && companion.isCriticallyInjured()) {
            return false;
        }

        // Check if still in range
        double range = ModConfig.get().getTeamCoordinationRange();
        return companion.distanceToSqr(target) < range * range;
    }

    @Override
    public void stop() {
        ownerTarget = null;
        companion.setTarget(null);
        super.stop();
    }

    /**
     * Finds the entity the owner recently attacked.
     *
     * @return The owner's target, or null if none found
     */
    @Nullable
    private LivingEntity findOwnerTarget() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return null;
        }

        LivingEntity ownerLastHurt = owner.getLastHurtMob();
        if (ownerLastHurt == null || !ownerLastHurt.isAlive()) {
            return null;
        }

        // Check if the attack was recent
        int timeSinceAttack = owner.tickCount - owner.getLastHurtMobTimestamp();
        if (timeSinceAttack > RECENT_ATTACK_THRESHOLD) {
            return null;
        }

        // Don't target same-team companions
        if (companion.isOnSameTeam(ownerLastHurt)) {
            return null;
        }

        // Don't target the owner themselves (shouldn't happen, but safety check)
        if (ownerLastHurt == owner) {
            return null;
        }

        // Don't target the companion itself
        if (ownerLastHurt == companion) {
            return null;
        }

        // Verify we can actually target this entity
        if (!targetConditions.test(companion, ownerLastHurt)) {
            return null;
        }

        // Check if target is within range
        double range = ModConfig.get().getTeamCoordinationRange();
        if (companion.distanceToSqr(ownerLastHurt) > range * range) {
            return null;
        }

        return ownerLastHurt;
    }
}
