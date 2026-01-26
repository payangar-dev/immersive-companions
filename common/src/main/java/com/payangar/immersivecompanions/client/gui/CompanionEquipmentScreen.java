package com.payangar.immersivecompanions.client.gui;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.inventory.CompanionEquipmentMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Equipment management screen for companions.
 * Displays the companion entity, armor slots, weapon slots, and player inventory.
 */
public class CompanionEquipmentScreen extends AbstractContainerScreen<CompanionEquipmentMenu> {

    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ImmersiveCompanions.MOD_ID, "textures/gui/companion.png");

    // Entity rendering area (black area in the texture)
    // The black area is approximately at x=26, y=8 with width=51, height=72
    private static final int ENTITY_AREA_X = 26;
    private static final int ENTITY_AREA_Y = 8;
    private static final int ENTITY_AREA_WIDTH = 51;

    // Entity rendering position - center of the black area
    private static final int ENTITY_X_OFFSET = ENTITY_AREA_WIDTH / 2;  // Center horizontally
    private static final int ENTITY_Y_OFFSET = 34; // Position for proper vertical centering

    public CompanionEquipmentScreen(CompanionEquipmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        // Adjust label positions
        this.titleLabelX = 8;
        this.titleLabelY = -10; // Hide title above the GUI
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Hide the title label by moving it off-screen
        this.titleLabelY = -100;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Draw the background texture
        graphics.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Calculate entity rendering position
        int entityCenterX = this.leftPos + ENTITY_AREA_X + ENTITY_X_OFFSET;
        int entityCenterY = this.topPos + ENTITY_AREA_Y + ENTITY_Y_OFFSET;

        // Calculate mouse offset for entity rotation (relative to entity center)
        float relativeMouseX = entityCenterX - mouseX;
        float relativeMouseY = entityCenterY - 50 - mouseY; // Offset Y to look at face height

        // Render the companion entity
        CompanionEntity companion = this.menu.getCompanion();
        renderCompanionEntity(graphics, entityCenterX, entityCenterY, 30, relativeMouseX, relativeMouseY, companion);
    }

    /**
     * Renders the companion entity in the inventory screen.
     * Uses the same technique as vanilla player inventory rendering.
     */
    private void renderCompanionEntity(GuiGraphics graphics, int x, int y, int scale,
                                       float mouseX, float mouseY, CompanionEntity companion) {
        // Calculate rotation based on mouse position
        float yRot = (float) Math.atan(mouseX / 40.0F);
        float xRot = (float) Math.atan(mouseY / 40.0F);

        // Create rotation quaternions
        Quaternionf bodyRotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf headRotation = new Quaternionf().rotateX(xRot * 20.0F * ((float) Math.PI / 180.0F));
        bodyRotation.mul(headRotation);

        // Store original state
        float originalYBodyRot = companion.yBodyRot;
        float originalYRot = companion.getYRot();
        float originalXRot = companion.getXRot();
        float originalYHeadRotO = companion.yHeadRotO;
        float originalYHeadRot = companion.yHeadRot;
        Component originalCustomName = companion.getCustomName();

        // Apply temporary state for rendering
        companion.yBodyRot = 180.0F + yRot * 20.0F;
        companion.setYRot(180.0F + yRot * 40.0F);
        companion.setXRot(-xRot * 20.0F);
        companion.yHeadRot = companion.getYRot();
        companion.yHeadRotO = companion.getYRot();
        companion.setCustomName(null); // Temporarily remove name to hide it during rendering

        // Render the entity
        Vector3f translation = new Vector3f(0.0F, companion.getBbHeight() / 2.0F + 0.0625F, 0.0F);
        InventoryScreen.renderEntityInInventory(
                graphics, x, y, scale, translation, bodyRotation, headRotation, companion);

        // Restore original state
        companion.yBodyRot = originalYBodyRot;
        companion.setYRot(originalYRot);
        companion.setXRot(originalXRot);
        companion.yHeadRotO = originalYHeadRotO;
        companion.yHeadRot = originalYHeadRot;
        companion.setCustomName(originalCustomName);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Don't render any labels - clean interface
    }
}
