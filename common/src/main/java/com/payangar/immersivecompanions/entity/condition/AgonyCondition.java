package com.payangar.immersivecompanions.entity.condition;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Set;

/**
 * Condition applied when a companion would die, entering a downed/agony state.
 *
 * <p>Effects:
 * <ul>
 *   <li>Blocks all actions (swimming, sleeping, jumping, sprinting)</li>
 *   <li>Disables combat</li>
 *   <li>Companion lies on the ground (SLEEPING pose)</li>
 *   <li>SOUL particles intensify as death approaches</li>
 *   <li>Can be revived by any player holding right-click</li>
 * </ul>
 */
public class AgonyCondition implements CompanionCondition {

    public static final AgonyCondition INSTANCE = new AgonyCondition();

    private static final int HURT_BY_ALLY_MESSAGE_COUNT = 5;
    private static final int HURT_BY_ENEMY_PLAYER_MESSAGE_COUNT = 5;
    private static final int HURT_BY_MOB_MESSAGE_COUNT = 5;
    private static final double AGONY_MESSAGE_RADIUS = 32.0;

    private AgonyCondition() {
    }

    @Override
    public String getId() {
        return "agony";
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.get().isEnableAgony();
    }

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
        // Remove critical injury - agony supersedes it
        if (entity.isCriticallyInjured()) {
            entity.setCriticallyInjured(false);
        }

        // Force dismount
        if (entity.isPassenger()) {
            entity.stopRiding();
        }

        // Stop all movement and targeting
        entity.getNavigation().stop();
        entity.setTarget(null);

        // Lie on the ground (flat on belly)
        entity.setPose(Pose.SWIMMING);

        // Broadcast distress messages
        broadcastAgonyMessage(entity);
    }

    @Override
    public void onRemove(CompanionEntity entity) {
        if (entity.getPose() == Pose.SWIMMING) {
            entity.setPose(Pose.STANDING);
        }
    }

    @Override
    public void tick(CompanionEntity entity) {
        if (entity.level().isClientSide) return;

        int remaining = entity.getAgonyTicksRemaining();
        int total = ModConfig.get().getAgonyDurationTicks();
        if (total <= 0) return;

        // Calculate urgency (0.0 at start, 1.0 near death)
        float urgency = 1.0f - (float) remaining / total;

        // Particle spawn interval decreases with urgency (20 ticks -> 5 ticks)
        int interval = Math.max(5, 20 - (int) (urgency * 15));
        // Particle count increases with urgency (1 -> 5)
        int count = 1 + (int) (urgency * 4);

        if (entity.tickCount % interval == 0 && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    entity.getX(), entity.getY() + 0.2, entity.getZ(),
                    count, 0.3, 0.1, 0.3, 0.02);
        }
    }

    /**
     * Broadcasts an agony message to nearby players.
     * Message type depends on attacker: owner/ally, other player, or mob.
     */
    private void broadcastAgonyMessage(CompanionEntity entity) {
        if (entity.level().isClientSide) return;

        LivingEntity attacker = entity.getLastHurtByMob();

        String messageKey;
        int messageCount;

        if (attacker instanceof Player player) {
            if (entity.isOwnedBy(player)) {
                messageKey = "chat.immersivecompanions.agony.hurt_by_ally.";
                messageCount = HURT_BY_ALLY_MESSAGE_COUNT;
            } else {
                messageKey = "chat.immersivecompanions.agony.hurt_by_enemy_player.";
                messageCount = HURT_BY_ENEMY_PLAYER_MESSAGE_COUNT;
            }
        } else if (attacker instanceof Mob) {
            messageKey = "chat.immersivecompanions.agony.hurt_by_mob.";
            messageCount = HURT_BY_MOB_MESSAGE_COUNT;
        } else {
            return;
        }

        int messageIndex = entity.getRandom().nextInt(messageCount) + 1;
        Component message = Component.translatable(
                messageKey + messageIndex, entity.getDisplayName().getString());

        if (entity.level() instanceof ServerLevel serverLevel) {
            AABB searchBox = entity.getBoundingBox().inflate(AGONY_MESSAGE_RADIUS);
            for (Player nearbyPlayer : serverLevel.getEntitiesOfClass(Player.class, searchBox)) {
                nearbyPlayer.sendSystemMessage(message);
            }
        }
    }
}
