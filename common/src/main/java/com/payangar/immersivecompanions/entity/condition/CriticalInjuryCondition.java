package com.payangar.immersivecompanions.entity.condition;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

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

    private static final int HURT_BY_ALLY_MESSAGE_COUNT = 5;
    private static final int HURT_BY_ENEMY_PLAYER_MESSAGE_COUNT = 5;
    private static final int HURT_BY_MOB_MESSAGE_COUNT = 5;
    private static final double INJURY_MESSAGE_RADIUS = 32.0;

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
        broadcastInjuryMessage(entity);
    }

    /**
     * Broadcasts an injury message to nearby players when critically injured.
     * Message type depends on attacker: owner/ally, other player, or mob.
     */
    private void broadcastInjuryMessage(CompanionEntity entity) {
        if (entity.level().isClientSide) return;

        // Get the attacker from Minecraft's tracking
        LivingEntity attacker = entity.getLastHurtByMob();

        String messageKey;
        int messageCount;

        if (attacker instanceof Player player) {
            // Check if the player is the companion's owner (ally)
            if (entity.isOwnedBy(player)) {
                // Betrayal - attacked by own ally
                messageKey = "chat.immersivecompanions.hurt_by_ally.";
                messageCount = HURT_BY_ALLY_MESSAGE_COUNT;
            } else {
                // Surrender - attacked by enemy player
                messageKey = "chat.immersivecompanions.hurt_by_enemy_player.";
                messageCount = HURT_BY_ENEMY_PLAYER_MESSAGE_COUNT;
            }
        } else if (attacker instanceof Mob) {
            // Help request - attacked by mob
            messageKey = "chat.immersivecompanions.hurt_by_mob.";
            messageCount = HURT_BY_MOB_MESSAGE_COUNT;
        } else {
            return; // Environmental damage or unknown - no message
        }

        int messageIndex = entity.getRandom().nextInt(messageCount) + 1;
        Component message = Component.translatable(
                messageKey + messageIndex, entity.getDisplayName().getString());

        // Broadcast to nearby players
        if (entity.level() instanceof ServerLevel serverLevel) {
            AABB searchBox = entity.getBoundingBox().inflate(INJURY_MESSAGE_RADIUS);
            for (Player nearbyPlayer : serverLevel.getEntitiesOfClass(Player.class, searchBox)) {
                nearbyPlayer.sendSystemMessage(message);
            }
        }
    }
}
