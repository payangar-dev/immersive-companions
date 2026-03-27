package com.payangar.immersivecompanions.entity.ai;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * AI goal that makes companions dance around playing jukeboxes.
 * Only active in WANDER mode (owned or unowned).
 * Requires Epic Fight mod for dance animation - no vanilla fallback.
 */
public class CompanionDanceGoal extends Goal {

    /** Range to detect playing jukeboxes */
    private static final double JUKEBOX_DETECTION_RANGE = 16.0;

    /** Minimum distance from jukebox */
    private static final double MIN_DANCE_RADIUS = 2.0;

    /** Maximum distance from jukebox */
    private static final double MAX_DANCE_RADIUS = 4.0;

    /** Distance threshold to consider companion "at" the dance position */
    private static final double POSITION_THRESHOLD = 1.5;

    /** Golden angle in radians for optimal circular distribution */
    private static final double GOLDEN_ANGLE = Math.toRadians(137.5);

    /** Maximum collision avoidance attempts when finding dance position */
    private static final int MAX_COLLISION_ATTEMPTS = 8;

    /** Minimum spacing between dancing companions */
    private static final double MIN_COMPANION_SPACING = 1.5;

    private final CompanionEntity companion;
    private final Level level;

    /** Position of the jukebox being danced around */
    private BlockPos jukeboxPos;

    /** Target position on the dance floor */
    private Vec3 dancePosition;

    /** Whether the companion has reached their dance position */
    private boolean isPositioned;

    public CompanionDanceGoal(CompanionEntity companion) {
        this.companion = companion;
        this.level = companion.level();
        // Only MOVE flag - allow looking at players while dancing
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Only in WANDER mode
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }

        // Don't dance while being interacted with (recruitment screen open)
        if (companion.isBeingInteractedWith()) {
            return false;
        }

        // Don't dance if agonizing, critically injured, or combat disabled
        if (companion.isAgonizing() || companion.isCriticallyInjured() || companion.isCombatDisabled()) {
            return false;
        }

        // Find a playing jukebox
        this.jukeboxPos = findPlayingJukebox();
        return this.jukeboxPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        // Stop if mode changed
        if (companion.getMode() != CompanionMode.WANDER) {
            return false;
        }

        // Stop if being interacted with
        if (companion.isBeingInteractedWith()) {
            return false;
        }

        // Stop if jukebox stopped playing or was removed
        if (jukeboxPos == null || !isJukeboxPlaying(jukeboxPos)) {
            return false;
        }

        // Stop if too far from jukebox
        double distance = companion.blockPosition().distSqr(jukeboxPos);
        if (distance > JUKEBOX_DETECTION_RANGE * JUKEBOX_DETECTION_RANGE) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.dancePosition = calculateDancePosition();
        this.isPositioned = false;

