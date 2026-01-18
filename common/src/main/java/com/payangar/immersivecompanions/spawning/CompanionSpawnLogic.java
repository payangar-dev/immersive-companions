package com.payangar.immersivecompanions.spawning;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles companion spawning logic in villages.
 * Spawn chance scales with villager count.
 */
public class CompanionSpawnLogic {

    private static final Set<ChunkPos> processedChunks = new HashSet<>();
    private static final int SEARCH_RADIUS = 64;
    private static final int SPAWN_RADIUS = 32;

    /**
     * Calculates spawn chance based on villager count.
     * 0 villagers or less than 3: 0% chance
     * 3 villagers: 10% chance
     * 10+ villagers: 40% chance
     * Linear interpolation between 3 and 10.
     */
    public static float calculateSpawnChance(int villagerCount) {
        if (villagerCount < 3) {
            return 0f;
        }
        int capped = Math.min(villagerCount, 10);
        return 0.1f + (capped - 3) * (0.3f / 7f);
    }

    /**
     * Determines how many companions to spawn.
     *
     * @param villagerCount Number of villagers nearby
     * @param random        Random source
     * @return 0, 1, or 2 companions to spawn
     */
    public static int determineSpawnCount(int villagerCount, RandomSource random) {
        float chance = calculateSpawnChance(villagerCount);
        if (random.nextFloat() >= chance) {
            return 0; // First roll failed
        }
        // With 10+ villagers, chance for second companion
        if (villagerCount >= 10 && random.nextFloat() < chance) {
            return 2;
        }
        return 1;
    }

    /**
     * Attempts to spawn companions in a village area.
     * Call this when a village structure is detected.
     *
     * @param level     The server level
     * @param centerPos The approximate center of the village
     */
    public static void trySpawnInVillage(ServerLevel level, BlockPos centerPos) {
        ChunkPos chunkPos = new ChunkPos(centerPos);

        // Check if we've already processed this chunk
        if (processedChunks.contains(chunkPos)) {
            return;
        }
        processedChunks.add(chunkPos);

        // Count villagers in the area
        AABB searchBox = new AABB(centerPos).inflate(SEARCH_RADIUS);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, searchBox);
        int villagerCount = villagers.size();

        if (villagerCount < 3) {
            return;
        }

        // Check if companions already exist in this area
        List<CompanionEntity> existingCompanions = level.getEntitiesOfClass(CompanionEntity.class, searchBox);
        if (!existingCompanions.isEmpty()) {
            return;
        }

        // Determine spawn count
        RandomSource random = level.getRandom();
        int spawnCount = determineSpawnCount(villagerCount, random);

        if (spawnCount == 0) {
            return;
        }

        // Spawn companions
        for (int i = 0; i < spawnCount; i++) {
            BlockPos spawnPos = findValidSpawnPosition(level, centerPos, random);
            if (spawnPos != null) {
                spawnCompanion(level, spawnPos);
            }
        }

        ImmersiveCompanions.LOGGER.debug("Spawned {} companion(s) in village at {} with {} villagers",
                spawnCount, centerPos, villagerCount);
    }

    /**
     * Finds a valid spawn position within the village area.
     */
    private static BlockPos findValidSpawnPosition(ServerLevel level, BlockPos center, RandomSource random) {
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = center.getX() + random.nextIntBetweenInclusive(-SPAWN_RADIUS, SPAWN_RADIUS);
            int z = center.getZ() + random.nextIntBetweenInclusive(-SPAWN_RADIUS, SPAWN_RADIUS);

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            // Check if position is valid for spawning
            if (level.getBlockState(pos.below()).isSolid() &&
                    level.getBlockState(pos).isAir() &&
                    level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Spawns a companion at the specified position.
     */
    private static void spawnCompanion(ServerLevel level, BlockPos pos) {
        CompanionEntity companion = ModEntityTypes.getCompanion().create(level);
        if (companion != null) {
            companion.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            companion.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(companion);
        }
    }

    /**
     * Clears tracked chunks. Call on server stop.
     */
    public static void clearTrackedChunks() {
        processedChunks.clear();
    }

    /**
     * Checks if a chunk position has been processed.
     */
    public static boolean isChunkProcessed(ChunkPos pos) {
        return processedChunks.contains(pos);
    }

    /**
     * Marks a chunk as processed.
     */
    public static void markChunkProcessed(ChunkPos pos) {
        processedChunks.add(pos);
    }
}
