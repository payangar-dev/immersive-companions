package com.payangar.immersivecompanions.client.gui;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.network.ModNetworking;
import com.payangar.immersivecompanions.recruitment.CompanionPricing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Recruitment dialog screen for companions.
 * Shows the companion's face, dialogue, price, and Deny/Recruit buttons.
 */
public class CompanionRecruitmentScreen extends Screen {

    // GUI dimensions
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 120;

    // Face rendering
    private static final int FACE_SIZE = 32;
    private static final int FACE_U = 8;  // Face UV starts at (8, 8) in skin texture
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;  // Hat overlay UV starts at (40, 8)
    private static final int HAT_V = 8;
    private static final int SKIN_TEX_SIZE = 64;
    private static final float FACE_SCALE = 4.0f;  // Scale 8x8 to 32x32

    // Emerald item for rendering
    private static final ItemStack EMERALD_STACK = new ItemStack(Items.EMERALD);

    // Background texture (256x256, panel at UV 0,0 is 176x120)
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ImmersiveCompanions.MOD_ID, "textures/gui/recruitment.png");

    // Text colors for light background
    private static final int TEXT_COLOR_NORMAL = 0x404040;      // Dark gray for main text
    private static final int TEXT_COLOR_LABEL = 0x555555;       // Medium gray for labels
    private static final int TEXT_COLOR_DISCOUNT = 0x3A8A3A;    // Dark green for discounts
    private static final int TEXT_COLOR_MARKUP = 0xAA3A3A;      // Dark red for markups
    private static final int TEXT_COLOR_STRIKETHROUGH = 0x808080; // Gray for strikethrough

    private final CompanionEntity companion;
    private final int entityId;
    private final int basePrice;
    private final int finalPrice;
    private final boolean hasDiscount;
    private final boolean hasMarkup;
    private final boolean canAfford;

    private int leftPos;
    private int topPos;

    public CompanionRecruitmentScreen(CompanionEntity companion, int basePrice, int finalPrice) {
        super(Component.translatable("gui.immersivecompanions.recruitment.title"));
        this.companion = companion;
        this.entityId = companion.getId();
        this.basePrice = basePrice;
        this.finalPrice = finalPrice;
        this.hasDiscount = finalPrice < basePrice;
        this.hasMarkup = finalPrice > basePrice;

        // Calculate affordability on client
        Minecraft mc = Minecraft.getInstance();
        this.canAfford = mc.player != null && CompanionPricing.countEmeralds(mc.player) >= finalPrice;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Button dimensions
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonY = topPos + GUI_HEIGHT - 30;
        int buttonGap = 10;

        // Deny button (left)
        int denyX = leftPos + (GUI_WIDTH / 2) - buttonWidth - (buttonGap / 2);
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.immersivecompanions.recruitment.deny"),
                button -> this.onClose()
        ).bounds(denyX, buttonY, buttonWidth, buttonHeight).build());

        // Recruit button (right) - disabled if can't afford
        int recruitX = leftPos + (GUI_WIDTH / 2) + (buttonGap / 2);
        Button recruitButton = Button.builder(
                Component.translatable("gui.immersivecompanions.recruitment.recruit"),
                button -> onRecruit()
        ).bounds(recruitX, buttonY, buttonWidth, buttonHeight).build();
        recruitButton.active = canAfford;
        this.addRenderableWidget(recruitButton);
    }

    private void onRecruit() {
        // Phase 1: Button is visible but purchase logic not implemented yet
        // Just close the screen for now
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render semi-transparent dark overlay (vanilla inventory style)
        this.renderTransparentBackground(graphics);

        // Render our panel texture on top
        graphics.blit(BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, 256, 256);

        // Draw companion face (top left area)
        int faceX = leftPos + 12;
        int faceY = topPos + 10;
        renderCompanionFace(graphics, faceX, faceY);

        // Draw dialogue text (right of face)
        int textX = faceX + FACE_SIZE + 10;
        int textY = topPos + 12;
        int maxTextWidth = GUI_WIDTH - (textX - leftPos) - 10;

        Component dialogue = Component.translatable("gui.immersivecompanions.recruitment.dialogue");
        graphics.drawWordWrap(this.font, dialogue, textX, textY, maxTextWidth, TEXT_COLOR_NORMAL);

        // Draw price section
        int priceY = topPos + 55;
        renderPriceSection(graphics, priceY);

        // Render widgets (buttons)
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCompanionFace(GuiGraphics graphics, int x, int y) {
        ResourceLocation skinTexture = companion.getSkinTexture();

        // Draw face (UV 8,8 to 16,16 at 32x32)
        graphics.blit(skinTexture, x, y, FACE_SIZE, FACE_SIZE,
                FACE_U, FACE_V, 8, 8, SKIN_TEX_SIZE, SKIN_TEX_SIZE);

        // Draw hat overlay on top (UV 40,8 to 48,16)
        graphics.blit(skinTexture, x, y, FACE_SIZE, FACE_SIZE,
                HAT_U, HAT_V, 8, 8, SKIN_TEX_SIZE, SKIN_TEX_SIZE);
    }

    private void renderPriceSection(GuiGraphics graphics, int y) {
        Component priceLabel = Component.translatable("gui.immersivecompanions.recruitment.price");
        int labelX = leftPos + 12;
        graphics.drawString(this.font, priceLabel, labelX, y, TEXT_COLOR_LABEL, false);

        int priceX = labelX + this.font.width(priceLabel) + 5;

        if (hasDiscount || hasMarkup) {
            // Show base price with strikethrough, then final price below
            renderStrikethroughPrice(graphics, priceX, y, basePrice);

            // Show final price below
            int finalY = y + 12;
            int finalColor = hasDiscount ? TEXT_COLOR_DISCOUNT : TEXT_COLOR_MARKUP;
            renderPriceWithEmerald(graphics, priceX, finalY, finalPrice, finalColor);
        } else {
            // No modifier - show price normally
            int color = canAfford ? TEXT_COLOR_DISCOUNT : TEXT_COLOR_MARKUP;
            renderPriceWithEmerald(graphics, priceX, y, finalPrice, color);
        }
    }

    private void renderPriceWithEmerald(GuiGraphics graphics, int x, int y, int price, int color) {
        // Render emerald icon
        graphics.renderFakeItem(EMERALD_STACK, x, y - 4);

        // Render price number
        String priceText = String.valueOf(price);
        graphics.drawString(this.font, priceText, x + 18, y, color, false);
    }

    private void renderStrikethroughPrice(GuiGraphics graphics, int x, int y, int price) {
        // Render emerald icon (grayed out)
        graphics.renderFakeItem(EMERALD_STACK, x, y - 4);

        // Render price with strikethrough
        String priceText = String.valueOf(price);
        int textX = x + 18;
        int textWidth = this.font.width(priceText);

        // Draw grayed out text
        graphics.drawString(this.font, priceText, textX, y, TEXT_COLOR_STRIKETHROUGH, false);

        // Draw strikethrough line
        int lineY = y + this.font.lineHeight / 2;
        graphics.fill(textX - 1, lineY, textX + textWidth + 1, lineY + 1, 0xFF000000 | TEXT_COLOR_STRIKETHROUGH);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        // Send packet to server to clear interaction state
        ModNetworking.get().sendCloseRecruitmentScreen(entityId);
    }

    @Override
    public void renderBlurredBackground(float partialTick) {
        // Don't render blur - we want a clear view of the game world behind the dialog
    }
}
