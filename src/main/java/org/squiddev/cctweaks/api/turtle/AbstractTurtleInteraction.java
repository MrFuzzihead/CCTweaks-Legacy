package org.squiddev.cctweaks.api.turtle;

import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;

public abstract class AbstractTurtleInteraction implements ITurtleInteraction {

    @Override
    public TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        return null;
    }

    @Override
    public boolean canSwing(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        return false;
    }

    @Override
    public TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack,
        ForgeDirection direction, MovingObjectPosition hit) throws LuaException {
        return null;
    }

    @Override
    public boolean canUse(ITurtleAccess turtle, FakePlayer player, ItemStack stack, ForgeDirection direction,
        MovingObjectPosition hit) {
        return false;
    }
}
