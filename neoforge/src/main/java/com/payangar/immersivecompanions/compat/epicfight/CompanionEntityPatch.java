package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
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
 * Ranged weapon animations (bow/crossbow) come from the weapon capability system.
 */
public class CompanionEntityPatch extends HumanoidMobPatch<CompanionEntity> {

    public CompanionEntityPatch(CompanionEntity original) {
        super(original, Factions.VILLAGER);  // Allies with villagers, hostile to undead

        // Set up callback to trigger shooting animation after ranged attacks
        // This replaces what Epic Fight's mixins do for vanilla RangedAttackGoal
        original.setOnRangedAttackCallback(this::playShootingAnimation);
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

        // Note: Ranged animations (AIM, SHOT, RELOAD) come from weapon capabilities
        // via modifyLivingMotionByCurrentItem() inherited from HumanoidMobPatch
    }

    @Override
    public OpenMatrix4f getModelMatrix(float partialTicks) {
        OpenMatrix4f matrix = super.getModelMatrix(partialTicks);
        // Apply scale correction to match player size
        float scale = CompanionEntity.RENDER_SCALE;
        return matrix.scale(scale, scale, scale);
    }

    @Override
    public void updateMotion(boolean considerInaction) {
        // Let death animation play normally
        if (this.original.isDeadOrDying()) {
            this.currentLivingMotion = LivingMotions.DEATH;
            return;
        }

        // Handle crouching state (from any source: critical injury, owner mimic, etc.)
        if (this.original.isCrouching()) {
            if (this.original.walkAnimation.speed() > 0.01F) {
                this.currentLivingMotion = LivingMotions.SNEAK;
            } else {
                this.currentLivingMotion = LivingMotions.KNEEL;
            }
            return;
        }

        // Handle weapon holstering when not in combat (Epic Fight uses weapon on back)
        updateWeaponHolstering();

        // Use ranged mob motion update - detects isUsingItem() for bow/crossbow animations
        super.commonAggressiveRangedMobUpdateMotion(considerInaction);
    }

    /**
     * Updates weapon positioning based on combat state.
     * When not in combat, moves weapons to the back.
     * When in combat, moves weapons to hands.
     */
    private void updateWeaponHolstering() {
        if (!ModConfig.get().isEnableWeaponHolstering()) {
            return;
        }

        if (!(this.getArmature() instanceof ToolHolderArmature toolArmature)) {
            return;
        }

        boolean inCombat = this.original.isAggressive();

        if (!inCombat && !isWeaponOnBack()) {
            // Move weapons to back when not in combat
            this.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.backToolJoint());
            this.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.backToolJoint());
        } else if (inCombat && isWeaponOnBack()) {
            // Move weapons to hands when entering combat
            this.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.rightToolJoint());
            this.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.leftToolJoint());
        }
    }

    /**
     * Checks if the main hand weapon is currently holstered on the back.
     */
    private boolean isWeaponOnBack() {
        if (!(this.getArmature() instanceof ToolHolderArmature toolArmature)) {
            return false;
        }
        return this.getParentJointOfHand(InteractionHand.MAIN_HAND) == toolArmature.backToolJoint();
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
