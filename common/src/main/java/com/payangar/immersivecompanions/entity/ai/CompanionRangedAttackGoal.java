package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.CrossbowItem;

import java.util.EnumSet;

/**
 * Custom ranged attack goal that includes a charging phase for bow/crossbow animations.
 * This allows the companion to display proper arm poses while charging their weapon.
 */
public class CompanionRangedAttackGoal extends Goal {

    private final CompanionEntity companion;
    private final double speedModifier;
    private final int attackIntervalMin;
    private final float attackRadius;
    private final float attackRadiusSqr;

    private int attackTime = -1;
    private int chargeTime = 0;
    private boolean isCharging = false;
    private int seeTime;

    public CompanionRangedAttackGoal(CompanionEntity companion, double speedModifier, int attackInterval, float attackRadius) {
        this.companion = companion;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackInterval;
        this.attackRadius = attackRadius;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Gets the charge time based on the equipped weapon.
     * Crossbows take slightly longer to charge than bows.
     */
    private int getChargeTime() {
        return companion.getMainHandItem().getItem() instanceof CrossbowItem ? 25 : 20;
    }

    @Override
    public boolean canUse() {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return companion.canUseRangedWeapon();
    }

    @Override
    public boolean canContinueToUse() {
        return (canUse() || !companion.getNavigation().isDone()) && companion.canUseRangedWeapon();
    }

    @Override
    public void start() {
        super.start();
        companion.setAggressive(true);
    }

    @Override
    public void stop() {
        super.stop();
        companion.setAggressive(false);
        companion.setCharging(false);
        isCharging = false;
        chargeTime = 0;
        seeTime = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = companion.getTarget();
        if (target == null) {
            companion.setCharging(false);
            isCharging = false;
            return;
        }

        double distSq = companion.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = companion.getSensing().hasLineOfSight(target);

        // Track line of sight
        if (canSee) {
            seeTime++;
        } else {
            seeTime = 0;
        }

        // Move toward target if too far or can't see
        if (distSq > attackRadiusSqr || !canSee) {
            companion.getNavigation().moveTo(target, speedModifier);
            // Reset charging if we need to move
            if (isCharging) {
                companion.setCharging(false);
                isCharging = false;
                chargeTime = 0;
            }
            return;
        }

        // In range and can see - stop moving and aim
        companion.getNavigation().stop();
        companion.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Handle attack cooldown
        if (attackTime > 0) {
            attackTime--;
            return;
        }

        // Start or continue charging
        if (!isCharging) {
            isCharging = true;
            chargeTime = 0;
            companion.setCharging(true);
        }

        chargeTime++;

        // Check if charge is complete
        int requiredChargeTime = getChargeTime();
        if (chargeTime >= requiredChargeTime) {
            // Calculate power based on charge time (max 1.0)
            float power = Math.min((float) chargeTime / 20.0f, 1.0f);
            companion.performRangedAttack(target, power);

            // Reset state
            companion.setCharging(false);
            isCharging = false;
            chargeTime = 0;
            attackTime = attackIntervalMin;
        }
    }
}
