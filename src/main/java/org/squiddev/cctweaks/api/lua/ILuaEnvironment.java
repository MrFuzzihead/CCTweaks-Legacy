package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;

public interface ILuaEnvironment {

    String EVENT_NAME = "cctweaks_task";

    void registerAPI(ILuaAPIFactory var1);

    long issueTask(IComputerAccess var1, ILuaTask var2, int var3) throws LuaException;

    Object[] executeTask(IComputerAccess var1, ILuaContext var2, ILuaTask var3, int var4)
        throws LuaException, InterruptedException;

    void sleep(IComputerAccess var1, ILuaContext var2, int var3) throws LuaException, InterruptedException;
}
