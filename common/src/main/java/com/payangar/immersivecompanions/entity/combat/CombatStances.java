package com.payangar.immersivecompanions.entity.combat;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for combat stances.
 * Provides access to all available stances and cycling between them.
 */
public final class CombatStances {

    private static final Map<String, CombatStance> REGISTRY = new LinkedHashMap<>();

    public static final CombatStance PASSIVE = register(PassiveStance.INSTANCE);
    public static final CombatStance DEFENSIVE = register(DefensiveStance.INSTANCE);
    public static final CombatStance ASSIST = register(AssistStance.INSTANCE);
    public static final CombatStance AGGRESSIVE = register(AggressiveStance.INSTANCE);

    private CombatStances() {}

    /**
     * Registers a combat stance.
     *
     * @param stance The stance to register
     * @return The registered stance
     * @throws IllegalArgumentException if a stance with the same ID is already registered
     */
    public static CombatStance register(CombatStance stance) {
        if (REGISTRY.containsKey(stance.getId())) {
            throw new IllegalArgumentException("Combat stance already registered: " + stance.getId());
        }
        REGISTRY.put(stance.getId(), stance);
        return stance;
    }

    /**
     * Gets a combat stance by its ID.
     *
     * @param id The stance ID
     * @return The stance, or AGGRESSIVE if not found
     */
    public static CombatStance byId(String id) {
        return REGISTRY.getOrDefault(id, AGGRESSIVE);
    }

    /**
     * Gets all registered combat stances.
     *
     * @return Unmodifiable collection of all stances
     */
    public static Collection<CombatStance> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Gets the next stance in the cycle.
     * Order: PASSIVE → DEFENSIVE → ASSIST → AGGRESSIVE → PASSIVE
     *
     * @param current The current stance
     * @return The next stance in the cycle
     */
    public static CombatStance next(CombatStance current) {
        CombatStance[] stances = REGISTRY.values().toArray(new CombatStance[0]);
        for (int i = 0; i < stances.length; i++) {
            if (stances[i].getId().equals(current.getId())) {
                return stances[(i + 1) % stances.length];
            }
        }
        return PASSIVE;
    }
}
