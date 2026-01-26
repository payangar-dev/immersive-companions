package com.payangar.immersivecompanions.inventory;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import com.payangar.immersivecompanions.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for companion equipment management.
 * Provides 6 equipment slots (4 armor + 2 hands) and player inventory access.
 */
public class CompanionEquipmentMenu extends AbstractContainerMenu {

    /** The companion being managed */
    private final CompanionEntity companion;
    private final CompanionInventory companionInventory;

    // Slot configuration matching companion.png texture
    // Armor slots on left side of entity area
    private static final int ARMOR_SLOT_X = 8;
    private static final int HEAD_SLOT_Y = 8;
    private static final int CHEST_SLOT_Y = 26;
    private static final int LEGS_SLOT_Y = 44;
    private static final int FEET_SLOT_Y = 62;

    // Weapon slots on right side of entity area
    private static final int WEAPON_SLOT_X = 77;
    private static final int OFFHAND_SLOT_Y = 44;
    private static final int MAINHAND_SLOT_Y = 62;

    // Player inventory positioning
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    /**
     * Server-side constructor - called when opening the menu.
     */
    public CompanionEquipmentMenu(int containerId, Inventory playerInventory, CompanionEntity companion) {
        super(ModMenuTypes.getCompanionEquipment(), containerId);
        this.companion = companion;
        this.companionInventory = new CompanionInventory(companion);

        addCompanionSlots();
        addPlayerInventory(playerInventory);
    }

    /**
     * Client-side constructor for NeoForge - called when receiving the menu packet.
     * Reads the entity ID from the extra data buffer.
     */
    public CompanionEquipmentMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readVarInt());
    }

    /**
     * Client-side constructor for Fabric - receives entity ID directly.
     * Also used by the NeoForge constructor after reading from buffer.
     */
    public CompanionEquipmentMenu(int containerId, Inventory playerInventory, int entityId) {
        super(ModMenuTypes.getCompanionEquipment(), containerId);

        Entity entity = playerInventory.player.level().getEntity(entityId);

        if (entity instanceof CompanionEntity comp) {
            this.companion = comp;
        } else {
            throw new IllegalStateException("Could not find companion entity with ID " + entityId);
        }

        this.companionInventory = new CompanionInventory(this.companion);

        addCompanionSlots();
        addPlayerInventory(playerInventory);
    }

    private void addCompanionSlots() {
        // Armor slots (indices 0-3)
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 0, ARMOR_SLOT_X, HEAD_SLOT_Y));   // HEAD
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 1, ARMOR_SLOT_X, CHEST_SLOT_Y)); // CHEST
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 2, ARMOR_SLOT_X, LEGS_SLOT_Y));  // LEGS
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 3, ARMOR_SLOT_X, FEET_SLOT_Y));  // FEET

        // Weapon slots (indices 4-5)
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 4, WEAPON_SLOT_X, OFFHAND_SLOT_Y));  // OFFHAND
        this.addSlot(new CompanionEquipmentSlot(companionInventory, 5, WEAPON_SLOT_X, MAINHAND_SLOT_Y)); // MAINHAND
    }

    private void addPlayerInventory(Inventory playerInventory) {
        // Main inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9; // Slots 9-35
                int x = PLAYER_INV_X + col * 18;
                int y = PLAYER_INV_Y + row * 18;
                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }

        // Hotbar (1 row of 9)
        for (int col = 0; col < 9; col++) {
            int x = PLAYER_INV_X + col * 18;
            this.addSlot(new Slot(playerInventory, col, x, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Equipment slots are 0-5, player inventory is 6-41
            if (slotIndex < 6) {
                // Moving from companion equipment to player inventory
                if (!this.moveItemStackTo(slotStack, 6, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to companion equipment
                // Try to find appropriate equipment slot
                boolean moved = false;

                // Check armor slots first
                for (int i = 0; i < 4; i++) {
                    Slot equipSlot = this.slots.get(i);
                    if (equipSlot.mayPlace(slotStack) && !equipSlot.hasItem()) {
                        if (this.moveItemStackTo(slotStack, i, i + 1, false)) {
                            moved = true;
                            break;
                        }
                    }
                }

                // Try mainhand slot (index 5)
                if (!moved && !this.slots.get(5).hasItem()) {
                    if (this.moveItemStackTo(slotStack, 5, 6, false)) {
                        moved = true;
                    }
                }

                // Try offhand slot (index 4)
                if (!moved && this.slots.get(4).mayPlace(slotStack) && !this.slots.get(4).hasItem()) {
                    if (this.moveItemStackTo(slotStack, 4, 5, false)) {
                        moved = true;
                    }
                }

                // Move between inventory and hotbar if no equipment slot worked
                if (!moved) {
                    if (slotIndex < 33) {
                        // From main inventory to hotbar
                        if (!this.moveItemStackTo(slotStack, 33, 42, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // From hotbar to main inventory
                        if (!this.moveItemStackTo(slotStack, 6, 33, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return companionInventory.stillValid(player);
    }

    /**
     * Gets the companion being managed.
     */
    public CompanionEntity getCompanion() {
        return companion;
    }
}