        // Start moving to dance position
        if (dancePosition != null) {
            companion.getNavigation().moveTo(dancePosition.x, dancePosition.y, dancePosition.z, 1.0);
        }
    }

    @Override
    public void tick() {
        if (jukeboxPos == null) {
            return;
        }

        Vec3 jukeboxCenter = Vec3.atCenterOf(jukeboxPos);

        // Check if we've reached the dance position
        if (!isPositioned && dancePosition != null) {
            double distanceToPosition = companion.position().distanceTo(dancePosition);
            if (distanceToPosition < POSITION_THRESHOLD) {
                isPositioned = true;
                companion.getNavigation().stop();
            } else if (companion.getNavigation().isDone()) {
                // Re-navigate if path finished but not at position
                companion.getNavigation().moveTo(dancePosition.x, dancePosition.y, dancePosition.z, 1.0);
            }
        }

        // Look at jukebox while dancing (optional - other goals can override since LOOK not blocked)
        companion.getLookControl().setLookAt(
                jukeboxCenter.x,
                jukeboxCenter.y,
                jukeboxCenter.z,
                10.0F,
                (float) companion.getMaxHeadXRot()
        );

        // Start dancing when positioned — listener handles animation
        if (isPositioned && !companion.isDancing()) {
            companion.setDancing(true);
        }
    }

    @Override
    public void stop() {
        companion.getNavigation().stop();
        this.jukeboxPos = null;
        this.dancePosition = null;
        this.isPositioned = false;
        companion.setDancing(false);
    }

    /**
     * Finds a playing jukebox within detection range.
     *
     * @return The position of a playing jukebox, or null if none found
     */
    private BlockPos findPlayingJukebox() {
        BlockPos companionPos = companion.blockPosition();
        int range = (int) JUKEBOX_DETECTION_RANGE;

        for (BlockPos pos : BlockPos.betweenClosed(
                companionPos.offset(-range, -range, -range),
                companionPos.offset(range, range, range))) {

            if (isJukeboxPlaying(pos)) {
                return pos.immutable();
            }
        }

        return null;
    }

    /**
     * Checks if a jukebox at the given position is currently playing.
     * Uses NBT tag check for ticks_since_song_started which indicates active playback.
     *
     * @param pos The position to check
     * @return true if there's a playing jukebox at this position
     */
    private boolean isJukeboxPlaying(BlockPos pos) {
        Optional<net.minecraft.world.level.block.entity.JukeboxBlockEntity> optional =
                level.getBlockEntity(pos, BlockEntityType.JUKEBOX);

        if (optional.isEmpty()) {
            return false;
        }

        CompoundTag tag = optional.get().saveWithoutMetadata(level.registryAccess());
        return tag.contains("ticks_since_song_started");
    }

    /**
     * Calculates the dance position for this companion.
     * Uses random distance (1-4 blocks) from jukebox and checks line of sight.
     * Handles collision avoidance with other dancing companions.
     *
     * @return The target dance position, or null if none could be found
     */
    private Vec3 calculateDancePosition() {
        if (jukeboxPos == null) {
            return null;
        }

        Vec3 jukeboxCenter = Vec3.atCenterOf(jukeboxPos);
        List<CompanionEntity> nearbyDancers = getNearbyDancingCompanions();

        // Random radius between MIN and MAX
        double radius = MIN_DANCE_RADIUS + companion.getRandom().nextDouble() * (MAX_DANCE_RADIUS - MIN_DANCE_RADIUS);

        double baseAngle = getCompanionAngle();
        double minSpacingSq = MIN_COMPANION_SPACING * MIN_COMPANION_SPACING;

        // Try base angle first, then rotate if collision or no line of sight
        for (int attempt = 0; attempt < MAX_COLLISION_ATTEMPTS; attempt++) {
            double angle = baseAngle + (attempt * Math.PI / 4); // Rotate 45 degrees each attempt
            Vec3 candidatePos = calculatePositionAtAngle(jukeboxCenter, angle, radius);

            // Check line of sight to jukebox
            if (!hasLineOfSight(candidatePos, jukeboxCenter)) {
                continue;
            }

            // Check for collision with other companions' positions
            boolean collision = false;
            for (CompanionEntity other : nearbyDancers) {
                double otherAngle = getAngleForEntity(other);
                Vec3 otherTargetPos = calculatePositionAtAngle(jukeboxCenter, otherAngle, radius);

                if (candidatePos.distanceToSqr(otherTargetPos) < minSpacingSq) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                return candidatePos;
            }
        }

        // Fallback: use base angle position even if there's a collision
        return calculatePositionAtAngle(jukeboxCenter, baseAngle, radius);
    }

    /**
     * Checks if there's a clear line of sight from position to target.
     *
     * @param from Starting position
     * @param to   Target position
     * @return true if line of sight is clear
     */
    private boolean hasLineOfSight(Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(
                from.add(0, companion.getEyeHeight(), 0),
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                companion
        )).getType() == HitResult.Type.MISS;
    }

    /**
     * Gets nearby companions that are also dancing around jukeboxes.
     */
    private List<CompanionEntity> getNearbyDancingCompanions() {
        AABB searchBox = companion.getBoundingBox().inflate(JUKEBOX_DETECTION_RANGE);
        return level.getEntitiesOfClass(CompanionEntity.class, searchBox, other ->
                other != companion &&
                other.getMode() == CompanionMode.WANDER &&
                other.isDancing()
        );
    }

    /**
     * Calculates the unique angle for this companion using golden angle distribution.
     */
    private double getCompanionAngle() {
        return getAngleForEntity(companion);
    }

    /**
     * Calculates the unique angle for any entity using golden angle distribution.
     */
    private double getAngleForEntity(CompanionEntity entity) {
        return (entity.getId() * GOLDEN_ANGLE) % (2 * Math.PI);
    }

    /**
     * Calculates a position at the given angle and radius from a center point.
     */
    private Vec3 calculatePositionAtAngle(Vec3 center, double angle, double radius) {
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        return new Vec3(center.x + offsetX, center.y, center.z + offsetZ);
    }
}
