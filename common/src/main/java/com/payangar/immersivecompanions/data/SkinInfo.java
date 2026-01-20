package com.payangar.immersivecompanions.data;

import net.minecraft.resources.ResourceLocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Record holding skin texture location and whether it uses the slim (Alex) model.
 */
public record SkinInfo(ResourceLocation texture, boolean slim) {

    // Strict pattern: {male|female}_{number}{optional:_wide|_slim}.png
    private static final Pattern SKIN_PATTERN = Pattern.compile(
            "^(male|female)_(\\d+)(_slim|_wide)?\\.png$"
    );

    /**
     * Checks if a filename matches the valid skin pattern.
     * Valid: male_1.png, male_12_slim.png, female_3_wide.png
     * Invalid: male_12_slim_emissive.png, random.png
     */
    public static boolean isValidSkin(String filename) {
        return SKIN_PATTERN.matcher(filename).matches();
    }

    /**
     * Parses skin info from a validated filename.
     * Priority order:
     * 1. Naming suffix: _slim.png or _wide.png overrides folder default
     * 2. Folder default: male/ = wide, female/ = slim
     *
     * @param texture The texture ResourceLocation
     * @param folderDefaultSlim Default slim value based on folder (male=false, female=true)
     */
    public static SkinInfo fromPath(ResourceLocation texture, boolean folderDefaultSlim) {
        String filename = texture.getPath().substring(texture.getPath().lastIndexOf('/') + 1);
        Matcher matcher = SKIN_PATTERN.matcher(filename);

        if (matcher.matches()) {
            String armType = matcher.group(3); // null, "_slim", or "_wide"
            if ("_slim".equals(armType)) {
                return new SkinInfo(texture, true);
            } else if ("_wide".equals(armType)) {
                return new SkinInfo(texture, false);
            }
        }

        return new SkinInfo(texture, folderDefaultSlim);
    }
}
