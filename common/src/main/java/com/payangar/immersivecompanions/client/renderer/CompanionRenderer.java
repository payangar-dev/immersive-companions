package com.payangar.immersivecompanions.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.payangar.immersivecompanions.data.SkinInfo;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import com.payangar.immersivecompanions.client.renderer.layer.ConditionalItemInHandLayer;
import com.payangar.immersivecompanions.platform.Services;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Renders companions using player models (Steve/Alex).
 * Swaps between normal and slim models based on the companion's gender.
 */
public class CompanionRenderer extends HumanoidMobRenderer<CompanionEntity, PlayerModel<CompanionEntity>> {

    private final PlayerModel<CompanionEntity> normalModel;
    private final PlayerModel<CompanionEntity> slimModel;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // Create both model variants
        this.normalModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // Add armor layer
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        // When Epic Fight is loaded, keep the default ItemInHandLayer
        // Epic Fight handles weapon holstering by moving items to back joint
        // When Epic Fight is NOT loaded, use ConditionalItemInHandLayer to hide weapons when not in combat
        if (!Services.get().isModLoaded("epicfight")) {
            this.layers.removeIf(layer -> layer.getClass() == ItemInHandLayer.class);
            this.addLayer(new ConditionalItemInHandLayer(this, context.getItemInHandRenderer()));
        }
    }

    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Swap model based on skin info (respects _slim/_wide suffix or folder default)
        SkinInfo skinInfo = entity.getSkinInfo();
        this.model = skinInfo.slim() ? slimModel : normalModel;

        // Set crouching state - required for vanilla rendering since HumanoidMobRenderer
        // doesn't set this for non-player entities (unlike PlayerRenderer)
        this.model.crouching = entity.isCrouching();

        // Set arm pose based on charging state
        setModelArmPose(entity);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * Sets the model's arm pose based on the companion's holster state, charging state, and held weapon.
     * When weapon is holstered, arms stay lowered. When drawn, applies appropriate weapon poses.
     */
    private void setModelArmPose(CompanionEntity entity) {
        // When weapon is holstered, keep arms lowered
        if (entity.isWeaponHolstered()) {
            this.model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            return;
        }

        // Weapon drawn - apply poses based on held item and charging state
        ItemStack mainHand = entity.getMainHandItem();
        HumanoidModel.ArmPose armPose = HumanoidModel.ArmPose.EMPTY;

        if (entity.isCharging()) {
            if (mainHand.getItem() instanceof CrossbowItem) {
                armPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
            } else if (mainHand.getItem() instanceof BowItem) {
                armPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        } else if (mainHand.getItem() instanceof CrossbowItem) {
            // Crossbow idle hold pose when not charging
            armPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }

        // Set arm pose for the main hand
        if (entity.getMainArm() == HumanoidArm.RIGHT) {
            this.model.rightArmPose = armPose;
            this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        } else {
            this.model.leftArmPose = armPose;
            this.model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return entity.getSkinTexture();
    }

    @Override
    protected void scale(CompanionEntity entity, PoseStack poseStack, float partialTick) {
        float scale = CompanionEntity.RENDER_SCALE;
        poseStack.scale(scale, scale, scale);
    }
}
