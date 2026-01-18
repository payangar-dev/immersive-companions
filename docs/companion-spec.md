# Companion Feature Specification

## Overview

A **companion** is a player-type entity controlled by the game that acts as a conditional ally, protecting villagers and reacting to hostile actions by the player.

---

## Entity Definition

### Model and Appearance

- **Model**: Player model (Steve or Alex variant)
- **Gender**: Randomly assigned at spawn (male or female)
- **Model variant**: Steve model for male, Alex model for female
- **Skin**: Random vanilla texture based on gender
- **Visual indicators**: None (no visual distinction between neutral/hostile states)

### Name

- **Format**: `<FirstName> <LastName>`
- **First name**: Randomly selected from a gendered name pool (male names for male companions, female names for female companions)
- **Last name**: Randomly selected from a shared surname pool
- **Display**: Shown as nametag above entity

### Base Stats

Identical to player defaults:

| Stat | Value |
|------|-------|
| Health | 20 HP (10 hearts) |
| Attack damage | 1 (base, modified by weapon) |
| Movement speed | Player movement speed |
| Knockback resistance | 0 (none) |

---

## Equipment

### General Rules

- Equipment is **randomized at spawn**
- Maximum tier: **Iron** (never diamond, netherite, or gold)
- Companions spawn with **either melee OR ranged weapon**, not both

### Weapon Options

**Melee (if melee type):**
- Sword (wood, stone, or iron)
- Axe (wood, stone, or iron)

**Ranged (if ranged type):**
- Bow
- Crossbow
- **Arrows**: Unlimited (no arrow item required in inventory)

### Armor Options

Each armor slot is randomized independently:

| Slot | Options |
|------|---------|
| Helmet | None, Leather, Chainmail, Iron |
| Chestplate | None, Leather, Chainmail, Iron |
| Leggings | None, Leather, Chainmail, Iron |
| Boots | None, Leather, Chainmail, Iron |

---

## Behavior

### Relationship with Player

- **Default state**: Neutral toward player
- **Does not attack player** unless provoked

**Triggers for hostility:**
1. Player attacks the companion directly
2. Player attacks a villager

**Hostility system**: Uses the same village reputation system as iron golems
- Hostility is **permanent** until the player recovers reputation
- Reputation recovery methods: trading with villagers, curing zombie villagers, etc.

### Combat Behavior

**Targets (attacks automatically):**
- All hostile mobs

**Exceptions (ignores and is ignored by):**
- Creepers
- Endermen

**Detection range**: Same as iron golem (~10-16 blocks)

**Combat style:**
- Melee companions: Engage in close combat with sword/axe
- Ranged companions: Attack from distance with bow/crossbow

---

## Spawning

### Trigger

- **Only during world generation** (village structure generation)
- Does NOT spawn from:
  - Villager breeding
  - Curing zombie villagers
  - Commands (for now)

### Conditions

- Requires **3 or more villagers** in the spawn group

### Spawn Chance

| Villager Count | Spawn Probability |
|----------------|-------------------|
| 3 | 10% (0.1) |
| 4 | ~14.3% |
| 5 | ~18.6% |
| 6 | ~22.9% |
| 7 | ~27.1% |
| 8 | ~31.4% |
| 9 | ~35.7% |
| 10+ | 40% (0.4) |

**Formula**: `probability = 0.1 + (min(villagerCount, 10) - 3) * (0.3 / 7)`

### Spawn Count Logic

1. Roll for first companion using calculated probability
2. If **failed**: Stop (0 companions)
3. If **success**:
   - Spawn first companion
   - If villager count is **10 or more**: Roll again for second companion (same probability)
   - If villager count is **less than 10**: Stop (1 companion)
4. **Maximum**: 2 companions per village spawn group

### Spawn Location

- Within a radius of the **villager group center**
- Must be a valid spawn position (solid block below, air at feet and head level)

---

## Persistence

### Despawn Behavior

- **Does NOT despawn** (same as iron golems)
- Persists through chunk unloading/reloading
- Saved with world data

### Village Binding

- **Bound to village area** (does not wander far from village)
- Uses village boundary detection (same as iron golem pathing)

### Village Death

- If all villagers in the village die: **Companion remains**
- Companion stays in the area, continues to attack hostile mobs

---

## Future Features (Not Implemented Now)

- Player interaction (right-click menu)
- Custom skins/textures
- Naming by player
- Commands to summon companions
- Companion inventory management

---

## Technical Notes

### Entity Registration

- Entity type: `immersivecompanions:companion`
- Extends player-like entity with AI goals

### AI Goals (Priority Order)

1. Float in water (swim)
2. Attack hostile target (melee or ranged based on equipment)
3. Defend village (same as iron golem)
4. Wander within village bounds
5. Look at nearby entities

### Data to Persist

- Gender (male/female)
- Name (first + last)
- Equipment (all slots)
- Current health
- Village binding (position or UUID)
- Hostility state per player (via reputation system)
