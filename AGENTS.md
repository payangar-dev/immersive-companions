# Companion Condition System

## Overview

Conditions are states that affect what a companion can do (injured, poisoned, etc.). Actions check conditions **before** starting, so blocked actions never begin - this prevents pose conflicts.

## Adding a New Condition

1. Create a class implementing `CompanionCondition` in `entity/condition/`:

```java
public class StunnedCondition implements CompanionCondition {
    public static final StunnedCondition INSTANCE = new StunnedCondition();

    @Override
    public String getId() { return "stunned"; }

    @Override
    public boolean isEnabled() { return ModConfig.get().isEnableStunned(); }

    // Block actions
    @Override
    public Set<ActionType> getBlockedActions() {
        return Set.of(ActionType.JUMP, ActionType.SPRINT);
    }

    // Disable combat targeting
    @Override
    public boolean disablesCombat() { return true; }

    // Add goals while active
    @Override
    public List<GoalEntry> getBehaviorGoals() {
        return List.of(new GoalEntry(1, SomeGoal::new));
    }

    // Force crouching pose
    @Override
    public boolean forcesCrouching() { return false; }

    // Lifecycle
    @Override
    public void onApply(CompanionEntity entity) {
        // Apply attribute modifiers, register goals
        entity.registerConditionGoals(this);
    }

    @Override
    public void onRemove(CompanionEntity entity) {
        // Remove modifiers, unregister goals
        entity.removeConditionGoals(this);
    }
}
```

2. Register in `CompanionConditions.java`:

```java
public static final CompanionCondition STUNNED = register(StunnedCondition.INSTANCE);
```

3. Trigger the condition somewhere in `CompanionEntity`:

```java
entity.addCondition(StunnedCondition.INSTANCE);
entity.removeCondition(StunnedCondition.INSTANCE);
```

## Adding a New Action Type

1. Add to `ActionType.java`:

```java
public enum ActionType {
    SWIM, SLEEP, JUMP, SPRINT,
    CLIMB  // new
}
```

2. Check it where the action happens:

```java
if (!companion.canPerformAction(ActionType.CLIMB)) {
    return; // or return false in canUse()
}
```

## Key Methods in CompanionEntity

| Method | Purpose |
|--------|---------|
| `addCondition(condition)` | Apply a condition |
| `removeCondition(condition)` | Remove a condition |
| `hasCondition(condition)` | Check if active |
| `canPerformAction(ActionType)` | Check if action is allowed |
| `isCombatDisabled()` | Check if any condition blocks combat |
| `shouldForceCrouch()` | Check if any condition forces crouching |
