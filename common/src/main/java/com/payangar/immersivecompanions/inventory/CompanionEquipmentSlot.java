package com.payangar.immersivecompanions.inventory;

import com.mojang.datafixers.util.Pair;
import com.payangar.immersivecompanions.ImmersiveCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;

import javax.annotation.Nullable;

/**
 * Custom slot implementation for companion equipment.
 * Validates items based on slot type and provides empty slot icons.
 */
public class CompanionEquipmentSlot extends Slot {

    private static final ResourceLocation EMPTY_MAIN_SLOT = ResourceLocation.fromNamespaceAndPath(
            ImmersiveCompanions.MOD_ID, "item/empty_main_slot");

    /** Vanilla armor slot backgrounds */
    private static final ResourceLocation[] ARMOR_SLOT_TEXTURES = new ResourceLocation[]{
            InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS
    };

    private final EquipmentSlot equipmentSlot;

    public CompanionEquipmentSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
        EquipmentSlot slot = CompanionInventory.getEquipmentSlot(index);
        if (slot == null) {
            throw new IllegalArgumentException("Invalid equipment slot index: " + index);
        }
        this.equipmentSlot = slot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        return switch (equipmentSlot) {
            case HEAD, CHEST, LEGS, FEET -> canEquipArmor(stack, equipmentSlot);
            case MAINHAND -> true; // Main hand accepts any item
            case OFFHAND -> canEquipOffhand(stack);
            default -> false; // Body slot (horse armor) - not applicable
        };
    }

    /**
     * Checks if an item can be equipped as armor in the given slot.
     */
    private boolean canEquipArmor(ItemStack stack, EquipmentSlot slot) {
        // ArmorItem provides explicit slot info
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot() == slot;
        }

        // Check for items that can be equipped in head slot (pumpkins, skulls, etc.)
        if (slot == EquipmentSlot.HEAD) {
            Item item = stack.getItem();
            if (item == Items.CARVED_PUMPKIN || item == Items.SKELETON_SKULL
                    || item == Items.WITHER_SKELETON_SKULL || item == Items.ZOMBIE_HEAD
                    || item == Items.CREEPER_HEAD || item == Items.DRAGON_HEAD
                    || item == Items.PIGLIN_HEAD || item == Items.PLAYER_HEAD
                    || item instanceof net.minecraft.world.item.BannerItem) {
                return true;
            }
        }

        // Elytra goes in chest slot
        if (slot == EquipmentSlot.CHEST && stack.getItem() instanceof ElytraItem) {
            return true;
        }

        return false;
    }

    /**
     * Checks if an item can be equipped in the offhand slot.
     * Accepts shields, one-handed weapons, torches, and lanterns.
     */
    private boolean canEquipOffhand(ItemStack stack) {
        Item item = stack.getItem();

        // Shields
        if (item instanceof ShieldItem) {
            return true;
        }

        // One-handed weapons (swords, axes)
        if (item instanceof SwordItem || item instanceof AxeItem) {
            return true;
        }

        // Torches and lanterns
        if (item == Items.TORCH || item == Items.SOUL_TORCH
                || item == Items.LANTERN || item == Items.SOUL_LANTERN) {
            return true;
        }

        return false;
    }

    @Override
    public int getMaxStackSize() {
        // Equipment slots only hold 1 item
        return 1;
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return switch (equipmentSlot) {
            case HEAD -> Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[0]);
            case CHEST -> Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[1]);
            case LEGS -> Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[2]);
            case FEET -> Pair.of(InventoryMenu.BLOCK_ATLAS, ARMOR_SLOT_TEXTURES[3]);
            case OFFHAND -> Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            case MAINHAND -> Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_MAIN_SLOT);
            default -> null; // Body slot - no icon
        };
    }
}
