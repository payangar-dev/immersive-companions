package com.payangar.immersivecompanions.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to access protected fields in Mob class.
 */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("targetSelector")
    GoalSelector getTargetSelector();
}
