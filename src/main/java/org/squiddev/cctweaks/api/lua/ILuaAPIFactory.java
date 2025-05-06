package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.peripheral.IComputerAccess;

public interface ILuaAPIFactory {

    ILuaAPI create(IComputerAccess var1);

    String[] getNames();
}
