package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

/**
 * Epic Fight entity patch for CompanionEntity.
 * Extends HumanoidMobPatch to provide humanoid combat animations for
 * companions.
 * This enables companions to use Epic Fight's biped animations for combat,
 * walking, idle, and other actions. Supports both melee and ranged combat.
 * Ranged weapon animations (bow/crossbow) come from the weapon capability
 * system.
 */
public class CompanionEntityPatch extends HumanoidMobPatch<CompanionEntity> {

    /**
     * Tracks the previous holster state to detect changes and update animations.
     */
    private boolean wasHolstered = true;

    public CompanionEntityPatch(CompanionEntity original) {
        super(original, Factions.VILLAGER); // Allies with villagers, hostile to undead
    }

    @Override
    public void initAnimator(Animator animator) {
        super.initAnimator(animator);

        // Basic living animations
        animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
        animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
        animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_RUN);
        animator.addLivingAnimation(LivingMotions.JUMP, Animations.BIPED_JUMP);
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
    public void preTick(EntityTickEvent.Pre event) {
        super.preTick(event);

        // Server-side: detect holster state changes and update animations/AI
        if (!this.isLogicalClient()) {
            boolean holstered = this.original.isWeaponHolstered();

            if (holstered != wasHolstered) {
                wasHolstered = holstered;

                // Update locomotion animations (bow AIM, crossbow RELOAD, etc.)
                this.modifyLivingMotionByCurrentItem(false);

                // Rebuild combat AI when unholstering (for melee weapon attack animations)
                if (!holstered) {
                    this.initAI();
                }
            }
        }
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
        // Handle weapon holstering when not in combat (Epic Fight uses weapon on back)
        updateWeaponHolstering();

        if (this.original.isDeadOrDying()) {
            this.currentLivingMotion = LivingMotions.DEATH;
        } else if (this.state.inaction() && considerInaction) {
            if (this.original.isCrouching()) {
                this.currentLivingMotion = LivingMotions.KNEEL;
            } else {
                this.currentLivingMotion = LivingMotions.IDLE;
            }
        } else if (this.original.getVehicle() != null) {
            this.currentLivingMotion = LivingMotions.MOUNT;
        } else if (!this.original.onGround() && this.original.getDeltaMovement().y > 0.0) {
            this.currentLivingMotion = LivingMotions.JUMP;
        } else if (!this.original.onGround() && this.original.getDeltaMovement().y < -0.5) {
            this.currentLivingMotion = LivingMotions.FALL;
        } else if (this.original.walkAnimation.speed() > 0.01F) {
            if (this.original.isCrouching()) {
                this.currentLivingMotion = LivingMotions.SNEAK;
            } else if (this.original.isSprinting()) {
                this.currentLivingMotion = LivingMotions.CHASE;
            } else {
                this.currentLivingMotion = LivingMotions.WALK;
            }
        } else if (this.original.isCrouching()) {
            this.currentLivingMotion = LivingMotions.KNEEL;
        } else {
            this.currentLivingMotion = LivingMotions.IDLE;
        }
        this.currentCompositeMotion = this.currentLivingMotion;

        if (this.original.isWeaponHolstered() || !this.original.getCombatType().isRanged())
            return;

        // Ranged combat animations
        UseAnim useAction = this.original.getItemInHand(this.original.getUsedItemHand()).getUseAnimation();
        if (((StaticAnimation) this.getClientAnimator().getCompositeLayer(Priority.MIDDLE).animationPlayer
                .getRealAnimation().get()).isReboundAnimation()) {
            this.currentCompositeMotion = LivingMotions.SHOT;
        } else if (this.original.isUsingItem()) {
            if (useAction == UseAnim.CROSSBOW) {
                this.currentCompositeMotion = LivingMotions.RELOAD;
            } else {
                this.currentCompositeMotion = LivingMotions.AIM;
            }
        } else if (CrossbowItem.isCharged(this.original.getMainHandItem())) {
            this.currentCompositeMotion = LivingMotions.AIM;
        } else {
            this.currentCompositeMotion = this.currentLivingMotion;
        }
    }

    /**
     * Updates weapon positioning based on holster state.
     * When holstered, moves weapons to the back.
     * When drawn, moves weapons to hands.
     * Animation and AI updates are handled by preTick().
     */
    private void updateWeaponHolstering() {
        if (!(this.getArmature() instanceof ToolHolderArmature toolArmature)) {
            return;
        }

        boolean holstered = this.original.isWeaponHolstered();

        if (holstered && !isWeaponOnBack()) {
            // Move weapons to back when holstered
            this.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.backToolJoint());
            this.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.backToolJoint());
        } else if (!holstered && isWeaponOnBack()) {
            // Move weapons to hands when drawn
            this.setParentJointOfHand(InteractionHand.MAIN_HAND, toolArmature.rightToolJoint());
            this.setParentJointOfHand(InteractionHand.OFF_HAND, toolArmature.leftToolJoint());
        }
    }

    /**
     * Returns the item capability for the held item.
     * When the weapon is holstered (on back), returns EMPTY capability
     * so Epic Fight uses unarmed animations instead of weapon-holding animations.
     */
    @Override
    public CapabilityItem getHoldingItemCapability(InteractionHand hand) {
        if (this.original.isWeaponHolstered()) {
            return CapabilityItem.EMPTY;
        }
        return super.getHoldingItemCapability(hand);
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
