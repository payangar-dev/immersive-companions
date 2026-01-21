package com.payangar.immersivecompanions.entity.condition;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.ai.CompanionFleeFromAttackerGoal;
import com.payangar.immersivecompanions.entity.mode.GoalEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Set;

/**
 * Condition applied when a companion's health drops below the critical injury threshold.
 *
 * <p>Effects:
 * <ul>
 *   <li>Blocks swimming, sleeping, and jumping</li>
 *   <li>Disables combat (companion won't attack)</li>
 *   <li>Adds flee behavior (runs from attackers)</li>
 *   <li>Applies movement speed penalty</li>
 *   <li>Forces crouching pose</li>
 * </ul>
 */
public class CriticalInjuryCondition implements CompanionCondition {

    public static final CriticalInjuryCondition INSTANCE = new CriticalInjuryCondition();

    private static final ResourceLocation SPEED_PENALTY_ID =
            ResourceLocation.fromNamespaceAndPath("immersivecompanions", "critical_injury_speed");

    private CriticalInjuryCondition() {}

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
                ActionType.JUMP
        );
    }

    // ========== Goal Modification ==========

    @Override
    public boolean disablesCombat() {
        return true;
    }

    @Override
    public List<GoalEntry> getBehaviorGoals() {
        // Add flee behavior when injured (high priority)
        return List.of(
                new GoalEntry(1, CompanionFleeFromAttackerGoal::new)
        );
    }

    // ========== Effects ==========

    @Override
    public boolean forcesCrouching() {
        return true;
    }

    @Override
    public void onApply(CompanionEntity entity) {
        // Apply speed penalty
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            // Remove existing modifier if present (safety)
            speed.removeModifier(SPEED_PENALTY_ID);

            // Create modifier based on config (e.g., 0.5 multiplier = -0.5 penalty)
            double penalty = -(1.0 - getSpeedMultiplier());
            AttributeModifier modifier = new AttributeModifier(
                    SPEED_PENALTY_ID, penalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addTransientModifier(modifier);
        }

        // Set crouching state so isCrouching() returns true
        entity.setShiftKeyDown(true);

        // Register condition goals
        entity.registerConditionGoals(this);
    }

    @Override
    public void onRemove(CompanionEntity entity) {
        // Remove speed penalty
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_PENALTY_ID);
        }

        // Clear crouching state
        entity.setShiftKeyDown(false);

        // Unregister condition goals
        entity.removeConditionGoals(this);
    }
}
