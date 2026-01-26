package com.payangar.immersivecompanions.inventory;

import com.payangar.immersivecompanions.entity.CompanionEntity;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Container implementation that wraps a companion's equipment slots.
 * Delegates to CompanionEntity's equipment methods.
 *
 * Slot mapping:
 * 0 - HEAD
 * 1 - CHEST
 * 2 - LEGS
 * 3 - FEET
 * 4 - OFFHAND
 * 5 - MAINHAND
 */
public class CompanionInventory implements Container {

    private static final int SIZE = 6;
    private static final EquipmentSlot[] SLOT_MAPPING = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.MAINHAND
    };

    private final CompanionEntity companion;
    private final List<ContainerListener> listeners = new ArrayList<>();

    public CompanionInventory(CompanionEntity companion) {
        this.companion = companion;
    }

    /**
     * Adds a listener that will be notified when the container contents change.
     */
    public void addListener(ContainerListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Removes a previously added listener.
     */
    public void removeListener(ContainerListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return ItemStack.EMPTY;
        }
        return companion.getItemBySlot(SLOT_MAPPING[slot]);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = current.split(amount);
        setItem(slot, current.isEmpty() ? ItemStack.EMPTY : current);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        companion.setItemSlot(SLOT_MAPPING[slot], stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        // Notify all listeners that the container has changed
        for (ContainerListener listener : this.listeners) {
            listener.containerChanged(this);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // Check if companion is still alive and player is the owner
        return companion.isAlive() && companion.isOwnedBy(player)
                && player.distanceToSqr(companion) < 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SIZE; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    /**
     * Gets the equipment slot type for a given container slot index.
     */
    public static EquipmentSlot getEquipmentSlot(int slot) {
        if (slot < 0 || slot >= SIZE) {
            return null;
        }
        return SLOT_MAPPING[slot];
    }
}
