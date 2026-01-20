package com.payangar.immersivecompanions.data;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionGender;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Handles skin texture mapping for companions.
 * Dynamically discovers available skins from resource packs and the mod's assets.
 */
public class CompanionSkins {

    /** Maximum skin variants to consider when skins haven't been discovered (server-side fallback) */
    public static final int MAX_SKIN_VARIANTS = 15;

    // Vanilla player skin textures (used as fallback)
    private static final ResourceLocation STEVE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final ResourceLocation ALEX_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    private static final SkinInfo STEVE_FALLBACK = new SkinInfo(STEVE_TEXTURE, false);
    private static final SkinInfo ALEX_FALLBACK = new SkinInfo(ALEX_TEXTURE, true);

    private static List<SkinInfo> maleSkins = new ArrayList<>();
    private static List<SkinInfo> femaleSkins = new ArrayList<>();

    /**
     * Called during resource reload (client-side) to discover available skins.
     * Scans textures/entity/male/ and textures/entity/female/ for skin files.
     */
    public static void discoverSkins(ResourceManager resourceManager) {
        maleSkins = discoverSkinsInFolder(resourceManager, "male", false);
        femaleSkins = discoverSkinsInFolder(resourceManager, "female", true);

        ImmersiveCompanions.LOGGER.info("Discovered {} male skins and {} female skins",
                maleSkins.size(), femaleSkins.size());
    }

    private static List<SkinInfo> discoverSkinsInFolder(ResourceManager rm, String folder, boolean defaultSlim) {
        List<SkinInfo> skins = new ArrayList<>();

        // Find all .png files in the folder
        Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> resources = rm.listResources(
                "textures/entity/" + folder,
                path -> path.getPath().endsWith(".png")
        );

        for (ResourceLocation texture : resources.keySet()) {
            if (texture.getNamespace().equals(ImmersiveCompanions.MOD_ID)) {
                String filename = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
                // Only include files matching pattern: {male|female}_{number}{_slim|_wide}?.png
                if (SkinInfo.isValidSkin(filename)) {
                    skins.add(SkinInfo.fromPath(texture, defaultSlim));
                }
            }
        }

        // Sort by filename for consistent ordering
        skins.sort(Comparator.comparing(s -> s.texture().getPath()));
        return skins;
    }

    /**
     * Gets the skin info for a companion based on gender and skin index.
     *
     * @param gender    The gender of the companion
     * @param skinIndex The skin variant index
     * @return The SkinInfo containing texture location and slim status
     */
    public static SkinInfo getSkinInfo(CompanionGender gender, int skinIndex) {
        List<SkinInfo> skins = gender == CompanionGender.MALE ? maleSkins : femaleSkins;
        if (skins.isEmpty()) {
            return getVanillaFallback(gender);
        }
        int actualIndex = Math.floorMod(skinIndex, skins.size());
        return skins.get(actualIndex);
    }

    /**
     * Gets the vanilla fallback skin for a gender.
     */
    private static SkinInfo getVanillaFallback(CompanionGender gender) {
        return gender == CompanionGender.MALE ? STEVE_FALLBACK : ALEX_FALLBACK;
    }

    /**
     * Gets the skin texture for a companion based on gender and skin index.
     * Convenience method that delegates to getSkinInfo.
     *
     * @param gender    The gender of the companion
     * @param skinIndex The skin variant index
     * @return The ResourceLocation of the skin texture
     */
    public static ResourceLocation getSkin(CompanionGender gender, int skinIndex) {
        return getSkinInfo(gender, skinIndex).texture();
    }

    /**
     * Gets the number of available skins for a gender.
     * On client (after skin discovery): returns actual skin count, capped at MAX_SKIN_VARIANTS.
     * On server (skins not discovered): returns MAX_SKIN_VARIANTS as fallback.
     */
    public static int getSkinCount(CompanionGender gender) {
        List<SkinInfo> skins = gender == CompanionGender.MALE ? maleSkins : femaleSkins;
        if (skins.isEmpty()) {
            // Server-side or before first resource reload - use max as fallback
            return MAX_SKIN_VARIANTS;
        }
        return Math.min(skins.size(), MAX_SKIN_VARIANTS);
    }
}
