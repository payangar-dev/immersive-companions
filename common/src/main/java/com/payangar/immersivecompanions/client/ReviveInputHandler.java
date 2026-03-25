package com.payangar.immersivecompanions.client;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Client-side handler that detects when the player is holding right-click
 * on an agonizing companion and sends revive tick packets to the server.
 * Also displays a blinking hint when looking at an agonizing companion.
 *
 * <p>Called each client tick from platform-specific event handlers.
 */
public class ReviveInputHandler {

    /** Maximum distance squared for revive interaction (3 blocks). */
    private static final double REVIVE_RANGE_SQ = 9.0;

    /** Blink cycle: visible for 15 ticks, hidden for 5 ticks. */
    private static final int BLINK_CYCLE = 20;
    private static final int BLINK_VISIBLE = 15;

    private static int hintTickCounter = 0;
    private static boolean wasShowingHint = false;

    /**
     * Called each client tick. Handles revive packet sending and hint display.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        boolean rightClickHeld = mc.options.keyUse.isDown();

        if (mc.hitResult instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof CompanionEntity companion
                    && companion.isAgonizing()
                    && mc.player.distanceToSqr(companion) <= REVIVE_RANGE_SQ) {
                if (rightClickHeld) {
                    ModNetworking.get().sendReviveCompanionTick(companion.getId());
                    wasShowingHint = false;
                    hintTickCounter = 0;
                } else {
                    // Blinking hint
                    hintTickCounter++;
                    if (hintTickCounter % BLINK_CYCLE < BLINK_VISIBLE) {
                        mc.player.displayClientMessage(
                                Component.translatable("action.immersivecompanions.revive_hint",
                                        mc.options.keyUse.getTranslatedKeyMessage()),
                                true);
                    }
                    wasShowingHint = true;
                }
                return;
            }
        }

        // Not looking at an agonizing companion — clear hint immediately
        if (wasShowingHint) {
            mc.player.displayClientMessage(Component.empty(), true);
            wasShowingHint = false;
        }
        hintTickCounter = 0;
    }
}
