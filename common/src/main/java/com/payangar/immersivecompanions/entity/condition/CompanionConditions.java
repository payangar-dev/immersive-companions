package com.payangar.immersivecompanions.entity.condition;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of all possible companion conditions.
 *
 * <p>Conditions are registered at class load time and can be looked up by ID.
 * This provides a central place to define and access all conditions.
 */
public class CompanionConditions {

    private static final Map<String, CompanionCondition> REGISTRY = new LinkedHashMap<>();

    // ========== Registered Conditions ==========

    /** Critical injury condition - applied when health drops below threshold */
    public static final CompanionCondition CRITICAL_INJURY = register(CriticalInjuryCondition.INSTANCE);

    // Future conditions can be added here:
    // public static final CompanionCondition POISONED = register(PoisonedCondition.INSTANCE);
    // public static final CompanionCondition STUNNED = register(StunnedCondition.INSTANCE);
    // public static final CompanionCondition EXHAUSTED = register(ExhaustedCondition.INSTANCE);

    // ========== Registry Methods ==========

    /**
     * Registers a condition in the registry.
     *
     * @param condition The condition to register
     * @return The registered condition (for static field assignment)
     */
    private static CompanionCondition register(CompanionCondition condition) {
        REGISTRY.put(condition.getId(), condition);
        return condition;
    }

    /**
     * Looks up a condition by its ID.
     *
     * @param id The condition ID
     * @return The condition, or null if not found
     */
    @Nullable
    public static CompanionCondition byId(String id) {
        return REGISTRY.get(id);
    }

    /**
     * Gets all registered conditions.
     *
     * @return Unmodifiable collection of all conditions
     */
    public static Collection<CompanionCondition> all() {
        return REGISTRY.values();
    }
}
