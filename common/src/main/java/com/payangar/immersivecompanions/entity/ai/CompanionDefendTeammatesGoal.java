package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * AI goal that makes companions defend same-team companions under attack.
 * Unlike CompanionTeamCoordinationGoal, this only defends - it does not assist
 * teammates in attacking their targets.
 *
 * <p>Active in DEFENSIVE, ASSIST, and AGGRESSIVE stances.
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
        // Check if companion can defend teammates (not passive, not combat disabled)
        if (!companion.canDefendTeammates()) {
            return false;
        }

        if (!ModConfig.get().isEnableTeamCoordination()) {
            return false;
        }

        if (--checkTimer > 0) {
            return false;
        }
        checkTimer = CHECK_INTERVAL;

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
        // Stop defending if stance changed
        if (!companion.canDefendTeammates()) {
            return false;
        }

        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
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
     * Finds an entity that is attacking a same-team companion, their mount, or the owner's mount.
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
            LivingEntity attacker = getRecentAttacker(teammate);
            if (attacker != null) return attacker;

            // Also check teammate's mount
            AbstractHorse mount = teammate.getMountedHorse();
            if (mount != null) {
                attacker = getRecentAttacker(mount);
                if (attacker != null) return attacker;
            }
        }

        // Check owner's mount
        Player owner = companion.getOwner();
        if (owner != null && owner.getVehicle() instanceof AbstractHorse ownerHorse) {
            LivingEntity attacker = getRecentAttacker(ownerHorse);
            if (attacker != null) return attacker;
        }

        return null;
    }

    /**
     * Returns the recent attacker of the given entity, if valid (alive, recent,
     * not the owner, not a same-team companion, not a tamed animal, and targetable).
     */
    @Nullable
    private LivingEntity getRecentAttacker(LivingEntity entity) {
        LivingEntity attacker = entity.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) return null;

        int timeSinceHurt = entity.tickCount - entity.getLastHurtByMobTimestamp();
        if (timeSinceHurt > RECENT_HURT_THRESHOLD) return null;

        if (companion.isOnSameTeam(attacker)) return null;
        if (attacker.equals(companion.getOwner())) return null;
        if (CompanionEntity.isTamedAnimal(attacker)) return null;
        if (!targetConditions.test(companion, attacker)) return null;

        return attacker;
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
