package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Helper class for playing Epic Fight emotes on companions.
 * Isolated from NeoForgeServices to avoid class loading issues when Epic Fight is absent.
 */
public class EpicFightEmoteHelper {

    /**
     * Plays the wave hand greeting emote for the companion.
     * Must be called server-side only.
     *
     * @param companion The companion to play the emote for
     * @return true if the emote was played successfully
     */
    public static boolean playGreetingEmote(CompanionEntity companion) {
        if (companion.level().isClientSide()) {
            return false;
        }

        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(
                companion, LivingEntityPatch.class);

        if (patch != null) {
            patch.playAnimationSynchronized(Animations.BIPED_WAVE_HAND, 0.0F);
            return true;
        }

        return false;
    }
}
