package com.payangar.immersivecompanions.entity.condition;

/**
 * Basic entity actions that can be blocked by conditions.
 * These are fundamental capabilities, not goal-based behaviors.
 *
 * <p>Goal-based combat is controlled separately via
 * {@link CompanionCondition#disablesCombat()}.
 */
public enum ActionType {
    /** Swimming in water (FloatGoal) */
    SWIM,

    /** Sleeping in a bed */
    SLEEP,

    /** Jumping from the ground */
    JUMP,

    /** Sprinting (fast movement) */
    SPRINT
}
