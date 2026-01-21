package com.payangar.immersivecompanions.entity.mode;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for all companion modes.
 * Provides mode lookup by ID and iteration over all registered modes.
 */
public final class CompanionModes {

    private static final Map<String, CompanionMode> REGISTRY = new LinkedHashMap<>();

    /** Default mode for unbought companions - wanders around village */
    public static final CompanionMode WANDER = register(WanderMode.INSTANCE);

    /** Mode for owned companions - follows their owner */
    public static final CompanionMode FOLLOW = register(FollowMode.INSTANCE);

    private CompanionModes() {}

    /**
     * Registers a mode in the registry.
     *
     * @param mode The mode to register
     * @return The registered mode (for chaining)
     */
    public static CompanionMode register(CompanionMode mode) {
        if (REGISTRY.containsKey(mode.getId())) {
            throw new IllegalArgumentException("Mode already registered: " + mode.getId());
        }
        REGISTRY.put(mode.getId(), mode);
        return mode;
    }

    /**
     * Looks up a mode by its string ID.
     *
     * @param id The mode ID to look up
     * @return The corresponding mode, or WANDER as fallback
     */
    public static CompanionMode byId(String id) {
        return REGISTRY.getOrDefault(id, WANDER);
    }

    /**
     * Gets all registered modes.
     *
     * @return Unmodifiable collection of all modes
     */
    public static Collection<CompanionMode> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}
