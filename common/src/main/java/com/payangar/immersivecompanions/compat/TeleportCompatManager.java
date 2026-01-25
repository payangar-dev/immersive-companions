package com.payangar.immersivecompanions.compat;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.platform.Services;

import java.util.ArrayList;
import java.util.List;

/**
 * Central manager for teleport mod compatibility handlers.
 * Tracks which teleport mods are loaded and handles their initialization.
 */
public final class TeleportCompatManager {

    private static final List<TeleportModHandler> registeredHandlers = new ArrayList<>();

    private TeleportCompatManager() {
        // Utility class - prevent instantiation
    }

    /**
     * Interface for teleport mod compatibility handlers.
     */
    public interface TeleportModHandler {
        /**
         * @return The mod ID this handler is for
         */
        String getModId();

        /**
         * @return true if the mod is currently loaded
         */
        default boolean isLoaded() {
            return Services.get().isModLoaded(getModId());
        }

        /**
         * @return A display name for logging purposes
         */
        default String getDisplayName() {
            return getModId();
        }
    }

    /**
     * Registers a teleport mod handler.
     * Should be called during mod initialization if the mod is detected.
     *
     * @param handler The handler to register
     */
    public static void registerHandler(TeleportModHandler handler) {
        registeredHandlers.add(handler);
        ImmersiveCompanions.LOGGER.info("Registered teleport compatibility handler for: {}", handler.getDisplayName());
    }

    /**
     * @return List of all registered handlers
     */
    public static List<TeleportModHandler> getRegisteredHandlers() {
        return List.copyOf(registeredHandlers);
    }

    /**
     * Checks if a specific teleport mod has a registered handler.
     *
     * @param modId The mod ID to check
     * @return true if a handler is registered for this mod
     */
    public static boolean hasHandler(String modId) {
        return registeredHandlers.stream()
                .anyMatch(h -> h.getModId().equals(modId));
    }

    /**
     * Clears all registered handlers. Used during testing or server shutdown.
     */
    public static void clear() {
        registeredHandlers.clear();
    }
}
