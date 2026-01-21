package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * AI goal that makes companions defend same-team companions under attack.
 * Unlike CompanionTeamCoordinationGoal, this only defends - it does not assist
 * teammates in attacking their targets.
 *
 * <p>Used by the Defensive combat stance.
 */
public class CompanionDefendTeammatesGoal extends TargetGoal {

    /** How often to check for teammates in danger (in ticks) */
    private static final int CHECK_INTERVAL = 20;

    /** How recent an attack must be to respond (in ticks, 100 = 5 seconds) */
    private static final int RECENT_HURT_THRESHOLD = 100;

    private final CompanionEntity companion;
    private final TargetingConditions targetConditions;

    @Nullable
    private LivingEntity attackerTarget;
    private int checkTimer;

    public CompanionDefendTeammatesGoal(CompanionEntity companion) {
        super(companion, false, false);
        this.companion = companion;
        this.targetConditions = TargetingConditions.forCombat()
                .ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        if (!ModConfig.get().isEnableTeamCoordination()) {
            return false;
        }

        if (--checkTimer > 0) {
            return false;
        }
        checkTimer = CHECK_INTERVAL;

        // Don't defend when combat is disabled by conditions
        if (companion.isCombatDisabled()) {
            return false;
        }

        attackerTarget = findDefendTarget();
        return attackerTarget != null;
    }

    @Override
    public void start() {
        companion.setTarget(attackerTarget);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        // Don't continue if combat is disabled by conditions
        if (companion.isCombatDisabled()) {
            return false;
        }

        double range = ModConfig.get().getTeamCoordinationRange();
        return companion.distanceToSqr(target) < range * range;
    }

    @Override
    public void stop() {
        attackerTarget = null;
        companion.setTarget(null);
        super.stop();
    }

    /**
     * Finds an entity that is attacking a same-team companion.
     *
     * @return The attacker to target, or null if none found
     */
    @Nullable
    private LivingEntity findDefendTarget() {
        double range = ModConfig.get().getTeamCoordinationRange();
        AABB searchBox = companion.getBoundingBox().inflate(range);
        List<CompanionEntity> teammates = companion.level().getEntitiesOfClass(
                CompanionEntity.class, searchBox,
                this::isValidTeammate
        );

        for (CompanionEntity teammate : teammates) {
            LivingEntity attacker = teammate.getLastHurtByMob();
            if (attacker == null || !attacker.isAlive()) {
                continue;
            }

            // Check if the attack was recent
            int timeSinceHurt = teammate.tickCount - teammate.getLastHurtByMobTimestamp();
            if (timeSinceHurt > RECENT_HURT_THRESHOLD) {
                continue;
            }

            // Don't target same-team companions
            if (companion.isOnSameTeam(attacker)) {
                continue;
            }

            // Don't target owner
            if (attacker.equals(companion.getOwner())) {
                continue;
            }

            // Verify we can actually target this entity
            if (!targetConditions.test(companion, attacker)) {
                continue;
            }

            return attacker;
        }

        return null;
    }

    /**
     * Checks if an entity is a valid teammate for coordination.
     */
    private boolean isValidTeammate(CompanionEntity other) {
        // Must be a different companion
        if (other == companion) {
            return false;
        }

        // Must be on the same team
        return companion.isOnSameTeam(other);
    }
}
