/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Open Mods
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.squiddev.cctweaks.core.utils;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Inventory helpers used by the upgrade items.
 *
 * <p>
 * This started life as a copy of OpenModsLib's inventory utilities. Everything CCTweaks actually
 * calls is here; the unused half -- sided inventories, double-chest fixups, insert/merge helpers --
 * was deleted along with the network features that needed it.
 * </p>
 */
public class InventoryUtils {

    /**
     * Count up to {@code max} items of a type across an inventory without modifying it.
     *
     * @param inventory The inventory to scan
     * @param item      The item to count
     * @param max       Stop counting once this many have been found
     * @return The number found, capped at {@code max}
     */
    public static int countItems(IInventory inventory, Item item, int max) {
        int count = 0;
        int size = inventory.getSizeInventory();
        for (int i = 0; i < size && count < max; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item && stack.stackSize > 0) {
                count += stack.stackSize;
            }
        }
        return Math.min(count, max);
    }

    /**
     * Extract n items
     *
     * @param inventory The inventory to extract from
     * @param item      The item to extract
     * @param count     The number to extract
     * @return Number of items remaining
     */
    public static int extractItems(IInventory inventory, Item item, final int count) {
        List<Integer> items = new ArrayList<Integer>();

        int remaining = count;

        int invSize = inventory.getSizeInventory();
        for (int i = 0; i < invSize; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == item && stack.stackSize > 0) {
                items.add(i);

                int size = stack.stackSize;
                if (size > remaining) {
                    remaining = 0;
                } else {
                    remaining -= stack.stackSize;
                }

                if (remaining <= 0) break;
            }
        }

        if (remaining > 0) return remaining;

        remaining = count;
        for (int i : items) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.stackSize <= remaining) {
                remaining -= stack.stackSize;
                inventory.setInventorySlotContents(i, null);
            } else {
                stack.stackSize -= remaining;
                inventory.setInventorySlotContents(i, stack);
                remaining = 0;
            }
        }

        inventory.markDirty();

        return 0;
    }

}
