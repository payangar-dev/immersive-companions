package com.payangar.immersivecompanions.entity.mode;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Sealed interface defining the contract for companion behavioral modes.
 * Each mode encapsulates its own behavior through declarative goal registration.
 *
 * <p>Design Pattern: State Pattern with singleton mode classes.
 * Each mode is stateless and can be safely shared across all companion instances.</p>
 */
public sealed interface CompanionMode permits WanderMode, FollowMode {

    /**
     * Gets the unique identifier for this mode.
     * Used for NBT persistence and network sync.
     *
     * @return The mode's unique string ID
     */
    String getId();

    /**
     * Gets the list of goals this mode provides.
     * Goals will be added to the companion's goal selector when entering this mode.
     *
     * @return List of goal entries with priorities and factories
     */
    List<GoalEntry> getGoals();

    /**
     * Called when a companion enters this mode.
     * Override to perform mode-specific initialization.
     *
     * @param companion The companion entering this mode
     */
    default void onEnter(CompanionEntity companion) {}

    /**
     * Called when a companion exits this mode.
     * Override to perform mode-specific cleanup.
     *
     * @param companion The companion exiting this mode
     */
    default void onExit(CompanionEntity companion) {}

    /**
     * Called every tick while the companion is in this mode.
     * Override for mode-specific per-tick behavior.
     *
     * @param companion The companion in this mode
     */
    default void tick(CompanionEntity companion) {}

    /**
     * Checks if a transition to the target mode is allowed.
     * Override to implement mode-specific transition rules.
     *
     * @param target    The mode being transitioned to
     * @param companion The companion attempting the transition
     * @return true if the transition is allowed
     */
    default boolean canTransitionTo(CompanionMode target, CompanionEntity companion) {
        return true;
    }

    /**
     * Gets the display name for this mode, used in UI.
     *
     * @return The translatable display name component
     */
    default Component getDisplayName() {
        return Component.translatable("mode.immersivecompanions." + getId());
    }

    /**
     * Looks up a mode by its string ID.
     *
     * @param id The mode ID to look up
     * @return The corresponding mode, or WANDER as fallback
     */
    static CompanionMode byId(String id) {
        return CompanionModes.byId(id);
    }
}
