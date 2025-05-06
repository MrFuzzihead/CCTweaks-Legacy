package org.squiddev.cctweaks.api.turtle;

import net.minecraft.item.ItemStack;

import dan200.computercraft.api.turtle.ITurtleAccess;

public interface ITurtleFuelRegistry {

    void addFuelProvider(ITurtleFuelProvider var1);

    ITurtleFuelProvider getProvider(ITurtleAccess var1, ItemStack var2);
}
