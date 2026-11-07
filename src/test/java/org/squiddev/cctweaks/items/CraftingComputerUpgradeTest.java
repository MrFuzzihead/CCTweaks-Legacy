package org.squiddev.cctweaks.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.turtle.items.ItemTurtleAdvanced;
import dan200.computercraft.shared.turtle.items.ItemTurtleBase;
import dan200.computercraft.shared.turtle.items.ItemTurtleNormal;

public class CraftingComputerUpgradeTest {

    private static final Container CONTAINER = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }
    };

    private final CraftingComputerUpgrade recipe = new CraftingComputerUpgrade();
    private final InventoryCrafting crafting = new InventoryCrafting(CONTAINER, 3, 3);
    private final ItemStack upgrade = new ItemStack(new ItemComputerUpgrade());

    private static ItemStack computer(ComputerFamily family) {
        // ItemStack#getItem() resolves through Item.delegate, so the Item has to be constructed
        // normally; a Mockito mock leaves the delegate field null.
        ItemComputer item = new ItemComputer(new Block(Material.rock) {});
        return new ItemStack(item, 1, family == ComputerFamily.Advanced ? 16384 : 0);
    }

    private static ItemStack turtle(ComputerFamily family) {
        ItemTurtleBase item = family == ComputerFamily.Advanced ? new ItemTurtleAdvanced(new Block(Material.iron) {})
            : new ItemTurtleNormal(new Block(Material.iron) {});
        return new ItemStack(item);
    }

    private static ItemStack pocket(ComputerFamily family) {
        return new ItemStack(new ItemPocketComputer(), 1, family == ComputerFamily.Advanced ? 1 : 0);
    }

    private void put(int slot, ItemStack stack) {
        crafting.setInventorySlotContents(slot, stack);
    }

    @Test
    public void emptyGridDoesNotMatch() {
        assertFalse(recipe.matches(crafting, null));
    }

    @Test
    public void normalComputerAndUpgradeMatches() {
        put(0, computer(ComputerFamily.Normal));
        put(1, upgrade);

        assertTrue(recipe.matches(crafting, null));
    }

    @Test
    public void computerWithoutUpgradeDoesNotMatch() {
        put(0, computer(ComputerFamily.Normal));
        assertFalse(recipe.matches(crafting, null));
    }

    @Test
    public void twoComputersDoNotMatch() {
        put(0, computer(ComputerFamily.Normal));
        put(1, computer(ComputerFamily.Normal));
        put(2, upgrade);

        assertFalse(recipe.matches(crafting, null));
    }

    @Test
    public void advancedComputerDoesNotMatch() {
        put(0, computer(ComputerFamily.Advanced));
        put(1, upgrade);

        assertFalse(recipe.matches(crafting, null));
    }

    @Test
    public void turtleNeedsSevenGoldSlots() {
        put(0, turtle(ComputerFamily.Normal));
        put(1, upgrade);
        for (int i = 2; i < 8; i++) put(i, new ItemStack(Items.gold_ingot));

        assertFalse(recipe.matches(crafting, null), "six gold slots must not match");

        put(8, new ItemStack(Items.gold_ingot));
        assertTrue(recipe.matches(crafting, null), "seven gold slots must match");
    }

    @Test
    public void stackOfGoldDoesNotCountAsSevenSlots() {
        put(0, turtle(ComputerFamily.Normal));
        put(1, upgrade);
        put(2, new ItemStack(Items.gold_ingot, 7));

        assertFalse(recipe.matches(crafting, null), "vanilla consumes one item per slot, so a stack is not seven gold");
    }

    @Test
    public void pocketComputerMatches() {
        put(0, pocket(ComputerFamily.Normal));
        put(1, upgrade);

        assertTrue(recipe.matches(crafting, null));
    }

    @Test
    public void pocketUpgradeAndDataArePreserved() {
        ItemPocketComputer old = ComputerCraft.Items.pocketComputer;
        ComputerCraft.Items.pocketComputer = new ItemPocketComputer();
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("upgrade", 3);
            NBTTagCompound data = new NBTTagCompound();
            data.setString("speaker", "value");
            tag.setTag("upgradeData", data);

            ItemStack pocket = new ItemStack(ComputerCraft.Items.pocketComputer, 1, 0);
            pocket.setTagCompound(tag);
            put(0, pocket);
            put(1, upgrade);

            assertTrue(recipe.matches(crafting, null));
            ItemStack result = recipe.getCraftingResult(crafting);
            org.junit.jupiter.api.Assertions.assertNotNull(result);
            assertEquals(
                3,
                result.getTagCompound()
                    .getInteger("upgrade"));
            assertEquals(
                "value",
                result.getTagCompound()
                    .getCompoundTag("upgradeData")
                    .getString("speaker"));
        } finally {
            ComputerCraft.Items.pocketComputer = old;
        }
    }

    @Test
    public void unsupportedComputerItemIsRejectedAtMatchTime() {
        put(0, new ItemStack(new ForeignComputer()));
        put(1, upgrade);

        assertFalse(recipe.matches(crafting, null), "matches() must agree with getCraftingResult()");
        org.junit.jupiter.api.Assertions.assertNull(recipe.getCraftingResult(crafting));
    }

    private static final class ForeignComputer extends Item implements IComputerItem {

        @Override
        public int getComputerID(ItemStack stack) {
            return -1;
        }

        @Override
        public String getLabel(ItemStack stack) {
            return null;
        }

        @Override
        public ComputerFamily getFamily(ItemStack stack) {
            return ComputerFamily.Normal;
        }
    }
}
