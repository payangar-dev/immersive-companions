package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

/**
 * Epic Fight entity patch for CompanionEntity.
 * Extends HumanoidMobPatch to provide humanoid combat animations for companions.
 * This enables companions to use Epic Fight's biped animations for combat,
 * walking, idle, and other actions. Supports both melee and ranged combat.
 */
public class CompanionEntityPatch extends HumanoidMobPatch<CompanionEntity> {

    public CompanionEntityPatch(CompanionEntity original) {
        super(original, Factions.VILLAGER);  // Allies with villagers, hostile to undead
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);

        // Basic living animations
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
        animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
        animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);

        // Critical injury animations (when health <= 2 hearts)
        animator.addLivingAnimation(LivingMotions.KNEEL, Animations.BIPED_KNEEL);
        animator.addLivingAnimation(LivingMotions.SNEAK, Animations.BIPED_SNEAK);

        // Ranged combat animations (bow)
        animator.addLivingAnimation(LivingMotions.AIM, Animations.BIPED_BOW_AIM);
        animator.addLivingAnimation(LivingMotions.SHOT, Animations.BIPED_BOW_SHOT);

        // Crossbow animations
        animator.addLivingAnimation(LivingMotions.RELOAD, Animations.BIPED_CROSSBOW_RELOAD);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        // Let death animation play normally
        if (this.original.isDeadOrDying()) {
            this.currentLivingMotion = LivingMotions.DEATH;
            return;
        }

        // Check for critical injury state (if enabled)
        if (ModConfig.get().isEnableCriticalInjury() && this.original.isCriticallyInjured()) {
            // Use SNEAK animation when moving, KNEEL when stationary
            if (this.original.walkAnimation.speed() > 0.01F) {
                this.currentLivingMotion = LivingMotions.SNEAK;
            } else {
                this.currentLivingMotion = LivingMotions.KNEEL;
            }
            return;
        }
        // Use ranged mob motion update - detects isUsingItem() for bow/crossbow animations
        super.commonAggressiveRangedMobUpdateMotion(considerInaction);
    }

    @Override
    public void setAIAsInfantry(boolean holdingRangedWeapon) {
        // Only set melee combat AI when not holding a ranged weapon
        // Ranged combat is handled by the vanilla AI
        if (!holdingRangedWeapon) {
            CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();

            if (builder != null) {
                this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, builder.build(this)));
                this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.original, 1.2D, true));
            }
        }
    }

    @Override
    protected void initAI() {
        super.initAI();
        // Combat AI is handled by setAIAsInfantry
    }
}
