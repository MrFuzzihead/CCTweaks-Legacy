package org.squiddev.cctweaks.core;

import org.squiddev.cctweaks.api.ICCTweaksAPI;
import org.squiddev.cctweaks.api.peripheral.IPeripheralHelpers;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;
import org.squiddev.cctweaks.core.peripheral.PeripheralHelpers;
import org.squiddev.cctweaks.core.turtle.TurtleRegistry;

import dan200.computercraft.api.lua.ILuaEnvironment;
import dan200.computercraft.core.lua.lib.LuaEnvironment;

/**
 * The implementation for {@link org.squiddev.cctweaks.api.CCTweaksAPI}
 */
public final class API implements ICCTweaksAPI {

    private final IPeripheralHelpers peripheralHelpers = new PeripheralHelpers();

    @Override
    public ITurtleRegistry turtleRegistry() {
        return TurtleRegistry.instance;
    }

    @Override
    public IPeripheralHelpers peripheralHelpers() {
        return peripheralHelpers;
    }

    @Override
    public ILuaEnvironment luaEnvironment() {
        return LuaEnvironment.instance;
    }
}
