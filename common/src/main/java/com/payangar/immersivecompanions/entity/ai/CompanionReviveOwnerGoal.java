package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.compat.hardcorerevival.HardcoreRevivalCompat;
import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal that allows companions to revive knocked-out owners when
 * Hardcore Revival mod is installed.
 *
 * Behavior:
 * - Detects when owner enters "knocked out" state (Hardcore Revival feature)
 * - Moves toward the owner if not within revival range
 * - Performs revival over a configurable duration (default 60 ticks / 3 seconds)
 * - Visual feedback: companion crouches and healing particles spawn
 *
 * This goal only activates when:
 * - Hardcore Revival mod is loaded
 * - Companion revival is enabled in config
 * - Companion has an owner who is knocked out
 * - Companion is not critically injured (combat disabled)
 */
public class CompanionReviveOwnerGoal extends Goal {

    /** Distance within which revival can occur (matches HR's rescue distance) */
    private static final double REVIVAL_RANGE = 3.0;
    private static final double REVIVAL_RANGE_SQ = REVIVAL_RANGE * REVIVAL_RANGE;

    /** How often to spawn particles during revival (in ticks) */
    private static final int PARTICLE_INTERVAL = 10;

    private final CompanionEntity companion;

    /** The owner we're trying to revive */
    private Player knockedOutOwner;

    /** Ticks spent reviving so far */
    private int revivingTicks;

    public CompanionReviveOwnerGoal(CompanionEntity companion) {
        this.companion = companion;
        // MOVE for pathfinding, LOOK for facing the owner
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Check if HR mod is loaded and feature is enabled
        if (!HardcoreRevivalCompat.isModLoaded()) {
            return false;
        }
        if (!ModConfig.enableCompanionRevival) {
            return false;
        }

        // Companion must have an owner
        Player owner = companion.getOwner();
        if (owner == null) {
            return false;
        }

        // Companion must not be critically injured (combat disabled)
        if (companion.isCombatDisabled()) {
            return false;
        }

        // Check if owner is knocked out
        if (!HardcoreRevivalCompat.isKnockedOut(owner)) {
            return false;
        }

        // Don't revive if another companion is already doing it
        if (isAnotherCompanionReviving(owner)) {
            return false;
        }

        this.knockedOutOwner = owner;
        return true;
    }

    /**
     * Checks if another companion is already reviving the same owner.
     * This prevents multiple companions from trying to revive simultaneously.
     */
    private boolean isAnotherCompanionReviving(Player owner) {
        List<CompanionEntity> nearbyCompanions = companion.level().getEntitiesOfClass(
                CompanionEntity.class,
                companion.getBoundingBox().inflate(32.0),
                other -> other != companion
                        && other.isRevivingOwner()
                        && owner.equals(other.getOwner())
        );

        return !nearbyCompanions.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if owner no longer exists or is no longer knocked out
        if (knockedOutOwner == null || !knockedOutOwner.isAlive()) {
            return false;
        }

        // Stop if owner is no longer knocked out (revived by someone else or died)
        if (!HardcoreRevivalCompat.isKnockedOut(knockedOutOwner)) {
            return false;
        }

        // Stop if companion becomes critically injured
        if (companion.isCombatDisabled()) {
            return false;
        }

        // Stop if feature disabled mid-revival
        if (!ModConfig.enableCompanionRevival) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.revivingTicks = 0;
    }

    @Override
    public void stop() {
        companion.setRevivingOwner(false);
        this.knockedOutOwner = null;
        this.revivingTicks = 0;
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (knockedOutOwner == null) {
            return;
        }

        // Always look at the owner
        companion.getLookControl().setLookAt(knockedOutOwner, 30.0F, 30.0F);

        double distanceSq = companion.distanceToSqr(knockedOutOwner);

        if (distanceSq > REVIVAL_RANGE_SQ) {
            // Move toward owner if too far
            companion.getNavigation().moveTo(knockedOutOwner, 1.0);

            // Not reviving yet - reset progress
            revivingTicks = 0;
        } else {
            // Within range - stop moving and start reviving
            companion.getNavigation().stop();
            companion.setRevivingOwner(true);

            // Increment revival progress
            revivingTicks++;

            // Spawn particles periodically
            if (revivingTicks % PARTICLE_INTERVAL == 0) {
                spawnRevivalParticles();
            }

            // Check if revival is complete
            if (revivingTicks >= ModConfig.companionRevivalTicks) {
                performRevival();
            }
        }
    }

    /**
     * Spawns healing/heart particles around the companion to indicate revival in progress.
     */
    private void spawnRevivalParticles() {
        if (!(companion.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Spawn particles between companion and owner
        double x = (companion.getX() + knockedOutOwner.getX()) / 2;
        double y = companion.getY() + companion.getBbHeight() * 0.5;
        double z = (companion.getZ() + knockedOutOwner.getZ()) / 2;

        // Spawn 2-3 heart particles with spread
        serverLevel.sendParticles(
                ParticleTypes.HEART,
                x, y, z,
                2, 0.3, 0.2, 0.3, 0
        );
    }

    /**
     * Completes the revival process by calling the Hardcore Revival API.
     */
    private void performRevival() {
        if (knockedOutOwner == null) {
            return;
        }

        boolean success = HardcoreRevivalCompat.wakeup(knockedOutOwner, true);

        if (success) {
            // Spawn a burst of particles on successful revival
            if (companion.level() instanceof ServerLevel serverLevel) {
                double x = knockedOutOwner.getX();
                double y = knockedOutOwner.getY() + knockedOutOwner.getBbHeight() * 0.5;
                double z = knockedOutOwner.getZ();

                serverLevel.sendParticles(
                        ParticleTypes.HEART,
                        x, y, z,
                        10, 0.5, 0.5, 0.5, 0
                );
            }
        }

        // Goal will stop on next canContinueToUse() check since owner is no longer knocked out
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
