package com.payangar.immersivecompanions.data;

import com.payangar.immersivecompanions.entity.CompanionGender;
import net.minecraft.resources.ResourceLocation;

/**
 * Handles skin texture mapping for companions.
 * Initially uses vanilla Steve/Alex textures, with support for custom skin variants.
 */
public class CompanionSkins {

    // Vanilla player skin textures
    private static final ResourceLocation STEVE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    /**
     * Gets the skin texture for a companion based on gender and skin index.
     *
     * @param gender    The gender of the companion
     * @param skinIndex The skin variant index (currently unused, for future custom skins)
     * @return The ResourceLocation of the skin texture
     */
    public static ResourceLocation getSkin(CompanionGender gender, int skinIndex) {
        // For now, return vanilla Steve/Alex textures
        // In the future, this can be expanded to support custom skin variants:
        // e.g., immersivecompanions:textures/entity/companion/male_1.png
        return gender == CompanionGender.MALE ? STEVE_TEXTURE : ALEX_TEXTURE;
    }
}
