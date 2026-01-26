package com.payangar.immersivecompanions.entity;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.data.CompanionEquipment;
import com.payangar.immersivecompanions.data.CompanionNames;
import com.payangar.immersivecompanions.data.CompanionSkins;
import com.payangar.immersivecompanions.data.SkinInfo;
import com.payangar.immersivecompanions.entity.ai.CompanionAssistOwnerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionDefendTeammatesGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionDefendVillageGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionFleeFromAttackerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionFloatGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionFollowOwnerGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionHurtByTargetGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionInteractionGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionMeleeAttackGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionNearestAttackableTargetGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionRangedAttackGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionTeamCoordinationGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionWaterAvoidingRandomStrollGoal;
import com.payangar.immersivecompanions.entity.ai.pathfinding.CompanionGroundPathNavigation;
import com.payangar.immersivecompanions.entity.combat.CombatStance;
import com.payangar.immersivecompanions.entity.condition.ActionType;
import com.payangar.immersivecompanions.entity.condition.CompanionCondition;
import com.payangar.immersivecompanions.entity.condition.CriticalInjuryCondition;
import com.payangar.immersivecompanions.entity.mode.CompanionMode;
import com.payangar.immersivecompanions.inventory.CompanionEquipmentMenu;
import com.payangar.immersivecompanions.network.ModNetworking;
import com.payangar.immersivecompanions.platform.Services;
import com.payangar.immersivecompanions.recruitment.CompanionPricing;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CompanionEntity extends PathfinderMob implements RangedAttackMob {

    /** Scale factor to match player size */
    public static final float RENDER_SCALE = 0.9275F;

    private static final EntityDataAccessor<Integer> DATA_GENDER = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMBAT_TYPE = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN_INDEX = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_TEAM = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_CRITICALLY_INJURED = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BASE_PRICE = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_MODE_ID = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_COMBAT_STANCE = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_WEAPON_HOLSTERED = SynchedEntityData.defineId(
            CompanionEntity.class, EntityDataSerializers.BOOLEAN);

    /** Default team for village-spawned companions */
    public static final String DEFAULT_TEAM = "village_guard";

    /** Attribute modifier ID for sneak speed reduction */
    private static final ResourceLocation SNEAK_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "immersivecompanions", "sneak_speed");

    /** Attribute modifier ID for sprint speed boost */
    private static final ResourceLocation SPRINT_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(
            "immersivecompanions", "sprint_speed");

    /**
     * Callback for post-ranged-attack events (used by Epic Fight compat for
     * shooting animation)
     */
    private Runnable onRangedAttackCallback = null;

    /**
     * UUID of the player currently interacting with this companion (recruitment
     * screen open)
     */
    @Nullable
    private UUID interactingPlayerUUID = null;

    /**
     * Timeout counter to auto-clear interaction if player disconnects or screen
     * closes without packet
     */
    private int interactionTimeout = 0;

    /** Maximum ticks before interaction auto-clears (30 seconds = 600 ticks) */
    private static final int INTERACTION_TIMEOUT_TICKS = 600;

    /**
     * Delay in ticks before holstering weapon after losing target (100 ticks = 5
     * seconds)
     */
    private static final int WEAPON_HOLSTER_DELAY_TICKS = 100;

    /**
     * Tracks ticks since the companion last had a valid target.
     * Used for delayed weapon holstering. Server-side only, not persisted.
     */
    private int ticksSinceLastTarget = Integer.MAX_VALUE;

    /** UUID of the player who owns this companion (null if unbought) */
    @Nullable
    private UUID ownerUUID = null;

    /** Current behavioral mode */
    private CompanionMode currentMode = CompanionMode.WANDER;

    /** Current combat stance controlling targeting behavior */
    private CombatStance currentStance = CombatStance.AGGRESSIVE;

    // ========== Condition System ==========

    /** Set of currently active conditions affecting this companion */
    private final Set<CompanionCondition> activeConditions = new HashSet<>();

    public CompanionEntity(EntityType<? extends CompanionEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        CompanionGroundPathNavigation navigation = new CompanionGroundPathNavigation(this, level);
        navigation.setCanOpenDoors(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    // ========== Despawn Prevention ==========

    /**
     * Companions should never despawn due to distance from players.
     * This is the primary despawn prevention - returning false means
     * the entity will never be removed by the distance-based despawn system.
     */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * Override to completely skip despawn checks.
     * This is a belt-and-suspenders approach - even if something
     * bypasses removeWhenFarAway(), this ensures no despawn occurs.
     */
    @Override
    public void checkDespawn() {
        // Do nothing - companions never despawn
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GENDER, CompanionGender.MALE.ordinal());
        builder.define(DATA_COMBAT_TYPE, CompanionType.MELEE.ordinal());
        builder.define(DATA_SKIN_INDEX, 0);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_TEAM, DEFAULT_TEAM);
        builder.define(DATA_CRITICALLY_INJURED, false);
        builder.define(DATA_BASE_PRICE, 0);
        builder.define(DATA_MODE_ID, CompanionMode.WANDER.getId());
        builder.define(DATA_COMBAT_STANCE, CombatStance.AGGRESSIVE.getId());
        builder.define(DATA_WEAPON_HOLSTERED, true); // Default: weapon holstered
    }

    @Override
    protected void registerGoals() {
        // ========== BEHAVIOR GOALS ==========
        // All goals are registered once; they use canUse()/canContinueToUse() to
        // determine when to run based on mode, stance, and conditions.

        // Priority 0: Interaction - stops movement and looks at player during
        // recruitment screen
        this.goalSelector.addGoal(0, new CompanionInteractionGoal(this));

        // Priority 1: Swimming (respects conditions via CompanionFloatGoal)
        this.goalSelector.addGoal(1, new CompanionFloatGoal(this));

        // Priority 1: Flee from attackers (active when shouldFlee() returns true)
        this.goalSelector.addGoal(1, new CompanionFleeFromAttackerGoal(this));

        // Priority 1: Combat goals - both registered, but check combat type in canUse()
        this.goalSelector.addGoal(1, new CompanionMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(1, new CompanionRangedAttackGoal(this, 1.0, 20, 15.0F, 6.0F));

        // Priority 3: Door interaction (like villagers)
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));

        // Priority 5: Village binding
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));

        // Priority 6: Movement goals - both registered, check mode in canUse()
        this.goalSelector.addGoal(6, new CompanionFollowOwnerGoal(this, 1.0));
        this.goalSelector.addGoal(6, new CompanionWaterAvoidingRandomStrollGoal(this, 0.9));

        // Priority 7-8: Looking around
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // ========== TARGET GOALS ==========
        // All target goals use canUse() checks based on stance.

        // Priority 2: Retaliate when hurt (active when canRetaliate() returns true)
        this.targetSelector.addGoal(2, new CompanionHurtByTargetGoal(this));

        // Priority 3: Defend teammates (active when canDefendTeammates() returns true)
        this.targetSelector.addGoal(3, new CompanionDefendTeammatesGoal(this));

        // Priority 3: Team coordination - defend and assist (active when
        // canAssistTeammates() returns true)
        this.targetSelector.addGoal(3, new CompanionTeamCoordinationGoal(this));

        // Priority 4: Assist owner (active when canAssistOwner() returns true)
        this.targetSelector.addGoal(4, new CompanionAssistOwnerGoal(this));

        // Priority 5: Proactively attack monsters (active when canProactivelyAttack()
        // returns true)
        this.targetSelector.addGoal(5, new CompanionNearestAttackableTargetGoal(this));

        // Priority 6: Defend village (active when canDefendVillage() returns true)
        this.targetSelector.addGoal(6, new CompanionDefendVillageGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData);

        // Randomize appearance
        CompanionGender gender = CompanionGender.random(level.getRandom());
        CompanionType combatType = CompanionType.random(level.getRandom());
        int skinIndex = level.getRandom().nextInt(CompanionSkins.getSkinCount(gender));

        setGender(gender);
        setCombatType(combatType);
        setSkinIndex(skinIndex);

        // Equip based on combat type
        CompanionEquipment.equipCompanion(this, combatType, level.getRandom());

        // Calculate and store base price (must be after equipment is set)
        setBasePrice(CompanionPricing.calculateBasePrice(this));

        // Set random name (custom names are always visible for mobs)
        this.setCustomName(CompanionNames.generateName(gender, level.getRandom()));

        return spawnData;
    }

    // Synced data getters and setters
    public CompanionGender getGender() {
        int ordinal = this.entityData.get(DATA_GENDER);
        return CompanionGender.values()[ordinal];
    }

    public void setGender(CompanionGender gender) {
        this.entityData.set(DATA_GENDER, gender.ordinal());
    }

    public CompanionType getCombatType() {
        int ordinal = this.entityData.get(DATA_COMBAT_TYPE);
        return CompanionType.values()[ordinal];
    }

    public void setCombatType(CompanionType type) {
        this.entityData.set(DATA_COMBAT_TYPE, type.ordinal());
    }

    public int getSkinIndex() {
        return this.entityData.get(DATA_SKIN_INDEX);
    }

    public void setSkinIndex(int index) {
        this.entityData.set(DATA_SKIN_INDEX, index);
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    public void setCharging(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    public String getCompanionTeam() {
        return this.entityData.get(DATA_TEAM);
    }

    public void setCompanionTeam(String team) {
        this.entityData.set(DATA_TEAM, team != null ? team : DEFAULT_TEAM);
    }

    /**
     * Checks if another entity is on the same companion team.
     * Used to prevent friendly fire and same-team targeting.
     *
     * @param entity The entity to check
     * @return true if the entity is a companion on the same team
     */
    public boolean isOnSameTeam(@Nullable LivingEntity entity) {
        if (entity instanceof CompanionEntity otherCompanion) {
            return this.getCompanionTeam().equals(otherCompanion.getCompanionTeam());
        }
        return false;
    }

    /**
     * Checks if this companion is critically injured.
     * Uses the condition system for the actual state tracking.
     *
     * @return true if the companion has the critical injury condition
     */
    public boolean isCriticallyInjured() {
        return hasCondition(CriticalInjuryCondition.INSTANCE);
    }

    /**
     * Sets the critically injured state by adding/removing the condition.
     * This method delegates to the condition system.
     *
     * @param injured true to apply the condition, false to remove it
     */
    public void setCriticallyInjured(boolean injured) {
        // Update synched data for client-side awareness
        this.entityData.set(DATA_CRITICALLY_INJURED, injured);

        if (injured) {
            addCondition(CriticalInjuryCondition.INSTANCE);
        } else {
            removeCondition(CriticalInjuryCondition.INSTANCE);
        }
    }

    /**
     * Gets the base recruitment price for this companion.
     * This is calculated once at spawn time and never changes.
     */
    public int getBasePrice() {
        return this.entityData.get(DATA_BASE_PRICE);
    }

    /**
     * Sets the base recruitment price for this companion.
     * Should only be called once during spawn.
     */
    public void setBasePrice(int price) {
        this.entityData.set(DATA_BASE_PRICE, price);
    }

    // ========== Mode System ==========

    /**
     * Gets the current behavioral mode.
     *
     * @return The current mode
     */
    public CompanionMode getMode() {
        return this.currentMode;
    }

    /**
     * Checks if the companion is in WANDER mode.
     */
    public boolean isInWanderMode() {
        return this.currentMode == CompanionMode.WANDER;
    }

    /**
     * Checks if the companion is in FOLLOW mode.
     */
    public boolean isInFollowMode() {
        return this.currentMode == CompanionMode.FOLLOW;
    }

    /**
     * Sets the behavioral mode.
     * Goals check the mode in their canUse() methods, so no goal manipulation
     * needed.
     *
     * @param mode The new mode to set
     */
    public void setMode(CompanionMode mode) {
        if (mode == null || mode == this.currentMode) {
            return;
        }

        this.currentMode = mode;
        this.entityData.set(DATA_MODE_ID, mode.getId());
    }

    // ========== Combat Stance System ==========

    /**
     * Gets the current combat stance.
     *
     * @return The current stance
     */
    public CombatStance getCombatStance() {
        return this.currentStance;
    }

    /**
     * Checks if the companion is in PASSIVE stance.
     */
    public boolean isPassive() {
        return this.currentStance == CombatStance.PASSIVE;
    }

    /**
     * Checks if the companion can retaliate when attacked.
     * True for DEFENSIVE, ASSIST, and AGGRESSIVE stances when not combat disabled.
     */
    public boolean canRetaliate() {
        return !isPassive() && !isCombatDisabled();
    }

    /**
     * Checks if the companion can defend teammates under attack.
     * True for DEFENSIVE, ASSIST, and AGGRESSIVE stances when not combat disabled.
     */
    public boolean canDefendTeammates() {
        return !isPassive() && !isCombatDisabled();
    }

    /**
     * Checks if the companion can assist teammates in attacking.
     * True for ASSIST and AGGRESSIVE stances when not combat disabled.
     */
    public boolean canAssistTeammates() {
        return (this.currentStance == CombatStance.ASSIST || this.currentStance == CombatStance.AGGRESSIVE)
                && !isCombatDisabled();
    }

    /**
     * Checks if the companion can assist their owner in attacking.
     * True for ASSIST and AGGRESSIVE stances when not combat disabled.
     */
    public boolean canAssistOwner() {
        return (this.currentStance == CombatStance.ASSIST || this.currentStance == CombatStance.AGGRESSIVE)
                && !isCombatDisabled();
    }

    /**
     * Checks if the companion can proactively attack monsters.
     * True only for AGGRESSIVE stance when not combat disabled.
     */
    public boolean canProactivelyAttack() {
        return this.currentStance == CombatStance.AGGRESSIVE && !isCombatDisabled();
    }

    /**
     * Checks if the companion should flee from attackers.
     * True when in PASSIVE stance or critically injured.
     */
    public boolean shouldFlee() {
        return isPassive() || isCriticallyInjured();
    }

    /**
     * Checks if the companion can defend the village from hostile players.
     * True when in AGGRESSIVE stance, not combat disabled, and in a villager team.
     */
    public boolean canDefendVillage() {
        return canProactivelyAttack() && isInVillagerTeam();
    }

    /**
     * Checks if this companion is in the default villager team.
     * Used to determine if they should defend villages.
     */
    public boolean isInVillagerTeam() {
        return DEFAULT_TEAM.equals(getCompanionTeam());
    }

    /**
     * Sets the combat stance.
     * Goals check the stance in their canUse() methods, so no goal manipulation
     * needed.
     *
     * @param stance The new stance to set
     */
    public void setCombatStance(CombatStance stance) {
        if (stance == null || stance == this.currentStance) {
            return;
        }

        this.currentStance = stance;
        this.entityData.set(DATA_COMBAT_STANCE, stance.getId());

        // Clear target when entering PASSIVE stance
        if (stance == CombatStance.PASSIVE) {
            this.setTarget(null);
        }
    }

    // ========== Weapon Holster State ==========

    /**
     * Checks if the companion's weapon is holstered.
     * When holstered, arms are lowered and weapon is hidden/on back.
     *
     * @return true if weapon is holstered, false if drawn
     */
    public boolean isWeaponHolstered() {
        return this.entityData.get(DATA_WEAPON_HOLSTERED);
    }

    /**
     * Sets the weapon holster state.
     * Called by combat goals when entering/exiting combat.
     *
     * @param holstered true to holster weapon, false to draw weapon
     */
    public void setWeaponHolstered(boolean holstered) {
        this.entityData.set(DATA_WEAPON_HOLSTERED, holstered);
    }

    /**
     * Updates weapon holster state based on target presence.
     * Should be called every tick on the server side.
     *
     * Logic:
     * - If combat is disabled, weapon stays holstered
     * - If companion has a valid target, draw weapon immediately and reset counter
     * - If no target, increment counter and holster after configured delay
     */
    private void updateWeaponHolsterState() {
        LivingEntity target = getTarget();
        boolean hasValidTarget = target != null && target.isAlive();

        if (hasValidTarget) {
            ticksSinceLastTarget = 0;
        } else if (ticksSinceLastTarget < WEAPON_HOLSTER_DELAY_TICKS) {
            ticksSinceLastTarget++;
        }

        Boolean shouldHostered = isCombatDisabled()
                || ticksSinceLastTarget >= WEAPON_HOLSTER_DELAY_TICKS;

        if (isWeaponHolstered() != shouldHostered) {
            setWeaponHolstered(shouldHostered);
        }
    }

    // ========== Condition System ==========

    /**
     * Adds a condition to this companion.
     * Conditions affect what actions are possible.
     *
     * @param condition The condition to add
     */
    public void addCondition(CompanionCondition condition) {
        if (!condition.isEnabled()) {
            return; // Condition is disabled in config
        }
        if (activeConditions.add(condition)) {
            condition.onApply(this);
        }
    }

    /**
     * Removes a condition from this companion.
     *
     * @param condition The condition to remove
     */
    public void removeCondition(CompanionCondition condition) {
        if (activeConditions.remove(condition)) {
            condition.onRemove(this);
        }
    }

    /**
     * Checks if this companion has a specific condition.
     *
     * @param condition The condition to check
     * @return true if the condition is active
     */
    public boolean hasCondition(CompanionCondition condition) {
        return activeConditions.contains(condition);
    }

    /**
     * Checks if a basic action can be performed.
     * Actions are blocked by conditions that list them in getBlockedActions().
     *
     * @param action The action type to check
     * @return true if the action can be performed
     */
    public boolean canPerformAction(ActionType action) {
        return activeConditions.stream()
                .noneMatch(c -> c.getBlockedActions().contains(action));
    }

    /**
     * Checks if combat is disabled by any active condition.
     * Used by combat goals to check if they should run.
     *
     * @return true if combat is disabled
     */
    public boolean isCombatDisabled() {
        return activeConditions.stream()
                .anyMatch(CompanionCondition::disablesCombat);
    }

    // ========== Sneaking State Management ==========

    /**
     * Starts sneaking for this companion.
     * Sets both the shift key state and the crouching pose.
     * Use this method instead of manually setting shift key and pose to ensure
     * consistency.
     */
    public void startSneaking() {
        if (this.getPose() == Pose.DYING || this.getPose() == Pose.SLEEPING)
            return;
        this.setShiftKeyDown(true);
        this.setPose(Pose.CROUCHING);

        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(SNEAK_SPEED_MODIFIER_ID)) {
            AttributeModifier modifier = new AttributeModifier(
                    SNEAK_SPEED_MODIFIER_ID, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addTransientModifier(modifier);
        }
    }

    /**
     * Stops sneaking for this companion.
     * Clears the shift key state and restores the standing pose.
     * Will not change pose if the companion is dying or sleeping.
     * Use this method instead of manually clearing shift key to ensure the pose is
     * also reset.
     */
    public void stopSneaking() {
        this.setShiftKeyDown(false);
        if (this.getPose() == Pose.CROUCHING) {
            this.setPose(Pose.STANDING);
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SNEAK_SPEED_MODIFIER_ID);
        }
    }

    // ========== Sprint State Management ==========

    /**
     * Checks if this companion can sprint.
     * Sprinting is blocked when crouching or when a condition blocks the SPRINT
     * action.
     *
     * @return true if the companion can sprint
     */
    public boolean canSprint() {
        return !isCrouching() && canPerformAction(ActionType.SPRINT);
    }

    /**
     * Starts sprinting for this companion.
     * Sets the sprint state and applies a speed boost modifier.
     * Will not sprint if crouching or blocked by a condition.
     */
    public void startSprinting() {
        if (!canSprint()) {
            return;
        }
        this.setSprinting(true);

        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(SPRINT_SPEED_MODIFIER_ID)) {
            AttributeModifier modifier = new AttributeModifier(
                    SPRINT_SPEED_MODIFIER_ID, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            speed.addTransientModifier(modifier);
        }
    }

    /**
     * Stops sprinting for this companion.
     * Clears the sprint state and removes the speed boost modifier.
     */
    public void stopSprinting() {
        this.setSprinting(false);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPRINT_SPEED_MODIFIER_ID);
        }
    }

    // ========== Owner System ==========

    /**
     * Gets the UUID of the player who owns this companion.
     *
     * @return The owner's UUID, or null if unbought
     */
    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    /**
     * Sets the owner of this companion.
     * Handles all ownership transitions: null→owner, owner→null, and owner A→owner
     * B.
     *
     * @param uuid The owner's UUID, or null to clear ownership
     */
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
    }

    /**
     * Checks if this companion has an owner.
     *
     * @return true if the companion is owned by a player
     */
    public boolean hasOwner() {
        return this.ownerUUID != null;
    }

    /**
     * Gets the player who owns this companion.
     *
     * @return The owner player, or null if unbought or owner offline
     */
    @Nullable
    public Player getOwner() {
        if (this.ownerUUID == null) {
            return null;
        }
        return this.level().getPlayerByUUID(this.ownerUUID);
    }

    /**
     * Checks if this companion is owned by a specific player.
     *
     * @param player The player to check
     * @return true if the companion is owned by this player
     */
    public boolean isOwnedBy(Player player) {
        return this.ownerUUID != null && this.ownerUUID.equals(player.getUUID());
    }

    /** Number of different recruitment messages available */
    private static final int RECRUITMENT_MESSAGE_COUNT = 12;

    /**
     * Sends a random recruitment message to the specified player.
     * Called when a companion is successfully recruited.
     *
     * @param player The player to send the message to
     */
    public void sendRecruitmentMessage(Player player) {
        if (this.level().isClientSide) {
            return;
        }

        // Pick a random message (1-12)
        int messageIndex = this.getRandom().nextInt(RECRUITMENT_MESSAGE_COUNT) + 1;
        String companionName = this.getDisplayName().getString();

        // Create the chat message with companion's name
        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(
                "chat.immersivecompanions.recruited." + messageIndex,
                companionName);

        // Send only to this player (system message)
        player.sendSystemMessage(message);
    }

    /**
     * Sets the player currently interacting with this companion.
     * Called when a player opens the recruitment screen.
     *
     * @param player The player who opened the recruitment screen
     */
    public void setInteractingPlayer(Player player) {
        this.interactingPlayerUUID = player.getUUID();
        this.interactionTimeout = INTERACTION_TIMEOUT_TICKS;
    }

    /**
     * Clears the interacting player state.
     * Called when the recruitment screen is closed.
     */
    public void clearInteractingPlayer() {
        this.interactingPlayerUUID = null;
        this.interactionTimeout = 0;
    }

    /**
     * Checks if this companion is currently being interacted with by a player.
     *
     * @return true if a player has the recruitment screen open for this companion
     */
    public boolean isBeingInteractedWith() {
        return this.interactingPlayerUUID != null;
    }

    /**
     * Gets the player currently interacting with this companion.
     *
     * @return The interacting player, or null if none
     */
    @Nullable
    public Player getInteractingPlayer() {
        if (this.interactingPlayerUUID == null) {
            return null;
        }
        return this.level().getPlayerByUUID(this.interactingPlayerUUID);
    }

    public ResourceLocation getSkinTexture() {
        return CompanionSkins.getSkin(getGender(), getSkinIndex());
    }

    /**
     * Gets the skin info for this companion, including texture and slim/wide model
     * status.
     * Used by the renderer to determine which model variant to use.
     */
    public SkinInfo getSkinInfo() {
        return CompanionSkins.getSkinInfo(getGender(), getSkinIndex());
    }

    // NBT persistence
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Gender", getGender().ordinal());
        tag.putInt("CombatType", getCombatType().ordinal());
        tag.putInt("SkinIndex", getSkinIndex());
        tag.putString("Team", getCompanionTeam());
        tag.putBoolean("CriticallyInjured", isCriticallyInjured());
        tag.putInt("BasePrice", getBasePrice());
        tag.putString("ModeId", currentMode.getId());
        tag.putString("CombatStance", currentStance.getId());
        tag.putBoolean("WeaponHolstered", isWeaponHolstered());
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }

        // Save armor equipment explicitly
        ListTag armorItems = new ListTag();
        for (ItemStack stack : this.getArmorSlots()) {
            if (stack.isEmpty()) {
                armorItems.add(new CompoundTag());
            } else {
                armorItems.add(stack.save(this.registryAccess()));
            }
        }
        tag.put("CompanionArmor", armorItems);

        // Save hand items explicitly
        ListTag handItems = new ListTag();
        for (ItemStack stack : this.getHandSlots()) {
            if (stack.isEmpty()) {
                handItems.add(new CompoundTag());
            } else {
                handItems.add(stack.save(this.registryAccess()));
            }
        }
        tag.put("CompanionHands", handItems);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Gender")) {
            setGender(CompanionGender.values()[tag.getInt("Gender")]);
        }
        if (tag.contains("CombatType")) {
            setCombatType(CompanionType.values()[tag.getInt("CombatType")]);
        }
        if (tag.contains("SkinIndex")) {
            setSkinIndex(tag.getInt("SkinIndex"));
        }
        if (tag.contains("Team")) {
            setCompanionTeam(tag.getString("Team"));
        }
        if (tag.contains("CriticallyInjured")) {
            setCriticallyInjured(tag.getBoolean("CriticallyInjured"));
        }
        if (tag.contains("BasePrice")) {
            setBasePrice(tag.getInt("BasePrice"));
        }
        if (tag.contains("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        if (tag.contains("ModeId")) {
            String modeId = tag.getString("ModeId");
            CompanionMode mode = CompanionMode.byId(modeId);
            this.currentMode = mode;
            this.entityData.set(DATA_MODE_ID, mode.getId());
        }
        if (tag.contains("CombatStance")) {
            String stanceId = tag.getString("CombatStance");
            CombatStance stance = CombatStance.byId(stanceId);
            this.currentStance = stance;
            this.entityData.set(DATA_COMBAT_STANCE, stance.getId());
        }
        if (tag.contains("WeaponHolstered")) {
            setWeaponHolstered(tag.getBoolean("WeaponHolstered"));
        }

        // Load armor equipment
        if (tag.contains("CompanionArmor", Tag.TAG_LIST)) {
            ListTag armorItems = tag.getList("CompanionArmor", Tag.TAG_COMPOUND);
            EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
            for (int i = 0; i < Math.min(armorItems.size(), armorSlots.length); i++) {
                CompoundTag itemTag = armorItems.getCompound(i);
                if (!itemTag.isEmpty()) {
                    final EquipmentSlot slot = armorSlots[i];
                    ItemStack.parse(this.registryAccess(), itemTag).ifPresent(
                            stack -> this.setItemSlot(slot, stack));
                }
            }
        }

        // Load hand items
        if (tag.contains("CompanionHands", Tag.TAG_LIST)) {
            ListTag handItems = tag.getList("CompanionHands", Tag.TAG_COMPOUND);
            EquipmentSlot[] handSlots = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
            for (int i = 0; i < Math.min(handItems.size(), handSlots.length); i++) {
                CompoundTag itemTag = handItems.getCompound(i);
                if (!itemTag.isEmpty()) {
                    final EquipmentSlot slot = handSlots[i];
                    ItemStack.parse(this.registryAccess(), itemTag).ifPresent(
                            stack -> this.setItemSlot(slot, stack));
                }
            }
        }
    }

    /**
     * Sets a callback to be invoked after performing a ranged attack.
     * Used by Epic Fight compat to trigger shooting animations.
     */
    public void setOnRangedAttackCallback(Runnable callback) {
        this.onRangedAttackCallback = callback;
    }

    // Ranged attack implementation
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack heldItem = this.getMainHandItem();

        if (heldItem.getItem() instanceof CrossbowItem) {
            performCrossbowAttack(target);
        } else {
            performBowAttack(target, distanceFactor);
        }

        // Notify listeners (e.g., Epic Fight for shooting animation)
        if (onRangedAttackCallback != null) {
            onRangedAttackCallback.run();
        }
    }

    private void performBowAttack(LivingEntity target, float distanceFactor) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, this.getMainHandItem());

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + distance * 0.2, dz, 1.6F, (float) (14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    private void performCrossbowAttack(LivingEntity target) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, 1.0F, this.getMainHandItem());

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Crossbows are faster and more accurate
        arrow.shoot(dx, dy + distance * 0.15, dz, 2.0F, (float) (10 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    // Handle reputation when attacked by player
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && source.getEntity() instanceof Player player && !this.level().isClientSide) {
            // Spread negative gossip to nearby villagers about this player
            spreadNegativeGossip(player);
        }

        // Check for critical injury (health at or below threshold)
        if (hurt && !this.level().isClientSide && ModConfig.get().isEnableCriticalInjury()
                && this.getHealth() <= ModConfig.get().getCriticalInjuryThreshold()
                && !this.isCriticallyInjured()) {
            setCriticallyInjured(true);
        }

        return hurt;
    }

    private void spreadNegativeGossip(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        // Find nearby villagers
        AABB searchBox = this.getBoundingBox().inflate(16.0);
        List<Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, searchBox);

        for (Villager villager : villagers) {
            // Add negative gossip about the player
            villager.getGossips().add(player.getUUID(),
                    net.minecraft.world.entity.ai.gossip.GossipType.MINOR_NEGATIVE, 25);
        }
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        // Exit critical injury state when health recovers above threshold
        if (!this.level().isClientSide && this.isCriticallyInjured()
                && this.getHealth() > ModConfig.get().getCriticalInjuryThreshold()) {
            setCriticallyInjured(false);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Handle interaction timeout
            if (this.interactingPlayerUUID != null) {
                // Check if player is still valid
                Player interactingPlayer = getInteractingPlayer();
                if (interactingPlayer == null || !interactingPlayer.isAlive()) {
                    // Player disconnected or died - clear interaction
                    clearInteractingPlayer();
                } else if (--this.interactionTimeout <= 0) {
                    // Timeout expired - clear interaction
                    clearInteractingPlayer();
                }
            }

            // Periodic health check for edge cases (regen effects, commands, etc.)
            if (this.tickCount % 10 == 0 && ModConfig.get().isEnableCriticalInjury()) {
                boolean shouldBeInjured = this.getHealth() <= ModConfig.get().getCriticalInjuryThreshold();
                if (this.isCriticallyInjured() != shouldBeInjured) {
                    setCriticallyInjured(shouldBeInjured);
                }
            }

            // Tick active conditions
            for (CompanionCondition condition : activeConditions) {
                condition.tick(this);
            }

            if (isSprinting()) {
                spawnSprintParticle();
            }

            // Enforce sprint restrictions - stop sprinting if no longer allowed
            if (isSprinting() && !canSprint()) {
                stopSprinting();
            }

            if (((this.getMode() == CompanionMode.FOLLOW && this.getOwner().isCrouching()) || this.isCriticallyInjured())) {
                if (!this.isCrouching()) {
                    startSneaking();
                }
            } else {
                if (this.isCrouching()) {
                    stopSneaking();
                }
            }

            // Update weapon holster state based on target presence
            updateWeaponHolsterState();
        }
    }

    @Override
    public void jumpFromGround() {
        // Disable jumping when blocked by a condition
        if (!canPerformAction(ActionType.JUMP)) {
            return;
        }
        super.jumpFromGround();
    }

    @Override
    public int getMaxFallDistance() {
        // Critically injured companions are more cautious about falls
        if (isCriticallyInjured()) {
            return 1;
        }
        // Normal companions avoid damage-causing falls (>3 blocks)
        return 3;
    }

    @Override
    public void aiStep() {
        updateSwingTime(); // Required for melee attack animation to work
        super.aiStep();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
    }

    // Check if this entity can use ranged weapons
    public boolean canUseRangedWeapon() {
        ItemStack mainHand = this.getMainHandItem();
        return mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Only handle main hand interactions
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }

        // Open equipment screen for owners
        if (hasOwner()) {
            if (isOwnedBy(player) && !this.level().isClientSide) {
                openEquipmentScreen(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Only show recruitment screen for companions in the default team
        if (!DEFAULT_TEAM.equals(getCompanionTeam())) {
            return super.mobInteract(player, hand);
        }

        if (!this.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // Block concurrent interactions - only one player can interact at a time
            if (isBeingInteractedWith()) {
                return InteractionResult.FAIL;
            }

            // Set interaction state before opening screen
            setInteractingPlayer(player);

            // Use stored base price, apply reputation modifier for final price
            int basePrice = getBasePrice();
            int finalPrice = CompanionPricing.calculateFinalPrice(basePrice, this, player);

            // Send packet to open recruitment screen
            ModNetworking.get().sendOpenRecruitmentScreen(serverPlayer, this.getId(), basePrice, finalPrice);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /**
     * Opens the equipment screen for the owner player.
     * Uses the vanilla container system with extra data for entity ID.
     */
    private void openEquipmentScreen(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            final CompanionEntity companion = this;
            Services.get().openMenu(serverPlayer, new MenuProvider() {
                @Override
                public net.minecraft.network.chat.Component getDisplayName() {
                    return companion.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory playerInventory, Player player) {
                    return new CompanionEquipmentMenu(containerId, playerInventory, companion);
                }
            }, buf -> buf.writeVarInt(this.getId()));
        }
    }
}
