package com.payangar.immersivecompanions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.payangar.immersivecompanions.ImmersiveCompanions;
import com.payangar.immersivecompanions.platform.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Immersive Companions mod.
 * Loaded from immersivecompanions.json in the config directory.
 * Config values are public static for easy access from YACL GUI.
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "immersivecompanions.json";

    private static final ModConfig INSTANCE = new ModConfig();

    // Config values (public static for YACL binding)
    public static boolean enableCriticalInjury = true;
    public static float criticalInjuryThreshold = 4.0f;
    public static float criticalInjurySpeedMultiplier = 0.5f;
    public static boolean enableMonstersTargetCompanions = true;
    public static boolean enableTeamCoordination = true;
    public static float teamCoordinationRange = 16.0f;
    public static boolean enableCompanionRevival = true;
    public static int companionRevivalTicks = 60;
    public static boolean enableAgony = true;
    public static int agonyDurationTicks = 1200;
    public static int agonyHitsBeforeDeath = 3;
    public static int reviveDurationTicks = 100;

    // Internal class for JSON serialization
    private static class ConfigData {
        boolean enableCriticalInjury = true;
        float criticalInjuryThreshold = 4.0f;
        float criticalInjurySpeedMultiplier = 0.5f;
        boolean enableMonstersTargetCompanions = true;
        boolean enableTeamCoordination = true;
        float teamCoordinationRange = 16.0f;
        boolean enableCompanionRevival = true;
        int companionRevivalTicks = 60;
        boolean enableAgony = true;
        int agonyDurationTicks = 1200;
        int agonyHitsBeforeDeath = 3;
        int reviveDurationTicks = 100;
    }

    /**
     * Gets the config instance for backwards compatibility.
     */
    public static ModConfig get() {
        return INSTANCE;
    }

    /**
     * Whether the critical injury system is enabled.
     * When enabled, companions will enter a weakened state at low health.
     */
    public boolean isEnableCriticalInjury() {
        return enableCriticalInjury;
    }

    /**
     * Health threshold for critical injury (in half-hearts).
     * Default is 4.0 (2 hearts).
     */
    public float getCriticalInjuryThreshold() {
        return criticalInjuryThreshold;
    }

    /**
     * Movement speed multiplier when critically injured.
     * Default is 0.5 (50% of normal speed).
     */
    public float getCriticalInjurySpeedMultiplier() {
        return criticalInjurySpeedMultiplier;
    }

    /**
     * Whether monsters should proactively target companions.
     * When enabled, hostile mobs (except creepers and endermen) will attack companions on sight.
     */
    public boolean isEnableMonstersTargetCompanions() {
        return enableMonstersTargetCompanions;
    }

    /**
     * Whether team coordination is enabled.
     * When enabled, companions will defend teammates being attacked and assist teammates in combat.
     */
    public boolean isEnableTeamCoordination() {
        return enableTeamCoordination;
    }

    /**
     * Detection range for team coordination in blocks.
     * Default is 16.0 blocks.
     */
    public float getTeamCoordinationRange() {
        return teamCoordinationRange;
    }

    /**
     * Whether the agony system is enabled.
     * When enabled, companions enter a downed state instead of dying immediately.
     */
    public boolean isEnableAgony() {
        return enableAgony;
    }

    /**
     * Duration of the agony state in ticks before death.
     * Default is 1200 (60 seconds).
     */
    public int getAgonyDurationTicks() {
        return agonyDurationTicks;
    }

    /**
     * Number of hits an agonizing companion can take before dying.
     * Default is 3.
     */
    public int getAgonyHitsBeforeDeath() {
        return agonyHitsBeforeDeath;
    }

    /**
     * Duration in ticks a player must hold right-click to revive.
     * Default is 100 (5 seconds).
     */
    public int getReviveDurationTicks() {
        return reviveDurationTicks;
    }

    /**
     * Loads the config from file, or creates default config if not found.
     * Should be called during mod initialization.
     */
    public static void load() {
        Path configPath = Services.get().getConfigDir().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    enableCriticalInjury = data.enableCriticalInjury;
                    criticalInjuryThreshold = data.criticalInjuryThreshold;
                    criticalInjurySpeedMultiplier = data.criticalInjurySpeedMultiplier;
                    enableMonstersTargetCompanions = data.enableMonstersTargetCompanions;
                    enableTeamCoordination = data.enableTeamCoordination;
                    teamCoordinationRange = data.teamCoordinationRange;
                    enableCompanionRevival = data.enableCompanionRevival;
                    companionRevivalTicks = data.companionRevivalTicks;
                    enableAgony = data.enableAgony;
                    agonyDurationTicks = data.agonyDurationTicks;
                    agonyHitsBeforeDeath = data.agonyHitsBeforeDeath;
                    reviveDurationTicks = data.reviveDurationTicks;
                }
                ImmersiveCompanions.LOGGER.info("Loaded config from {}", configPath);
            } catch (IOException e) {
                ImmersiveCompanions.LOGGER.error("Failed to load config, using defaults", e);
                save();
            }
        } else {
            ImmersiveCompanions.LOGGER.info("Config not found, creating default config at {}", configPath);
            save();
        }
    }

    /**
     * Saves the current config to file.
     */
    public static void save() {
        Path configPath = Services.get().getConfigDir().resolve(CONFIG_FILE_NAME);

        try {
            Files.createDirectories(configPath.getParent());
            ConfigData data = new ConfigData();
            data.enableCriticalInjury = enableCriticalInjury;
            data.criticalInjuryThreshold = criticalInjuryThreshold;
            data.criticalInjurySpeedMultiplier = criticalInjurySpeedMultiplier;
            data.enableMonstersTargetCompanions = enableMonstersTargetCompanions;
            data.enableTeamCoordination = enableTeamCoordination;
            data.teamCoordinationRange = teamCoordinationRange;
            data.enableCompanionRevival = enableCompanionRevival;
            data.companionRevivalTicks = companionRevivalTicks;
            data.enableAgony = enableAgony;
            data.agonyDurationTicks = agonyDurationTicks;
            data.agonyHitsBeforeDeath = agonyHitsBeforeDeath;
            data.reviveDurationTicks = reviveDurationTicks;
            String json = GSON.toJson(data);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            ImmersiveCompanions.LOGGER.error("Failed to save config", e);
        }
    }
}
