package com.payangar.immersivecompanions.entity.combat;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.function.Function;

/**
 * Record for declarative target goal configuration in combat stances.
 * Allows stances to specify target goals with priorities and factory functions.
 *
 * @param priority The goal priority (lower = higher priority)
 * @param factory  Factory function to create the goal for a specific companion
 */
public record TargetGoalEntry(int priority, Function<CompanionEntity, Goal> factory) {}
