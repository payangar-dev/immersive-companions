package com.payangar.immersivecompanions.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Wrapper around vanilla ItemInHandLayer that conditionally renders held items.
 * - When in combat (aggressive): Always renders items in hand
 * - When not in combat: Only renders items that cannot be holstered
 *
 * This prevents double rendering (weapon shown both in hand and holstered).
 */
public class ConditionalItemInHandLayer extends ItemInHandLayer<CompanionEntity, PlayerModel<CompanionEntity>> {

    public ConditionalItemInHandLayer(RenderLayerParent<CompanionEntity, PlayerModel<CompanionEntity>> parent,
                                      ItemInHandRenderer itemInHandRenderer) {
        super(parent, itemInHandRenderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       CompanionEntity entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // If holstering is disabled, always render items in hand normally
        if (!ModConfig.enableWeaponHolstering) {
            super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                    partialTicks, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        // When weapon is drawn (not holstered), render items in hand
        if (!entity.isWeaponHolstered()) {
            super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount,
                    partialTicks, ageInTicks, netHeadYaw, headPitch);
            return;
        }

        // When not in combat, only render items that cannot be holstered
        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();

        boolean renderMainHand = !canHolster(mainHand);
        boolean renderOffHand = !canHolster(offHand);

        if (renderMainHand || renderOffHand) {
            // Manually render only the items that shouldn't be holstered
            poseStack.pushPose();

            if (renderMainHand && !mainHand.isEmpty()) {
                renderArmWithItem(entity, mainHand, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        HumanoidArm.RIGHT, poseStack, buffer, packedLight);
            }

            if (renderOffHand && !offHand.isEmpty()) {
                renderArmWithItem(entity, offHand, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        HumanoidArm.LEFT, poseStack, buffer, packedLight);
            }

            poseStack.popPose();
        }
    }

    /**
     * Checks if an item should be holstered (hidden from hand when not in combat).
     * Weapons like swords, axes, bows, and crossbows are holsterable.
     */
    private static boolean canHolster(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof AxeItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }
}
