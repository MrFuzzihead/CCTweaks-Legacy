package org.squiddev.cctweaks.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.squiddev.cctweaks.testsupport.StubInventory;

public class InventoryUtilsTest {

    private Item item;
    private Item other;
    private StubInventory inventory;

    @BeforeEach
    public void setUp() {
        item = new Item();
        other = new Item();
        inventory = new StubInventory(4);
    }

    @Test
    public void countsAcrossStacksAndCaps() {
        inventory.setInventorySlotContents(0, new ItemStack(item, 3));
        inventory.setInventorySlotContents(1, new ItemStack(other, 2));
        inventory.setInventorySlotContents(2, new ItemStack(item, 4));

        assertEquals(7, InventoryUtils.countItems(inventory, item, 10));
        assertEquals(5, InventoryUtils.countItems(inventory, item, 5));
        assertEquals(0, InventoryUtils.countItems(inventory, new Item(), 10));
    }

    @Test
    public void extractsExactAmountAcrossStacks() {
        inventory.setInventorySlotContents(0, new ItemStack(item, 3));
        inventory.setInventorySlotContents(1, new ItemStack(item, 4));

        assertEquals(0, InventoryUtils.extractItems(inventory, item, 5));
        assertNull(inventory.getStackInSlot(0));
        assertEquals(2, inventory.getStackInSlot(1).stackSize);
        assertEquals(1, inventory.getDirtyCount());
    }

    @Test
    public void extractionIsAllOrNothingWhenInsufficient() {
        inventory.setInventorySlotContents(0, new ItemStack(item, 2));
        ItemStack before = inventory.getStackInSlot(0);

        assertEquals(3, InventoryUtils.extractItems(inventory, item, 5));
        assertEquals(before, inventory.getStackInSlot(0));
        assertEquals(2, inventory.getStackInSlot(0).stackSize);
        assertEquals(0, inventory.getDirtyCount());
    }

    @Test
    public void extractsFromTheFirstStackWhenAmountFits() {
        inventory.setInventorySlotContents(0, new ItemStack(item, 8));
        inventory.setInventorySlotContents(1, new ItemStack(item, 8));

        assertEquals(0, InventoryUtils.extractItems(inventory, item, 7));
        assertEquals(1, inventory.getStackInSlot(0).stackSize);
        assertEquals(8, inventory.getStackInSlot(1).stackSize);
    }
}
