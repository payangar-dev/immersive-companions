package com.payangar.immersivecompanions.compat.epicfight;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Helper class for Epic Fight queries that need to be called from common code via Services.
 * Isolated from NeoForgeServices to avoid class loading issues when Epic Fight is absent.
 *
 * Animation playback for stateful animations (dance) is handled by CompanionEntityPatch
 * via the CompanionAnimationListener pattern.
 * One-shot animations (greeting wave) use the Services path for reliability.
 */
public class EpicFightEmoteHelper {

    /**
     * Plays the greeting wave emote on a companion.
     * Uses Epic Fight's synchronized animation system to play BIPED_WAVE_HAND.
     *
     * @param companion The companion to play the emote on
     * @return true if the emote was played, false if no entity patch found
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
