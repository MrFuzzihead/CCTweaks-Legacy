package org.squiddev.cctweaks.items;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.computer.items.ItemComputer;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.pocket.items.PocketComputerItemFactory;
import dan200.computercraft.shared.turtle.items.ItemTurtleBase;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;

/**
 * Handles crafting with ComputerUpgrades
 */
public class CraftingComputerUpgrade implements IRecipe {

    @Override
    public boolean matches(InventoryCrafting inventorycrafting, World world) {
        return getComputerStack(inventorycrafting) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventorycrafting) {
        ItemStack computerStack = getComputerStack(inventorycrafting);
        if (computerStack == null) return null;

        IComputerItem computerItem = (IComputerItem) computerStack.getItem();

        // Get the current data
        int id = computerItem.getComputerID(computerStack);
        String label = computerItem.getLabel(computerStack);

        if (computerItem instanceof ItemComputer) {
            return ComputerItemFactory.create(id, label, ComputerFamily.Advanced);
        } else if (computerItem instanceof ItemTurtleBase) {
            ItemTurtleBase turtle = (ItemTurtleBase) computerItem;

            return TurtleItemFactory.create(
                id,
                label,
                turtle.getColour(computerStack),
                ComputerFamily.Advanced,
                turtle.getUpgrade(computerStack, TurtleSide.Left),
                turtle.getUpgrade(computerStack, TurtleSide.Right),
                turtle.getFuelLevel(computerStack),
                turtle.getOverlay(computerStack),
                turtle.getHatOverlay(computerStack));
        } else if (computerItem instanceof ItemPocketComputer) {
            ItemPocketComputer pocket = (ItemPocketComputer) computerItem;

            ItemStack result = PocketComputerItemFactory
                .create(id, label, ComputerFamily.Advanced, pocket.getHasModem(computerStack));

            // The upgrade NBT carries the ender modem, speaker, chat box and NPC upgrades, plus
            // any data those upgrades own. PocketComputerItemFactory only knows about the
            // wireless-modem boolean, so copy the rest across ourselves.
            int upgrade = pocket.getUpgrade(computerStack);
            NBTTagCompound source = computerStack.getTagCompound();
            if (result != null && upgrade > 0 && source != null) {
                NBTTagCompound tag = result.getTagCompound();
                if (tag == null) {
                    tag = new NBTTagCompound();
                    result.setTagCompound(tag);
                }

                tag.setInteger("upgrade", upgrade);
                if (source.hasKey("upgradeData")) {
                    tag.setTag(
                        "upgradeData",
                        source.getTag("upgradeData")
                            .copy());
                }
            }

            return result;
        }

        return null;
    }

    /**
     * Find the ItemStack for the computer
     *
     * @param crafting The crafting inventory space
     * @return The ItemStack with a computer type in, or null if the recipe doesn't match
     */
    protected ItemStack getComputerStack(InventoryCrafting crafting) {
        int size = crafting.getSizeInventory();

        ItemStack computerStack = null;
        boolean hasUpgrade = false;
        int gold = 0;

        for (int i = 0; i < size; i++) {
            ItemStack itemStack = crafting.getStackInSlot(i);
            if (itemStack == null) {
                continue;
            }

            Item item = itemStack.getItem();
            if (item == Items.gold_ingot) {
                // Count slots, not stack size: vanilla crafting consumes one item per slot, so
                // seven ingots in one slot would otherwise be a free upgrade.
                gold++;
            } else if (item instanceof IComputerItem) {
                // Reject IComputerItem implementations we cannot rebuild. Accepting them here and
                // returning null from getCraftingResult would shadow valid recipes with no output.
                if (!(item instanceof ItemComputer) && !(item instanceof ItemTurtleBase)
                    && !(item instanceof ItemPocketComputer)) {
                    return null;
                }

                // If its a 'computer'
                // Don't allow more than one computer and it must be a normal computer
                if (computerStack != null || ((IComputerItem) item).getFamily(itemStack) != ComputerFamily.Normal) {
                    return null;
                }

                computerStack = itemStack;
            } else if (item instanceof ItemComputerUpgrade) {
                // Don't allow more than one upgrade
                if (hasUpgrade) return null;

                hasUpgrade = true;
            }
        }

        if (computerStack == null || !hasUpgrade) {
            return null;
        } else if (computerStack.getItem() instanceof ItemTurtleBase && gold < 7) {
            return null;
        } else {
            return computerStack;
        }
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        // A representative result for recipe browsers. The real result depends on the input
        // computer, but IRecipe's contract expects a non-null sample.
        return ComputerItemFactory.create(-1, null, ComputerFamily.Advanced);
    }
}
