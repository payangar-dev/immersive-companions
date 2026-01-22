package com.payangar.immersivecompanions.mixin;

import com.payangar.immersivecompanions.entity.CompanionTeleportHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect player teleportation within the same dimension.
 * Triggers companion teleportation when owner uses /tp, waystones, etc.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportMixin {

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",
            at = @At("HEAD"))
    private void onTeleportTo(ServerLevel level, double x, double y, double z,
                               float yRot, float xRot, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        // Only handle same-dimension teleports (dimension changes handled by world change event)
        if (self.level().dimension().equals(level.dimension())) {
            CompanionTeleportHandler.onPlayerTeleport(self, level, new Vec3(x, y, z));
        }
    }
}
