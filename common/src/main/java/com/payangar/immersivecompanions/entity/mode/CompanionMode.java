package com.payangar.immersivecompanions.entity.mode;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Enum defining the behavioral modes for companions.
 * Each mode controls which movement-related goals are active via canUse() checks.
 *
 * <p>Goals check the companion's current mode to determine if they should run.
 */
public enum CompanionMode {
    WANDER("wander"),
    FOLLOW("follow");

    private final String id;

    CompanionMode(String id) {
        this.id = id;
    }

    /**
     * Gets the unique identifier for this mode.
     * Used for NBT persistence and network sync.
     *
     * @return The mode's unique string ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name for this mode, used in UI.
     *
     * @return The translatable display name component
     */
    public Component getDisplayName() {
        return Component.translatable("mode.immersivecompanions." + id);
    }

    /**
     * Looks up a mode by its string ID.
     *
     * @param id The mode ID to look up
     * @return The corresponding mode, or WANDER as fallback
     */
    @Nullable
    public static CompanionMode byId(String id) {
        for (CompanionMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return WANDER;
    }

    /**
     * Gets the next mode in the cycle.
     * Order: WANDER → FOLLOW → WANDER
     *
     * @return The next mode
     */
    public CompanionMode next() {
        CompanionMode[] modes = values();
        return modes[(this.ordinal() + 1) % modes.length];
    }
}
