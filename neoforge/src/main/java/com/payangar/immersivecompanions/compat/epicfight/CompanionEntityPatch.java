package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.model.armature.types.ToolHolderArmature;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.registry.entries.EpicFightAttributes;
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
    public void preTick() {
        super.preTick();

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

    /**
     * Workaround for Epic Fight's HumanoidMobPatch.updateHeldItem() bugs.
     * <p>
     * Epic Fight has two bugs causing crashes when re-equipping dual-wielded weapons:
     * <ol>
     *   <li>Line 160: Uses {@code to.getAttributeModifiers()} instead of {@code from.getAttributeModifiers()}
     *       when removing old modifiers</li>
     *   <li>Line 177: Calls {@code addTransientModifier()} without checking if modifier already exists</li>
     * </ol>
     * <p>
     * This override fixes both issues by:
     * <ul>
     *   <li>Using {@code from.getAttributeModifiers()} for removal (correct item)</li>
     *   <li>Using {@code removeModifier(id)} which is safe if modifier doesn't exist</li>
     *   <li>Checking {@code hasModifier(id)} before {@code addTransientModifier()}</li>
     * </ul>
     */
    @Override
    public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
        this.initAI();

        if (hand == InteractionHand.OFF_HAND) {
            // Remove attribute modifiers from the OLD item (from), not the new one
            if (!from.isEmpty()) {
                from.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute == Attributes.ATTACK_SPEED) {
                        AttributeInstance instance = this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED);
                        if (instance != null) {
                            // removeModifier(id) is safe even if modifier doesn't exist
                            instance.removeModifier(modifier.id());
                        }
                    }
                });
            }

            // Remove Epic Fight capability modifiers from the old item
            if (!fromCap.isEmpty()) {
                removeCapabilityModifiers(fromCap, Attributes.ATTACK_SPEED, EpicFightAttributes.OFFHAND_ATTACK_SPEED);
                removeCapabilityModifiers(fromCap, EpicFightAttributes.ARMOR_NEGATION, EpicFightAttributes.OFFHAND_ARMOR_NEGATION);
                removeCapabilityModifiers(fromCap, EpicFightAttributes.IMPACT, EpicFightAttributes.OFFHAND_IMPACT);
                removeCapabilityModifiers(fromCap, EpicFightAttributes.MAX_STRIKES, EpicFightAttributes.OFFHAND_MAX_STRIKES);
            }

            // Add attribute modifiers from the NEW item
            if (!to.isEmpty()) {
                to.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute == Attributes.ATTACK_SPEED) {
                        addModifierSafely(EpicFightAttributes.OFFHAND_ATTACK_SPEED, modifier);
                    }
                });
            }

            // Add Epic Fight capability modifiers from the new item
            if (!toCap.isEmpty()) {
                addCapabilityModifiers(toCap, Attributes.ATTACK_SPEED, EpicFightAttributes.OFFHAND_ATTACK_SPEED);
                addCapabilityModifiers(toCap, EpicFightAttributes.ARMOR_NEGATION, EpicFightAttributes.OFFHAND_ARMOR_NEGATION);
                addCapabilityModifiers(toCap, EpicFightAttributes.IMPACT, EpicFightAttributes.OFFHAND_IMPACT);
                addCapabilityModifiers(toCap, EpicFightAttributes.MAX_STRIKES, EpicFightAttributes.OFFHAND_MAX_STRIKES);
            }
        }

        this.modifyLivingMotionByCurrentItem(false);

        // Note: We intentionally skip super.updateHeldItem() because LivingEntityPatch's
        // implementation is empty, and we've already done everything HumanoidMobPatch does
    }

    /**
     * Removes capability attribute modifiers safely (no error if modifier doesn't exist).
     */
    private void removeCapabilityModifiers(CapabilityItem cap, Holder<Attribute> sourceAttr, Holder<Attribute> targetAttr) {
        AttributeInstance instance = this.original.getAttribute(targetAttr);
        if (instance != null) {
            cap.getAttributeModifiers(this).get(sourceAttr).forEach(modifier ->
                instance.removeModifier(modifier.id())
            );
        }
    }

    /**
     * Adds capability attribute modifiers safely (checks if modifier already exists).
     */
    private void addCapabilityModifiers(CapabilityItem cap, Holder<Attribute> sourceAttr, Holder<Attribute> targetAttr) {
        AttributeInstance instance = this.original.getAttribute(targetAttr);
        if (instance != null) {
            cap.getAttributeModifiers(this).get(sourceAttr).forEach(modifier ->
                addModifierSafely(instance, modifier)
            );
        }
    }

    /**
     * Adds a transient modifier only if it doesn't already exist on the attribute instance.
     */
    private void addModifierSafely(Holder<Attribute> attribute, AttributeModifier modifier) {
        AttributeInstance instance = this.original.getAttribute(attribute);
        if (instance != null) {
            addModifierSafely(instance, modifier);
        }
    }

    /**
     * Adds a transient modifier only if it doesn't already exist on the attribute instance.
     */
    private void addModifierSafely(AttributeInstance instance, AttributeModifier modifier) {
        if (!instance.hasModifier(modifier.id())) {
            instance.addTransientModifier(modifier);
        }
    }
}
