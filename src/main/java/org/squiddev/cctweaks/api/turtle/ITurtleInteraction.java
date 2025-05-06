package org.squiddev.cctweaks.api.turtle;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public interface ITurtleInteraction {

    TurtleCommandResult swing(ITurtleAccess var1, IComputerAccess var2, FakePlayer var3, ItemStack var4,
        ForgeDirection var5, MovingObjectPosition var6) throws LuaException;

    boolean canSwing(ITurtleAccess var1, FakePlayer var2, ItemStack var3, ForgeDirection var4,
        MovingObjectPosition var5);

    TurtleCommandResult use(ITurtleAccess var1, IComputerAccess var2, FakePlayer var3, ItemStack var4,
        ForgeDirection var5, MovingObjectPosition var6) throws LuaException;

    boolean canUse(ITurtleAccess var1, FakePlayer var2, ItemStack var3, ForgeDirection var4, MovingObjectPosition var5);
}
