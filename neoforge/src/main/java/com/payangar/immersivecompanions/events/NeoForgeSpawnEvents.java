package com.payangar.immersivecompanions.events;

import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.spawning.CompanionSpawnLogic;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;

/**
 * NeoForge event handlers for companion spawning in villages.
 */
@EventBusSubscriber(modid = ImmersiveCompanions.MOD_ID)
public class NeoForgeSpawnEvents {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();

        // Skip if already processed
        if (CompanionSpawnLogic.isChunkProcessed(chunkPos)) {
            return;
        }

        // Check for village structures in this chunk
        // We need to schedule this for later since entities might not be loaded yet
        serverLevel.getServer().execute(() -> checkForVillageAndSpawn(serverLevel, chunkPos));
    }

    private static void checkForVillageAndSpawn(ServerLevel level, ChunkPos chunkPos) {
        // Skip if already processed (double-check after scheduling)
        if (CompanionSpawnLogic.isChunkProcessed(chunkPos)) {
            return;
        }

        BlockPos checkPos = new BlockPos(chunkPos.getMinBlockX(), 64, chunkPos.getMinBlockZ());

        // Check structures at this position using getAllStructuresAt which returns Map<Structure, LongSet>
        Map<Structure, LongSet> structures = level.structureManager().getAllStructuresAt(checkPos);

        for (Map.Entry<Structure, LongSet> entry : structures.entrySet()) {
            Structure structure = entry.getKey();

            // Check if it's a village structure
            if (!isVillageStructure(structure, level)) {
                continue;
            }

            // Get the structure start at this position
            StructureStart start = level.structureManager().getStructureAt(checkPos, structure);
            if (start.isValid()) {
                BlockPos center = new BlockPos(
                        (start.getBoundingBox().minX() + start.getBoundingBox().maxX()) / 2,
                        start.getBoundingBox().minY(),
                        (start.getBoundingBox().minZ() + start.getBoundingBox().maxZ()) / 2
                );

                CompanionSpawnLogic.trySpawnInVillage(level, center);
                return;
            }
        }
    }

    private static boolean isVillageStructure(Structure structure, ServerLevel level) {
        // Check structure type by registry name
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var key = registry.getKey(structure);
        if (key != null) {
            String path = key.getPath();
            return path.contains("village");
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CompanionSpawnLogic.clearTrackedChunks();
    }
}
