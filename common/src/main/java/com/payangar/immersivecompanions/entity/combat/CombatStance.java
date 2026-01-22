package com.payangar.immersivecompanions.entity.combat;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * Enum representing combat stances that control companion targeting behavior.
 * Each stance defines which targets the companion will engage.
 *
 * <p>Goals check the companion's current stance to determine if they should run.
 *
 * <p>Stance progression from least to most aggressive:
 * PASSIVE → DEFENSIVE → ASSIST → AGGRESSIVE
 */
public enum CombatStance {
    PASSIVE("passive"),
    DEFENSIVE("defensive"),
    ASSIST("assist"),
    AGGRESSIVE("aggressive");

    private final String id;

    CombatStance(String id) {
        this.id = id;
    }

    /**
     * Gets the unique identifier for this stance.
     * Used for persistence and network synchronization.
     *
     * @return The stance ID (e.g., "passive", "aggressive")
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the display name for this stance, suitable for UI.
     *
     * @return The translatable component for the stance name
     */
    public Component getDisplayName() {
        return Component.translatable("stance.immersivecompanions." + id);
    }

    /**
     * Gets the description for this stance, explaining its behavior.
     *
     * @return The translatable component for the stance description
     */
    public Component getDescription() {
        return Component.translatable("stance.immersivecompanions." + id + ".desc");
    }

    /**
     * Gets the next stance in the cycle.
     * Order: PASSIVE → DEFENSIVE → ASSIST → AGGRESSIVE → PASSIVE
     *
     * @return The next stance in the cycle
     */
    public CombatStance next() {
        CombatStance[] stances = values();
        return stances[(this.ordinal() + 1) % stances.length];
    }

    /**
     * Looks up a stance by its ID.
     *
     * @param id The stance ID
     * @return The stance, or AGGRESSIVE if not found
     */
    @Nullable
    public static CombatStance byId(String id) {
        for (CombatStance stance : values()) {
            if (stance.id.equals(id)) {
                return stance;
            }
        }
        return AGGRESSIVE;
    }
}
