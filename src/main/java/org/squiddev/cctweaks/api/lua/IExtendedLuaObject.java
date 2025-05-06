package org.squiddev.cctweaks.api.lua;

import java.util.Map;

import dan200.computercraft.api.lua.ILuaObject;

public interface IExtendedLuaObject extends ILuaObject {

    Map<Object, Object> getAdditionalData();
}
