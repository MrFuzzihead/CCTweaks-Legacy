package org.squiddev.cctweaks.api.turtle;

import net.minecraft.item.ItemStack;

import dan200.computercraft.api.turtle.ITurtleAccess;

public interface ITurtleFuelProvider {

    boolean canRefuel(ITurtleAccess var1, ItemStack var2);

    int refuel(ITurtleAccess var1, ItemStack var2, int var3);
}
