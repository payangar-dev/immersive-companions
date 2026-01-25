package com.payangar.immersivecompanions.entity.condition;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;

import java.util.Set;

/**
 * Condition applied when a companion's health drops below the critical injury
 * threshold.
 *
 * <p>Effects:
 * <ul>
 *   <li>Blocks swimming, sleeping, and jumping</li>
 *   <li>Disables combat (companion won't attack)</li>
 *   <li>Triggers flee behavior (via shouldFlee() check in CompanionFleeFromAttackerGoal)</li>
 *   <li>Forces crouching pose</li>
 * </ul>
 *
 * <p>Goals check the companion's state via convenience methods (shouldFlee(),
 * isCombatDisabled()) rather than this condition providing goals directly.
 */
public class CriticalInjuryCondition implements CompanionCondition {

    public static final CriticalInjuryCondition INSTANCE = new CriticalInjuryCondition();

    private CriticalInjuryCondition() {
    }

    @Override
    public String getId() {
        return "critical_injury";
    }

    // ========== Config Integration ==========

    @Override
    public boolean isEnabled() {
        return ModConfig.get().isEnableCriticalInjury();
    }

    /**
     * Gets the health threshold for entering critical injury state.
     *
     * @return The threshold in half-hearts
     */
    public float getThreshold() {
        return ModConfig.get().getCriticalInjuryThreshold();
    }

    /**
     * Gets the movement speed multiplier when critically injured.
     *
     * @return The speed multiplier (0.0 to 1.0)
     */
    public float getSpeedMultiplier() {
        return ModConfig.get().getCriticalInjurySpeedMultiplier();
    }

    // ========== Action Blocking ==========

    @Override
    public Set<ActionType> getBlockedActions() {
        return Set.of(
                ActionType.SWIM,
                ActionType.SLEEP,
                ActionType.JUMP,
                ActionType.SPRINT);
    }

    @Override
    public boolean disablesCombat() {
        return true;
    }

    // ========== Lifecycle Hooks ==========

    @Override
    public void onApply(CompanionEntity entity) {
        // Start sneaking when critically injured
        entity.startSneaking();
    }

    @Override
    public void onRemove(CompanionEntity entity) {
        entity.stopSneaking();
    }

    @Override
    public void tick(CompanionEntity entity) {
        // Ensure crouching state is maintained
        if (!entity.isCrouching()) {
            entity.startSneaking();
        }
    }
}
