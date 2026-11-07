package org.squiddev.cctweaks.api;

import org.squiddev.cctweaks.api.peripheral.IPeripheralHelpers;
import org.squiddev.cctweaks.api.turtle.ITurtleRegistry;

import dan200.computercraft.api.lua.ILuaEnvironment;

/**
 * A provider for the API interface
 */
public interface ICCTweaksAPI {

    ITurtleRegistry turtleRegistry();

    IPeripheralHelpers peripheralHelpers();

    ILuaEnvironment luaEnvironment();
}
