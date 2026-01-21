package com.payangar.immersivecompanions.entity.mode;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.Function;

/**
 * Record for declarative goal configuration in companion modes.
 * Allows modes to specify goals with priorities and factory functions.
 *
 * @param priority The goal priority (lower = higher priority)
 * @param factory  Factory function to create the goal for a specific companion
 */
public record GoalEntry(int priority, Function<CompanionEntity, Goal> factory) {}
