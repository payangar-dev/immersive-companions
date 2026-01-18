package com.payangar.immersivecompanions.mixin;

import com.payangar.immersivecompanions.spawning.CompanionSpawnLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into village structure generation and spawn companions.
 */
@Mixin(StructureStart.class)
public abstract class VillageStructureMixin {

    @Shadow
    public abstract BoundingBox getBoundingBox();

    @Inject(method = "placeInChunk", at = @At("TAIL"))
    private void onPlaceInChunk(ServerLevel level, StructureManager structureManager,
                                 ChunkGenerator generator, RandomSource random, BoundingBox boundingBox,
                                 ChunkPos chunkPos, CallbackInfo ci) {
        StructureStart self = (StructureStart) (Object) this;

        // Check if this is a village structure
        if (!isVillageStructure(self, level)) {
            return;
        }

        // Skip if already processed
        if (CompanionSpawnLogic.isChunkProcessed(chunkPos)) {
            return;
        }

        // Schedule spawn for later (after chunk is fully loaded)
        BoundingBox fullBox = getBoundingBox();
        BlockPos center = new BlockPos(
                (fullBox.minX() + fullBox.maxX()) / 2,
                fullBox.minY(),
                (fullBox.minZ() + fullBox.maxZ()) / 2
        );

        // Mark as processed
        CompanionSpawnLogic.markChunkProcessed(chunkPos);

        // Schedule spawn check for when entities are loaded
        level.getServer().execute(() -> {
            // Delay slightly to allow villagers to spawn
            level.getServer().execute(() -> {
                CompanionSpawnLogic.trySpawnInVillage(level, center);
            });
        });
    }

    private boolean isVillageStructure(StructureStart start, ServerLevel level) {
        var structure = start.getStructure();
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var key = registry.getKey(structure);
        if (key != null) {
            String path = key.getPath();
            return path.contains("village");
        }
        return false;
    }
}
