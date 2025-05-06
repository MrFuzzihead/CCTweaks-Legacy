package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaObject;
import dan200.computercraft.api.lua.LuaException;

public interface ILuaObjectWithArguments extends ILuaObject {

    Object[] callMethod(ILuaContext var1, int var2, IArguments var3) throws LuaException, InterruptedException;
}
