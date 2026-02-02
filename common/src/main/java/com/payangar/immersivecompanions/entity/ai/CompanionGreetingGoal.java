package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.platform.Services;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * AI goal that makes companions wave at players who approach and look at them.
 * Only active in WANDER mode (owned or unowned).
 * Has a per-player cooldown that resets when the companion is discharged.
 */
public class CompanionGreetingGoal extends Goal {

    private static final float DETECTION_RANGE = 6.0f;
    private static final double LOOK_DOT_THRESHOLD = 0.7;
    private static final int LOOK_DURATION_TICKS = 40;

    private final CompanionEntity companion;
    private Player targetPlayer;
    private int lookTimer;

    public CompanionGreetingGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only in WANDER mode
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }

        // Don't greet while being interacted with (recruitment screen open)
        if (companion.isBeingInteractedWith()) {
            return false;
        }

        // Find a valid player to greet
        this.targetPlayer = findPlayerToGreet();
        return this.targetPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue looking at player for the duration
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
            return false;
        }
        return this.lookTimer > 0;
    }

    @Override
    public void start() {
        if (this.targetPlayer == null) {
            return;
        }

        // Stop any current navigation
        companion.getNavigation().stop();

        // Play the greeting emote via platform services
        Services.get().playGreetingEmote(companion);

        // Send greeting message to player
        companion.sendGreetingMessage(this.targetPlayer);

        // Record the greeting to start cooldown
        companion.recordGreeting(this.targetPlayer);

        // Start the look timer
        this.lookTimer = LOOK_DURATION_TICKS;
    }

    @Override
    public void tick() {
        // Stop navigation every tick to prevent other goals from moving us
        companion.getNavigation().stop();

        if (this.targetPlayer != null && this.targetPlayer.isAlive()) {
            // Look at the player
            companion.getLookControl().setLookAt(
                    this.targetPlayer.getX(),
                    this.targetPlayer.getEyeY(),
                    this.targetPlayer.getZ(),
                    10.0F,
                    (float) companion.getMaxHeadXRot()
            );
        }
        this.lookTimer--;
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.lookTimer = 0;
    }

    /**
     * Finds a player that this companion can greet.
     * Player must be:
     * - Not a spectator
     * - Alive
     * - Not on cooldown for this companion
     * - Companion is facing the player (not back turned)
     * - Looking at the companion (dot product >= threshold)
     * - Has line of sight to the companion
     *
     * @return A valid player to greet, or null if none found
     */
    private Player findPlayerToGreet() {
        List<Player> nearbyPlayers = companion.level().getEntitiesOfClass(
                Player.class,
                companion.getBoundingBox().inflate(DETECTION_RANGE),
                player -> !player.isSpectator() && player.isAlive()
        );

        for (Player player : nearbyPlayers) {
            // Check cooldown
            if (!companion.canGreetPlayer(player)) {
                continue;
            }

            // Check if companion is facing the player (not back turned)
            if (!isCompanionFacingPlayer(player)) {
                continue;
            }

            // Check if player is looking at companion
            if (!isPlayerLookingAtCompanion(player)) {
                continue;
            }

            // Check line of sight
            if (!companion.hasLineOfSight(player)) {
                continue;
            }

            return player;
        }

        return null;
    }

    /**
     * Checks if a player is looking at this companion.
     * Uses dot product between player's look direction and direction to companion.
     *
     * @param player The player to check
     * @return true if the player is looking at the companion (within ~45 degrees)
     */
    private boolean isPlayerLookingAtCompanion(Player player) {
        Vec3 playerLook = player.getLookAngle();
        Vec3 toCompanion = companion.position().add(0, companion.getEyeHeight() / 2, 0)
                .subtract(player.getEyePosition())
                .normalize();

        double dot = playerLook.dot(toCompanion);
        return dot >= LOOK_DOT_THRESHOLD;
    }

    /**
     * Checks if this companion is facing a player (not back turned).
     * Uses the companion's body rotation to calculate forward direction.
     *
     * @param player The player to check
     * @return true if the companion is facing the player (within ~90 degrees)
     */
    private boolean isCompanionFacingPlayer(Player player) {
        // Get companion's forward direction based on body rotation
        float yawRadians = (float) Math.toRadians(companion.yBodyRot);
        Vec3 companionForward = new Vec3(-Math.sin(yawRadians), 0, Math.cos(yawRadians));

        // Direction from companion to player (horizontal only)
        Vec3 toPlayer = player.position().subtract(companion.position());
        toPlayer = new Vec3(toPlayer.x, 0, toPlayer.z).normalize();

        // Companion needs to be roughly facing the player (dot >= 0 means within 90 degrees)
        double dot = companionForward.dot(toPlayer);
        return dot >= 0;
    }
}
