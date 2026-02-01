package com.payangar.immersivecompanions.compat.hardcorerevival;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * Central manager for Hardcore Revival mod compatibility.
 * Tracks if the mod is loaded and provides reflection-based API access.
 *
 * This class lives in common and does NOT reference any HR classes directly,
 * making it safe to load even when Hardcore Revival is absent.
 */
public final class HardcoreRevivalCompat {

    private static boolean modLoaded = false;

    // Cached reflection handles
    private static Class<?> apiClass = null;
    private static Method isKnockedOutMethod = null;
    private static Method wakeupMethod = null;
    private static boolean reflectionFailed = false;

    private HardcoreRevivalCompat() {
        // Utility class
    }

    /**
     * Checks if the Hardcore Revival mod is loaded and initialized.
     */
    public static boolean isModLoaded() {
        return modLoaded;
    }

    /**
     * Called by loader-specific init code when Hardcore Revival is detected.
     */
    public static void setModLoaded(boolean loaded) {
        modLoaded = loaded;
        if (loaded) {
            initReflection();
        }
    }

    /**
     * Initializes reflection handles for the Hardcore Revival API.
     * Called once when the mod is detected.
     */
    private static void initReflection() {
        if (reflectionFailed) {
            return;
        }

        try {
            apiClass = Class.forName("net.blay09.mods.hardcorerevival.api.HardcoreRevivalAPI");
            isKnockedOutMethod = apiClass.getMethod("isKnockedOut", Player.class);
            wakeupMethod = apiClass.getMethod("wakeup", Player.class, boolean.class);
            ImmersiveCompanions.LOGGER.debug("Hardcore Revival API reflection initialized successfully");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            ImmersiveCompanions.LOGGER.warn("Failed to initialize Hardcore Revival API reflection: {}", e.getMessage());
            reflectionFailed = true;
            apiClass = null;
            isKnockedOutMethod = null;
            wakeupMethod = null;
        }
    }

    /**
     * Checks if the given player is in the "knocked out" state.
     *
     * @param player The player to check
     * @return true if the player is knocked out, false otherwise or if API unavailable
     */
    public static boolean isKnockedOut(Player player) {
        if (!modLoaded || reflectionFailed || isKnockedOutMethod == null) {
            return false;
        }

        try {
            Object result = isKnockedOutMethod.invoke(null, player);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            ImmersiveCompanions.LOGGER.debug("Failed to call isKnockedOut: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Revives a knocked-out player.
     *
     * @param player The player to revive
     * @param triggerToast Whether to show the revival toast notification
     * @return true if the wakeup call succeeded, false otherwise
     */
    public static boolean wakeup(Player player, boolean triggerToast) {
        if (!modLoaded || reflectionFailed || wakeupMethod == null) {
            return false;
        }

        try {
            wakeupMethod.invoke(null, player, triggerToast);
            return true;
        } catch (Exception e) {
            ImmersiveCompanions.LOGGER.warn("Failed to call wakeup: {}", e.getMessage());
            return false;
        }
    }
}
