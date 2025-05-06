package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.lua.LuaException;

public interface IArguments {

    int size();

    double getNumber(int var1) throws LuaException;

    boolean getBoolean(int var1) throws LuaException;

    String getString(int var1) throws LuaException;

    byte[] getStringBytes(int var1) throws LuaException;

    Object getArgumentBinary(int var1);

    Object getArgument(int var1);

    Object[] asArguments();

    Object[] asBinary();

    IArguments subArgs(int var1);
}
