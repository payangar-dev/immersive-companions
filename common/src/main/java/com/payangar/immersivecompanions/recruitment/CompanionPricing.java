package com.payangar.immersivecompanions.recruitment;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handles price calculation for companion recruitment.
 */
public class CompanionPricing {

    // Base price range (emeralds)
    private static final int BASE_PRICE_MIN = 8;
    private static final int BASE_PRICE_MAX = 16;

    // Armor bonuses per slot
    private static final Map<Item, Integer> ARMOR_BONUSES = Map.ofEntries(
            // Leather armor
            Map.entry(Items.LEATHER_HELMET, 3),
            Map.entry(Items.LEATHER_CHESTPLATE, 3),
            Map.entry(Items.LEATHER_LEGGINGS, 3),
            Map.entry(Items.LEATHER_BOOTS, 3),
            // Chainmail armor
            Map.entry(Items.CHAINMAIL_HELMET, 6),
            Map.entry(Items.CHAINMAIL_CHESTPLATE, 6),
            Map.entry(Items.CHAINMAIL_LEGGINGS, 6),
            Map.entry(Items.CHAINMAIL_BOOTS, 6),
            // Iron armor
            Map.entry(Items.IRON_HELMET, 9),
            Map.entry(Items.IRON_CHESTPLATE, 9),
            Map.entry(Items.IRON_LEGGINGS, 9),
            Map.entry(Items.IRON_BOOTS, 9)
    );

    // Weapon bonuses
    private static final Map<Item, Integer> WEAPON_BONUSES = Map.of(
            Items.WOODEN_SWORD, 2,
            Items.WOODEN_AXE, 2,
            Items.STONE_SWORD, 5,
            Items.STONE_AXE, 5,
            Items.IRON_SWORD, 8,
            Items.IRON_AXE, 8,
            Items.BOW, 7,
            Items.CROSSBOW, 10
    );

    // Reputation thresholds
    private static final double MAX_DISCOUNT = 0.30; // 30% discount for good rep
    private static final double MAX_MARKUP = 0.50;   // 50% markup for bad rep
    private static final double SEARCH_RANGE = 16.0;

    /**
     * Calculates the base price for a companion (before reputation modifier).
     * Uses entity UUID as seed for consistent pricing.
     *
     * @param companion The companion entity
     * @return Base price including equipment bonuses
     */
    public static int calculateBasePrice(CompanionEntity companion) {
        // Seed random with UUID for consistent pricing
        Random random = new Random(companion.getUUID().getLeastSignificantBits());
        int basePrice = BASE_PRICE_MIN + random.nextInt(BASE_PRICE_MAX - BASE_PRICE_MIN + 1);

        // Add equipment bonuses
        basePrice += getEquipmentBonus(companion);

        return basePrice;
    }

    /**
     * Calculates the final price including reputation modifier.
     * Uses the companion's stored base price.
     *
     * @param basePrice The pre-calculated base price
     * @param companion The companion entity (for location-based reputation check)
     * @param player    The player attempting to recruit
     * @return Final price after reputation adjustment
     */
    public static int calculateFinalPrice(int basePrice, CompanionEntity companion, Player player) {
        double reputationModifier = getReputationModifier(companion, player);
        return Math.max(1, (int) Math.round(basePrice * reputationModifier));
    }

    /**
     * Gets the equipment bonus for a companion based on their gear.
     *
     * @param companion The companion entity
     * @return Total equipment bonus in emeralds
     */
    public static int getEquipmentBonus(CompanionEntity companion) {
        int bonus = 0;

        // Check armor slots
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = companion.getItemBySlot(slot);
            bonus += ARMOR_BONUSES.getOrDefault(armor.getItem(), 0);
        }

        // Check weapon
        ItemStack weapon = companion.getMainHandItem();
        bonus += WEAPON_BONUSES.getOrDefault(weapon.getItem(), 0);

        return bonus;
    }

    /**
     * Calculates the reputation modifier based on nearby villagers' opinion of the player.
     *
     * @param companion The companion entity (used for location)
     * @param player    The player to check reputation for
     * @return Modifier (< 1.0 for discount, > 1.0 for markup)
     */
    public static double getReputationModifier(CompanionEntity companion, Player player) {
        if (!(companion.level() instanceof ServerLevel serverLevel)) {
            return 1.0;
        }

        AABB searchBox = companion.getBoundingBox().inflate(SEARCH_RANGE);
        List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(Villager.class, searchBox);

        if (nearbyVillagers.isEmpty()) {
            return 1.0;
        }

        // Calculate average reputation
        int totalReputation = 0;
        for (Villager villager : nearbyVillagers) {
            totalReputation += getPlayerReputation(villager, player);
        }
        double avgReputation = (double) totalReputation / nearbyVillagers.size();

        // Convert reputation to modifier
        if (avgReputation > 0) {
            // Good reputation: discount (scales up to MAX_DISCOUNT at rep 100)
            double discountFactor = Math.min(avgReputation / 100.0, 1.0);
            return 1.0 - (discountFactor * MAX_DISCOUNT);
        } else if (avgReputation < 0) {
            // Bad reputation: markup (scales up to MAX_MARKUP at rep -100)
            double markupFactor = Math.min(Math.abs(avgReputation) / 100.0, 1.0);
            return 1.0 + (markupFactor * MAX_MARKUP);
        }

        return 1.0;
    }

    /**
     * Gets the player's reputation with a specific villager.
     * Extracted from CompanionDefendVillageGoal for reuse.
     *
     * @param villager The villager to check
     * @param player   The player to check reputation for
     * @return Reputation score (positive = good, negative = bad)
     */
    public static int getPlayerReputation(Villager villager, Player player) {
        int reputation = 0;

        // Negative gossip types reduce reputation
        reputation -= villager.getGossips().getReputation(player.getUUID(), gossipType ->
                gossipType == GossipType.MINOR_NEGATIVE || gossipType == GossipType.MAJOR_NEGATIVE);

        // Positive gossip types increase reputation
        reputation += villager.getGossips().getReputation(player.getUUID(), gossipType ->
                gossipType == GossipType.MINOR_POSITIVE || gossipType == GossipType.MAJOR_POSITIVE ||
                        gossipType == GossipType.TRADING);

        return reputation;
    }

    /**
     * Checks if a player can afford the recruitment price.
     *
     * @param player The player to check
     * @param price  The price in emeralds
     * @return true if player has enough emeralds
     */
    public static boolean canAfford(Player player, int price) {
        return countEmeralds(player) >= price;
    }

    /**
     * Counts the total emeralds in a player's inventory.
     *
     * @param player The player to check
     * @return Total emerald count
     */
    public static int countEmeralds(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
