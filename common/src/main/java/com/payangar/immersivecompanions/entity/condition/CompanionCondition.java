package com.payangar.immersivecompanions.entity.condition;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.combat.TargetGoalEntry;
import com.payangar.immersivecompanions.entity.mode.GoalEntry;

import java.util.List;
import java.util.Set;

/**
 * Interface for conditions that affect companion behavior.
 *
 * <p>Conditions represent states the entity is in (injured, poisoned, etc.)
 * that affect what actions are possible. Actions check conditions in their
 * {@code canUse()} methods, ensuring blocked actions never start.
 *
 * <p>This is the correct abstraction: conditions control what's possible,
 * and poses are just the consequence of whatever action is active.
 */
public interface CompanionCondition {

    /**
     * Gets the unique identifier for this condition.
     *
     * @return The condition ID
     */
    String getId();

    /**
     * Checks if this condition is enabled in config.
     * Disabled conditions will not be applied even when triggered.
     *
     * @return true if the condition is enabled
     */
    boolean isEnabled();

    // ========== Action Blocking ==========

    /**
     * Gets the set of basic actions blocked by this condition.
     * Actions like SWIM, SLEEP, JUMP can be blocked here.
     *
     * @return Set of blocked action types (empty by default)
     */
    default Set<ActionType> getBlockedActions() {
        return Set.of();
    }

    // ========== Goal Modification ==========

    /**
     * Gets behavior goals to add while this condition is active.
     * For example, adding a flee goal when injured.
     *
     * @return List of goal entries to add (empty by default)
     */
    default List<GoalEntry> getBehaviorGoals() {
        return List.of();
    }

    /**
     * Gets target goals to add while this condition is active.
     * Rarely needed, but possible for special conditions.
     *
     * @return List of target goal entries to add (empty by default)
     */
    default List<TargetGoalEntry> getTargetGoals() {
        return List.of();
    }

    /**
     * Checks if this condition disables combat targeting entirely.
     * If true, target goals from the current stance won't run.
     *
     * @return true if combat is disabled
     */
    default boolean disablesCombat() {
        return false;
    }

    // ========== Effects ==========

    /**
     * Called when the condition is applied to an entity.
     * Use this for one-time effects like applying attribute modifiers.
     *
     * @param entity The companion entity
     */
    default void onApply(CompanionEntity entity) {}

    /**
     * Called when the condition is removed from an entity.
     * Use this to clean up effects applied in {@link #onApply}.
     *
     * @param entity The companion entity
     */
    default void onRemove(CompanionEntity entity) {}

    /**
     * Called every tick while the condition is active.
     * Use for ongoing effects that need continuous application.
     *
     * @param entity The companion entity
     */
    default void tick(CompanionEntity entity) {}

    /**
     * Checks if this condition forces the companion to crouch.
     * Used for conditions like critical injury that require crouching.
     *
     * @return true if the companion should be forced to crouch
     */
    default boolean forcesCrouching() {
        return false;
    }
}
