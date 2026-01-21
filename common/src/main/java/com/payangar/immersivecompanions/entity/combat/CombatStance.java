package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.GoalEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Sealed interface representing a combat stance that controls companion targeting behavior.
 * Each stance defines which targets the companion will engage and any custom behaviors.
 *
 * <p>Combat stances are independent of movement modes (follow/wander) and control:
 * <ul>
 *   <li>Target selection (who to attack)</li>
 *   <li>Custom behaviors (like fleeing for passive stance)</li>
 * </ul>
 *
 * <p>Stances follow the Strategy pattern with singleton instances.
 */
public sealed interface CombatStance permits PassiveStance, DefensiveStance, AssistStance, AggressiveStance {

    /**
     * Gets the unique identifier for this stance.
     * Used for persistence and network synchronization.
     *
     * @return The stance ID (e.g., "passive", "aggressive")
     */
    String getId();

    /**
     * Gets the target goals that determine who/when to attack.
     * These are added to the targetSelector when this stance is active.
     *
     * @return List of target goal entries with priorities
     */
    List<TargetGoalEntry> getTargetGoals();

    /**
     * Gets additional behavior goals specific to this stance.
     * For example, passive stance adds a flee behavior.
     * These are added to the goalSelector when this stance is active.
     *
     * @return List of behavior goal entries with priorities
     */
    default List<GoalEntry> getBehaviorGoals() {
        return List.of();
    }

    /**
     * Called when entering this stance.
     * Override to perform initialization like clearing targets.
     *
     * @param companion The companion entering this stance
     */
    default void onEnter(CompanionEntity companion) {}

    /**
     * Called when leaving this stance.
     * Override to perform cleanup.
     *
     * @param companion The companion leaving this stance
     */
    default void onExit(CompanionEntity companion) {}

    /**
     * Gets the display name for this stance, suitable for UI.
     *
     * @return The translatable component for the stance name
     */
    default Component getDisplayName() {
        return Component.translatable("stance.immersivecompanions." + getId());
    }

    /**
     * Gets the description for this stance, explaining its behavior.
     *
     * @return The translatable component for the stance description
     */
    default Component getDescription() {
        return Component.translatable("stance.immersivecompanions." + getId() + ".desc");
    }

    /**
     * Looks up a stance by its ID.
     *
     * @param id The stance ID
     * @return The stance, or AGGRESSIVE if not found
     */
    static CombatStance byId(String id) {
        return CombatStances.byId(id);
    }
}
