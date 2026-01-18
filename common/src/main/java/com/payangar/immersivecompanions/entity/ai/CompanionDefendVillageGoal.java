package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

/**
 * AI goal that makes companions defend villages by targeting players
 * who have attacked villagers (tracked through the gossip system).
 */
public class CompanionDefendVillageGoal extends TargetGoal {

    private static final int REPUTATION_THRESHOLD = -100;
    private static final double SEARCH_RANGE = 16.0;
    private static final int CHECK_INTERVAL = 20; // Check every second

    private final CompanionEntity companion;
    private final TargetingConditions targetConditions;

    @Nullable
    private Player targetPlayer;
    private int checkTimer;

    public CompanionDefendVillageGoal(CompanionEntity companion) {
        super(companion, false, false);
        this.companion = companion;
        this.targetConditions = TargetingConditions.forCombat()
                .range(SEARCH_RANGE)
                .ignoreLineOfSight();
    }

    @Override
    public boolean canUse() {
        if (--checkTimer > 0) {
            return false;
        }
        checkTimer = CHECK_INTERVAL;

        if (!(companion.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        targetPlayer = findHostilePlayer(serverLevel);
        return targetPlayer != null;
    }

    @Override
    public void start() {
        companion.setTarget(targetPlayer);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = companion.getTarget();
        if (target instanceof Player player) {
            return player.isAlive() && companion.distanceToSqr(player) < SEARCH_RANGE * SEARCH_RANGE;
        }
        return false;
    }

    @Override
    public void stop() {
        targetPlayer = null;
        companion.setTarget(null);
        super.stop();
    }

    @Nullable
    private Player findHostilePlayer(ServerLevel level) {
        AABB searchBox = companion.getBoundingBox().inflate(SEARCH_RANGE);
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(Villager.class, searchBox);

        if (nearbyVillagers.isEmpty()) {
            return null;
        }

        // Find players in range
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, searchBox);

        for (Player player : nearbyPlayers) {
            if (!targetConditions.test(companion, player)) {
                continue;
            }

            // Check if any villager has bad reputation for this player
            int totalReputation = 0;
            for (Villager villager : nearbyVillagers) {
                totalReputation += getPlayerReputation(villager, player);
            }

            // Average reputation across villagers
            int avgReputation = totalReputation / nearbyVillagers.size();

            if (avgReputation <= REPUTATION_THRESHOLD) {
                return player;
            }
        }

        return null;
    }

    private int getPlayerReputation(Villager villager, Player player) {
        // Get gossip-based reputation
        int reputation = 0;

        // Negative gossip types reduce reputation
        reputation -= villager.getGossips().getReputation(player.getUUID(), gossipType ->
                gossipType == GossipType.MINOR_NEGATIVE || gossipType == GossipType.MAJOR_NEGATIVE);

        // Positive gossip types increase reputation (but we care about negative)
        reputation += villager.getGossips().getReputation(player.getUUID(), gossipType ->
                gossipType == GossipType.MINOR_POSITIVE || gossipType == GossipType.MAJOR_POSITIVE ||
                        gossipType == GossipType.TRADING);

        return reputation;
    }
}
