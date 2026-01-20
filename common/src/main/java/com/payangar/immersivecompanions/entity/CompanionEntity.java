package com.payangar.immersivecompanions.entity;

import com.payangar.immersivecompanions.config.ModConfig;
import com.payangar.immersivecompanions.data.CompanionEquipment;
import com.payangar.immersivecompanions.data.CompanionNames;
import com.payangar.immersivecompanions.data.CompanionSkins;
import com.payangar.immersivecompanions.data.SkinInfo;
import com.payangar.immersivecompanions.entity.ai.CompanionDefendVillageGoal;
import com.payangar.immersivecompanions.entity.ai.CompanionRangedAttackGoal;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class CompanionEntity extends PathfinderMob implements RangedAttackMob {

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

    /** Default team for village-spawned companions */
    public static final String DEFAULT_TEAM = "village_guard";

    /** Movement speed modifier ID for critical injury state */
    private static final ResourceLocation CRITICAL_INJURY_SPEED_ID = ResourceLocation.fromNamespaceAndPath(
            "immersivecompanions", "critical_injury_slowdown");

    /** Callback for post-ranged-attack events (used by Epic Fight compat for shooting animation) */
    private Runnable onRangedAttackCallback = null;

    public CompanionEntity(EntityType<? extends CompanionEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();

        // Enable door opening in pathfinding
        if (this.getNavigation() instanceof GroundPathNavigation groundNav) {
            groundNav.setCanOpenDoors(true);
            groundNav.setCanPassDoors(true);
        }
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
    }

    @Override
    protected void registerGoals() {
        // Priority 0: Swimming
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Priority 1: Combat - added dynamically based on type
        // Will be set in finalizeSpawn after combat type is determined

        // Priority 3: Door interaction (like villagers)
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));

        // Priority 5: Village binding
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));

        // Priority 6: Wandering
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));

        // Priority 7-8: Looking around
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Target goals
        // Priority 2: Retaliation (but not against same-team companions)
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                // Don't retaliate against same-team companions
                LivingEntity attacker = this.mob.getLastHurtByMob();
                return !isOnSameTeam(attacker);
            }
        });

        // Priority 3: Attack monsters (filtered)
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                this::shouldAttackEntity));

        // Priority 4: Village defense
        this.targetSelector.addGoal(4, new CompanionDefendVillageGoal(this));
    }

    private void registerCombatGoals() {
        // Remove any existing combat goals first
        if (getCombatType().isRanged()) {
            // Use custom ranged attack goal with charging animation and kiting behavior
            // Parameters: companion, speedModifier, attackInterval, maxRange, minRange
            // - Will approach targets beyond maxRange (15 blocks)
            // - Will retreat from targets closer than minRange (6 blocks)
            // - Will strafe and shoot while in optimal range
            this.goalSelector.addGoal(1, new CompanionRangedAttackGoal(this, 1.0, 20, 15.0F, 6.0F));
        } else {
            // Wrap melee goal to prevent attacking when critically injured and track combat state
            this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true) {
                @Override
                public boolean canUse() {
                    if (ModConfig.get().isEnableCriticalInjury() && CompanionEntity.this.isCriticallyInjured()) {
                        return false;
                    }
                    return super.canUse();
                }
                @Override
                public boolean canContinueToUse() {
                    if (ModConfig.get().isEnableCriticalInjury() && CompanionEntity.this.isCriticallyInjured()) {
                        return false;
                    }
                    return super.canContinueToUse();
                }
                @Override
                public void start() {
                    super.start();
                    CompanionEntity.this.setAggressive(true);
                }
                @Override
                public void stop() {
                    super.stop();
                    CompanionEntity.this.setAggressive(false);
                }
            });
        }
    }

    private boolean shouldAttackEntity(LivingEntity entity) {
        // Don't attack Creepers or Endermen
        if (entity instanceof Creeper || entity instanceof EnderMan) {
            return false;
        }
        return entity instanceof Monster;
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

        // Register combat goals after type is determined
        registerCombatGoals();

        // Equip based on combat type
        CompanionEquipment.equipCompanion(this, combatType, level.getRandom());

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

    public boolean isCriticallyInjured() {
        return this.entityData.get(DATA_CRITICALLY_INJURED);
    }

    public void setCriticallyInjured(boolean injured) {
        this.entityData.set(DATA_CRITICALLY_INJURED, injured);

        // Apply or remove movement speed penalty
        AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(CRITICAL_INJURY_SPEED_ID);
            if (injured) {
                // Create modifier based on config (e.g., 0.5 multiplier = -0.5 penalty)
                double speedPenalty = -(1.0 - ModConfig.get().getCriticalInjurySpeedMultiplier());
                AttributeModifier modifier = new AttributeModifier(
                        CRITICAL_INJURY_SPEED_ID, speedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                speedAttr.addTransientModifier(modifier);
            }
        }
    }

    public ResourceLocation getSkinTexture() {
        return CompanionSkins.getSkin(getGender(), getSkinIndex());
    }

    /**
     * Gets the skin info for this companion, including texture and slim/wide model status.
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Gender")) {
            setGender(CompanionGender.values()[tag.getInt("Gender")]);
        }
        if (tag.contains("CombatType")) {
            setCombatType(CompanionType.values()[tag.getInt("CombatType")]);
            registerCombatGoals();
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

        arrow.shoot(dx, dy + distance * 0.2, dz, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
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
        arrow.shoot(dx, dy + distance * 0.15, dz, 2.0F, (float)(10 - this.level().getDifficulty().getId() * 4));
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
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

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
        // Periodic health check for edge cases (regen effects, commands, etc.)
        if (!this.level().isClientSide && this.tickCount % 10 == 0 && ModConfig.get().isEnableCriticalInjury()) {
            boolean shouldBeInjured = this.getHealth() <= ModConfig.get().getCriticalInjuryThreshold();
            if (this.isCriticallyInjured() != shouldBeInjured) {
                setCriticallyInjured(shouldBeInjured);
            }
        }
    }

    @Override
    public void setPose(Pose pose) {
        // Force crouching when critically injured (except for dying/sleeping)
        if (ModConfig.get().isEnableCriticalInjury() && this.isCriticallyInjured()
                && pose != Pose.DYING && pose != Pose.SLEEPING) {
            super.setPose(Pose.CROUCHING);
        } else {
            super.setPose(pose);
        }
    }

    @Override
    public void jumpFromGround() {
        // Disable jumping when critically injured
        if (ModConfig.get().isEnableCriticalInjury() && this.isCriticallyInjured()) {
            return;
        }
        super.jumpFromGround();
    }

    // Check if this entity can use ranged weapons
    public boolean canUseRangedWeapon() {
        ItemStack mainHand = this.getMainHandItem();
        return mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem;
    }
}
