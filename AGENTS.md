# Immersive Companions - AI Agent Guide

## Project Overview

Minecraft mod that adds recruitable companion NPCs to villages. Companions have different genders, combat types (melee/ranged), skins, and can be hired by players using emeralds. They follow owners, fight alongside them, and defend villages.

- **Minecraft**: 1.21.1 | **Java**: 21 | **Loaders**: Fabric, NeoForge (multiloader)

## Build & Run Commands

```bash
./gradlew build              # Build all loaders
./gradlew runFabric          # Run Fabric client
./gradlew runNeoForge        # Run NeoForge client
```

## Project Structure

```
immersive-companions/
├── common/src/main/java/.../    # Shared code (most logic lives here)
│   ├── entity/                  # CompanionEntity + AI + combat/conditions/modes
│   │   ├── ai/                  # Goal classes (movement, combat, targeting)
│   │   ├── combat/              # CombatStance enum
│   │   ├── condition/           # Condition system (CriticalInjury, etc.)
│   │   └── mode/                # CompanionMode enum (WANDER/FOLLOW)
│   ├── config/                  # ModConfig (JSON-based)
│   ├── data/                    # Skins, names, equipment definitions
│   ├── network/                 # Platform-agnostic packet interfaces
│   ├── platform/                # Services interface for platform abstraction
│   ├── recruitment/             # Pricing logic
│   ├── registry/                # ModEntityTypes holder
│   ├── spawning/                # Village spawn logic
│   └── client/                  # GUI screens, renderers
├── fabric/src/main/java/.../    # Fabric-specific implementation
│   ├── platform/                # FabricServices
│   ├── network/                 # FabricNetworking
│   ├── registry/                # Entity registration
│   └── mixin/                   # VillageStructureMixin for spawning
├── neoforge/src/main/java/.../  # NeoForge-specific implementation
│   ├── platform/                # NeoForgeServices
│   ├── network/                 # NeoForgeNetworking
│   ├── events/                  # Spawn events
│   └── compat/epicfight/        # Epic Fight mod compatibility
└── gradle.properties            # Version config
```

## Core Systems

### CompanionEntity (`entity/CompanionEntity.java`)
Central mob class extending `PathfinderMob`. Key aspects:
- **Synced Data**: Gender, combat type, skin, mode, stance, team via `EntityDataAccessor`
- **NBT Persistence**: `addAdditionalSaveData()` / `readAdditionalSaveData()`
- **Goal Registration**: `registerGoals()` - all goals added once, filter via `canUse()`
- **Conditions**: `addCondition()`, `removeCondition()`, `hasCondition()`, `canPerformAction()`
- **Combat**: `performRangedAttack()`, implements `RangedAttackMob`

### Goal System (`entity/ai/`)
Priority-based Minecraft AI. **All goals registered statically at entity creation**; they decide when to run via `canUse()` / `canContinueToUse()` checks.

| Priority | Goal | Activation |
|----------|------|------------|
| 0 | InteractionGoal | During recruitment screen |
| 1 | FloatGoal, FleeGoal, MeleeAttackGoal, RangedAttackGoal | Conditions/combat type |
| 4 | MimicOwnerGoal | FOLLOW mode |
| 6 | FollowOwnerGoal, WanderGoal | Mode-based |

Target goals check stance: `canRetaliate()`, `canAssistOwner()`, `canProactivelyAttack()`, etc.

### Condition System (`entity/condition/`)
States affecting capabilities. Goals check via entity methods before starting.

- `CompanionCondition` - interface with `getBlockedActions()`, `disablesCombat()`, lifecycle hooks
- `CompanionConditions` - registry of all conditions
- `CriticalInjuryCondition` - example: blocks swimming/jumping, disables combat, forces crouch
- `ActionType` - enum of blockable actions (SWIM, SLEEP, JUMP, SPRINT)

### Mode System (`entity/mode/CompanionMode.java`)
Movement behavior enum:
- `WANDER` - random stroll, stays near village
- `FOLLOW` - follows owner, teleports if too far

### Combat Stance (`entity/combat/CombatStance.java`)
Targeting behavior enum:
- `PASSIVE` - no combat
- `DEFENSIVE` - retaliate only
- `ASSIST` - help owner's targets
- `AGGRESSIVE` - attack hostile mobs proactively

### Config (`config/ModConfig.java`)
JSON file at `config/immersivecompanions.json`. Static fields for YACL GUI binding.
Key options: `enableCriticalInjury`, `criticalInjuryThreshold`, `enableTeamCoordination`, etc.

### Spawning (`spawning/CompanionSpawnLogic.java`)
Village generation hook. Scales spawn chance with villager count (3+ villagers required).
- Fabric: `VillageStructureMixin`
- NeoForge: `NeoForgeSpawnEvents`

### Networking (`network/`)
Platform abstraction via `ModNetworking` interface. Payloads: `OpenRecruitmentScreen`, `CloseRecruitmentScreen`, `PurchaseCompanion`.

## Key Patterns

### Static Goal Registration
Goals are added once in `registerGoals()`. Each goal checks conditions in `canUse()`:
```java
@Override
public boolean canUse() {
    if (companion.getMode() != CompanionMode.FOLLOW) return false;
    // ... other checks
}
```

### Platform Abstraction
`Services` interface implemented per-loader. Access via `Services.get()`.

### NBT + EntityDataAccessor
- NBT for persistence (save/load)
- EntityDataAccessor for client-server sync

### Condition Checks Before Actions
```java
if (!companion.canPerformAction(ActionType.JUMP)) return;
if (companion.isCombatDisabled()) return false;
```

## Quick Reference

| System | Location |
|--------|----------|
| Main Entity | `common/.../entity/CompanionEntity.java` |
| All Goals | `common/.../entity/ai/` |
| Conditions | `common/.../entity/condition/` |
| Combat Stance | `common/.../entity/combat/CombatStance.java` |
| Mode | `common/.../entity/mode/CompanionMode.java` |
| Config | `common/.../config/ModConfig.java` |
| Spawn Logic | `common/.../spawning/CompanionSpawnLogic.java` |
| Networking | `common/.../network/ModNetworking.java` |
| Services | `common/.../platform/Services.java` |

## Adding New Features

### Adding a Condition
1. Create class implementing `CompanionCondition` in `entity/condition/`
2. Register in `CompanionConditions.java`: `public static final CompanionCondition X = register(XCondition.INSTANCE);`
3. Trigger via `entity.addCondition()` / `removeCondition()`

See `CriticalInjuryCondition.java` for reference.

### Adding a Goal
1. Create goal class in `entity/ai/`, check mode/stance/conditions in `canUse()`
2. Register in `CompanionEntity.registerGoals()` with appropriate priority
3. No dynamic add/remove needed - goal filters itself via `canUse()`

### Adding Config Options
1. Add static field + getter in `ModConfig.java`
2. Add to `ConfigData` inner class
3. Update `load()` and `save()` methods
4. Update client config screen (per-loader)
