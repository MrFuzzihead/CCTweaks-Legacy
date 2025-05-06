package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;

public interface IExtendedLuaTask extends ILuaTask {

    void update() throws LuaException;
}
