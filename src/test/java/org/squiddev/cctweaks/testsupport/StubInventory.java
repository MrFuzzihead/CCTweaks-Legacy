package org.squiddev.cctweaks.testsupport;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * A plain fixed-size inventory used by the headless tests.
 */
public class StubInventory implements IInventory {

    private final ItemStack[] stacks;
    private final int stackLimit;
    private int dirtyCount;

    public StubInventory(int size) {
        this(size, 64);
    }

    public StubInventory(int size, int stackLimit) {
        this.stacks = new ItemStack[size];
        this.stackLimit = stackLimit;
    }

    @Override
    public int getSizeInventory() {
        return stacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot >= 0 && slot < stacks.length ? stacks[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        ItemStack stack = getStackInSlot(slot);
        if (stack == null) return null;

        if (stack.stackSize <= count) {
            stacks[slot] = null;
            return stack;
        }

        ItemStack result = stack.splitStack(count);
        if (stack.stackSize <= 0) stacks[slot] = null;
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        stacks[slot] = null;
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        stacks[slot] = stack;
    }

    @Override
    public String getInventoryName() {
        return "stub";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return stackLimit;
    }

    @Override
    public void markDirty() {
        dirtyCount++;
    }

    public int getDirtyCount() {
        return dirtyCount;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }
}
