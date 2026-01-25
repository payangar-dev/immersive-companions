package com.payangar.immersivecompanions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.CompanionTeleportHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Debug commands for Immersive Companions mod.
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("immersivecompanions")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("companions")
                                .then(Commands.literal("nearby")
                                        .executes(context -> listNearbyCompanions(context.getSource()))
                                )
                        )
        );
    }

    private static int listNearbyCompanions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }

        List<CompanionEntity> companions = CompanionTeleportHandler.getCompanionsForTeleport(player);

        source.sendSuccess(() -> Component.literal("=== Nearby Companions (Teleport-Ready) ==="), false);

        if (companions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No companions nearby that would teleport with you."), false);
            source.sendSuccess(() -> Component.literal("(Must be within 16 blocks, in FOLLOW mode, and owned by you)"), false);
            return 1;
        }

        for (CompanionEntity companion : companions) {
            String name = companion.getDisplayName().getString();
            String mode = companion.getMode().getId();
            boolean injured = companion.isCriticallyInjured();
            double distance = companion.distanceTo(player);

            source.sendSuccess(() -> Component.literal(String.format(
                    "  - %s | %s | %.1f blocks%s",
                    name, mode, distance, injured ? " [INJURED]" : ""
            )), false);
        }

        int count = companions.size();
        source.sendSuccess(() -> Component.literal(
                "Total: " + count + " companion(s) would teleport with you"
        ), false);

        return 1;
    }
}
