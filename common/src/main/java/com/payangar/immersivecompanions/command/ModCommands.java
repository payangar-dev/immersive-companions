package com.payangar.immersivecompanions.command;

import com.mojang.brigadier.CommandDispatcher;
import com.payangar.immersivecompanions.entity.CompanionTeleportHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Debug commands for Immersive Companions mod.
 */
public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("immersivecompanions")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("registry")
                                .then(Commands.literal("list")
                                        .executes(context -> listRegistry(context.getSource()))
                                )
                        )
        );
    }

    private static int listRegistry(CommandSourceStack source) {
        Map<UUID, List<CompanionTeleportHandler.TrackedCompanionInfo>> snapshot =
                CompanionTeleportHandler.getRegistrySnapshot();

        if (snapshot.isEmpty()) {
            source.sendSuccess(() -> Component.literal("=== Companion Teleport Registry ==="), false);
            source.sendSuccess(() -> Component.literal("No companions currently tracked."), false);
            return 1;
        }

        MinecraftServer server = source.getServer();
        int totalCompanions = 0;
        int totalPlayers = 0;

        source.sendSuccess(() -> Component.literal("=== Companion Teleport Registry ==="), false);

        for (Map.Entry<UUID, List<CompanionTeleportHandler.TrackedCompanionInfo>> entry : snapshot.entrySet()) {
            UUID playerUuid = entry.getKey();
            List<CompanionTeleportHandler.TrackedCompanionInfo> companions = entry.getValue();

            // Try to get player name
            String playerName = getPlayerName(server, playerUuid);
            source.sendSuccess(() -> Component.literal("Player: " + playerName), false);

            for (CompanionTeleportHandler.TrackedCompanionInfo info : companions) {
                String companionIdShort = info.companionId().toString().substring(0, 8);
                String dimension = formatDimension(info.dimension());
                BlockPos pos = info.lastPos();

                source.sendSuccess(() -> Component.literal(
                        "  - Companion: " + companionIdShort + " | " + dimension +
                                " | [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"
                ), false);
                totalCompanions++;
            }
            totalPlayers++;
        }

        int finalTotalCompanions = totalCompanions;
        int finalTotalPlayers = totalPlayers;
        source.sendSuccess(() -> Component.literal(
                "Total: " + finalTotalCompanions + " companions tracked for " + finalTotalPlayers + " players"
        ), false);

        return 1;
    }

    private static String getPlayerName(MinecraftServer server, UUID playerUuid) {
        var player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            return player.getName().getString();
        }
        // Player offline - show shortened UUID
        return playerUuid.toString().substring(0, 8) + "...";
    }

    private static String formatDimension(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }
}
