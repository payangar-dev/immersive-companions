package com.payangar.immersivecompanions.entity;

/**
 * Listener interface for companion animation events.
 * CompanionEntity declares intent (state changes), and the listener handles
 * platform-specific animation logic (e.g., Epic Fight emotes).
 *
 * Implemented by CompanionEntityPatch (NeoForge + Epic Fight).
 * When no listener is registered (Fabric, or NeoForge without Epic Fight),
 * the state changes still happen but no animations play.
 */
public interface CompanionAnimationListener {

    /**
     * Called when a companion starts dancing (isDancing transitions to true).
     * The listener should trigger the appropriate dance animation.
     *
     * @param companion The companion that started dancing
     */
    void onStartDancing(CompanionEntity companion);

    /**
     * Called when a companion stops dancing (isDancing transitions to false).
     * The listener can clean up animation state if needed.
     *
     * @param companion The companion that stopped dancing
     */
    void onStopDancing(CompanionEntity companion);
}
