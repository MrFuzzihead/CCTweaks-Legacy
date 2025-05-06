package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public interface IPeripheralWithArguments extends IPeripheral {

    Object[] callMethod(IComputerAccess var1, ILuaContext var2, int var3, IArguments var4)
        throws LuaException, InterruptedException;
}
