package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.ILuaObject;

public interface ILuaAPI extends ILuaObject {

    void startup();

    void shutdown();

    void advance(double var1);
}
