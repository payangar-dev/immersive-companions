package com.payangar.immersivecompanions.data;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.entity.CompanionType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Handles randomized equipment for companions.
 */
public class CompanionEquipment {

    private static final List<Item> MELEE_WEAPONS = List.of(
            Items.WOODEN_SWORD, Items.WOODEN_AXE,
            Items.STONE_SWORD, Items.STONE_AXE,
            Items.IRON_SWORD, Items.IRON_AXE
    );

    private static final List<Item> RANGED_WEAPONS = List.of(
            Items.BOW, Items.CROSSBOW
    );

    private static final List<Item> HELMETS = List.of(
            Items.AIR, Items.AIR, // Higher chance of no helmet
            Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET
    );

    private static final List<Item> CHESTPLATES = List.of(
            Items.AIR, Items.AIR,
            Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE
    );

    private static final List<Item> LEGGINGS = List.of(
            Items.AIR, Items.AIR,
            Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS
    );

    private static final List<Item> BOOTS = List.of(
            Items.AIR, Items.AIR,
            Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS
    );

    /**
     * Equips a companion with randomized gear based on their combat type.
     *
     * @param companion  The companion entity to equip
     * @param combatType The combat type (MELEE or RANGED)
     * @param random     The random source to use
     */
    public static void equipCompanion(CompanionEntity companion, CompanionType combatType, RandomSource random) {
        // Equip weapon based on combat type
        List<Item> weaponList = combatType.isRanged() ? RANGED_WEAPONS : MELEE_WEAPONS;
        Item weapon = weaponList.get(random.nextInt(weaponList.size()));
        companion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));

        // Equip random armor pieces
        equipArmorPiece(companion, EquipmentSlot.HEAD, HELMETS, random);
        equipArmorPiece(companion, EquipmentSlot.CHEST, CHESTPLATES, random);
        equipArmorPiece(companion, EquipmentSlot.LEGS, LEGGINGS, random);
        equipArmorPiece(companion, EquipmentSlot.FEET, BOOTS, random);

        // Set drop chances to 0 so equipment doesn't drop on death
        setNoDropChances(companion);
    }

    private static void equipArmorPiece(CompanionEntity companion, EquipmentSlot slot,
                                         List<Item> armorList, RandomSource random) {
        Item armor = armorList.get(random.nextInt(armorList.size()));
        if (armor != Items.AIR) {
            companion.setItemSlot(slot, new ItemStack(armor));
        }
    }

    private static void setNoDropChances(CompanionEntity companion) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            companion.setDropChance(slot, 0.0F);
        }
    }
}
